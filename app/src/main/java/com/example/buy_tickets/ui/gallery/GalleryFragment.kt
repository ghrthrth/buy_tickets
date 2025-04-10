package com.example.buy_tickets.ui.gallery

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.buy_tickets.databinding.FragmentGalleryBinding
import com.example.buy_tickets.ui.filter.FilterFragment
import com.example.buy_tickets.ui.user.UserPreferences
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class GalleryFragment : Fragment(), ProductDetailFragment.OnProductDeletedListener, FilterFragment.FilterFragmentListener {

    private var binding: FragmentGalleryBinding? = null
    private val ids = mutableListOf<String>()
    private val photoUrls = mutableListOf<String>()
    private val titles = mutableListOf<String>()
    private val descriptions = mutableListOf<String>()
    private val latitudes = mutableListOf<Double?>()
    private val longitudes = mutableListOf<Double?>()

    private val client = OkHttpClient()
    private lateinit var adapter: ImageAdapter
    private lateinit var userPreferences: UserPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val galleryViewModel = ViewModelProvider(this).get(GalleryViewModel::class.java)
        binding = FragmentGalleryBinding.inflate(inflater, container, false)

        val root = binding?.root


        userPreferences = UserPreferences(requireContext())
        getPhotoUrlsFromServer()
        return root
    }

    private fun addItemsToList(jsonArray: JSONArray, list: MutableList<String>) {
        for (i in 0 until jsonArray.length()) {
            try {
                list.add(jsonArray.getString(i))
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    private fun addPricesToList(jsonArray: JSONArray, list: MutableList<String>) {
        for (i in 0 until jsonArray.length()) {
            try {
                list.add(jsonArray.getString(i))
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    private fun addCoordinatesToList(jsonArray: JSONArray, list: MutableList<Double?>) {
        for (i in 0 until jsonArray.length()) {
            try {
                if (jsonArray.isNull(i)) {
                    list.add(null)
                } else {
                    list.add(jsonArray.getDouble(i))
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                list.add(null)
            }
        }
    }

    private fun getPhotoUrlsFromServer() {
        val url = "https://decadances.store/buy_tickets/admin_api/return.php"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (!isAdded) return

                if (response.isSuccessful) {
                    response.body?.string()?.let { json ->
                        try {
                            val jsonObject = JSONObject(json)
                            ids.clear()
                            photoUrls.clear()
                            titles.clear()
                            descriptions.clear()
                            latitudes.clear()
                            longitudes.clear()

                            addItemsToList(jsonObject.getJSONArray("id"), ids)
                            addItemsToList(jsonObject.getJSONArray("photoUrls"), photoUrls)
                            addItemsToList(jsonObject.getJSONArray("title"), titles)
                            addItemsToList(jsonObject.getJSONArray("description"), descriptions)
                            addCoordinatesToList(jsonObject.getJSONArray("latitude"), latitudes)
                            addCoordinatesToList(jsonObject.getJSONArray("longitude"), longitudes)

                            activity?.runOnUiThread {
                                displayPhotosInGrid()
                            }
                        } catch (e: JSONException) {
                            Log.e("GalleryFragment", "JSON error", e)
                        }
                    }
                }
            }
        })
    }

    private fun sendPurchaseData(productId: String, productName: String) {
        val userId = userPreferences.getUserId() ?: ""

        // Создаем JSON-объект
        val json = JSONObject().apply {
            put("user_id", userId)
            put("service_id", productId)
            put("product_name", productName)
            put("dates", SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Calendar.getInstance().time))
            put("times", SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().time))
        }

        // Создаем RequestBody с JSON
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://decadances.store/buy_tickets/api/add_application/add.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("GalleryFragment", "Failed to send purchase data", e)
                activity?.runOnUiThread {
                    Log.d("GalleryFragment", "Ошибка отправки: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseText = response.body?.string() ?: "Пустой ответ"
                activity?.runOnUiThread {
                    Log.d("GalleryFragment", "Ответ сервера: $responseText")
                }
            }
        })
    }

    override fun onProductDeleted(productId: Int) {
        val index = ids.indexOf(productId.toString())
        if (index != -1) {
            ids.removeAt(index)
            photoUrls.removeAt(index)
            titles.removeAt(index)
            descriptions.removeAt(index)
            latitudes.removeAt(index)
            longitudes.removeAt(index)
        }
        displayPhotosInGrid()
    }

    private fun displayPhotosInGrid() {
        if (!isAdded || isDetached) return

        activity?.runOnUiThread {
            binding?.recyclerView?.apply {
                layoutManager = GridLayoutManager(context, 1)
                adapter = ImageAdapter(
                    requireContext(),
                    photoUrls,
                    titles,
                    descriptions,
                    latitudes as List<Double>,
                    longitudes as List<Double>
                ).apply {
                    setOnItemClickListener(object : ImageAdapter.OnItemClickListener {
                        override fun onItemClick(position: Int) {
                            if (position in ids.indices) {
                                val detailFragment = ProductDetailFragment(
                                    requireContext(),
                                    ids[position].toInt(),
                                    titles[position],
                                    descriptions[position],
                                    photoUrls[position],
                                    latitudes.getOrNull(position) ?: 55.179902,
                                    longitudes.getOrNull(position) ?: 30.213778
                                )
                                detailFragment.setOnProductDeletedListener(this@GalleryFragment)
                                detailFragment.setOnProductBuyListener { ids, titles ->
                                    sendPurchaseData(ids, titles)
                                }
                                detailFragment.show(parentFragmentManager, "product_detail")
                            }
                        }
                    })
                }
            }
        }
    }

    override fun onFilterApplied(minPrice: Double, maxPrice: Double, category: String) {
        // Implement your filter logic here
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}

