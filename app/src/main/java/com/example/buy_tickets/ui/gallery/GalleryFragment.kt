package com.example.buy_tickets.ui.gallery

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.buy_tickets.databinding.FragmentGalleryBinding
import com.example.buy_tickets.ui.filter.FilterFragment
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class GalleryFragment : Fragment(), ProductDetailFragment.OnProductDeletedListener, FilterFragment.FilterFragmentListener {

    private var binding: FragmentGalleryBinding? = null // Изменено на nullable
    private val ids = mutableListOf<String>()
    private val photoUrls = mutableListOf<String>()
    private val titles = mutableListOf<String>()
    private val descriptions = mutableListOf<String>()

    private val client = OkHttpClient()
    private lateinit var adapter: ImageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val galleryViewModel = ViewModelProvider(this).get(GalleryViewModel::class.java)

        binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root = binding?.root // Используем безопасный вызов

        getPhotoUrlsFromServer()  // Получаем данные с сервера

        return root
    }

    private fun addItemsToList(jsonArray: JSONArray, list: MutableList<String>) {
        for (i in 0 until jsonArray.length()) {
            try {
                val item = jsonArray.getString(i)
                list.add(item)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    private fun getPhotoUrlsFromServer() {

        val url = "https://claimbes.store/buy_tickets/admin_api/return.php"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                activity?.runOnUiThread {
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!isAdded) return

                activity?.runOnUiThread {
                }

                if (response.isSuccessful) {
                    val json = response.body?.string()
                    Log.d("GalleryFragment", "Server response: $json")

                    try {
                        val jsonObject = JSONObject(json)
                        val idArray = jsonObject.getJSONArray("id")
                        val photoUrlsArray = jsonObject.getJSONArray("photoUrls")
                        val titleArray = jsonObject.getJSONArray("title")
                        val descriptionArray = jsonObject.getJSONArray("description")

                        ids.clear()
                        photoUrls.clear()
                        titles.clear()
                        descriptions.clear()

                        addItemsToList(idArray, ids)
                        addItemsToList(photoUrlsArray, photoUrls)
                        addItemsToList(titleArray, titles)
                        addItemsToList(descriptionArray, descriptions)

                        Log.d("GalleryFragment", "Data loaded successfully")

                        if (isAdded) {
                            displayPhotosInGrid()
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Log.e("GalleryFragment", "JSON parsing error: ${e.message}")
                    }
                } else {
                    Log.e("GalleryFragment", "Failed to load data: ${response.code}")
                }
            }
        })
    }

    override fun onProductDeleted(productId: Int) {
        // Удаляем товар из списка по ID
        val index = ids.indexOf(productId.toString())
        if (index != -1) {
            ids.removeAt(index)
            photoUrls.removeAt(index)
            titles.removeAt(index)
            descriptions.removeAt(index)
        }

        // Обновляем адаптер
        displayPhotosInGrid()
    }

    private fun displayPhotosInGrid() {
        if (!isAdded || isDetached) {
            return // Проверка, что фрагмент все еще прикреплен к активности
        }

        activity?.runOnUiThread {
            if (binding == null) return@runOnUiThread

            val recyclerView = binding?.recyclerView // Используем безопасный вызов
            recyclerView?.layoutManager = GridLayoutManager(context, 1)

            adapter = ImageAdapter(requireContext(), photoUrls, titles, descriptions)
            recyclerView?.adapter = adapter

            adapter.setOnItemClickListener(object : ImageAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    if (ids.isEmpty() || photoUrls.isEmpty() || titles.isEmpty() || descriptions.isEmpty()) {
                        Log.e("GalleryFragment", "One of the lists is empty")
                        return
                    }

                    if (position < 0 || position >= ids.size) {
                        Log.e("GalleryFragment", "Invalid position: $position")
                        return
                    }
                    val selectedId = ids[position]
                    val selectedTitle = titles[position]
                    val selectedDescription = descriptions[position]
                    val selectedImageUrl = photoUrls[position]

                    Log.d("GalleryFragment", "Selected ID: $selectedId")
                    Log.d("GalleryFragment", "Selected Title: $selectedTitle")
                    Log.d("GalleryFragment", "Selected Description: $selectedDescription")
                    Log.d("GalleryFragment", "Selected Image URL: $selectedImageUrl")

                    val selectedIds = selectedId.toInt()

                    activity?.runOnUiThread {
                        val detailFragment = ProductDetailFragment(
                            requireContext(),
                            selectedIds,
                            selectedTitle,
                            selectedDescription,
                            selectedImageUrl
                        )
                        detailFragment.setOnProductDeletedListener(this@GalleryFragment)
                        detailFragment.show(parentFragmentManager, "product_detail")
                    }
                }
            })


        }
    }

    override fun onFilterApplied(minPrice: Double, maxPrice: Double, category: String) {
        TODO("Not yet implemented")
    }
}