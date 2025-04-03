package com.example.buy_tickets.ui.gallery

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.buy_tickets.databinding.FragmentGalleryBinding
import com.example.buy_tickets.ui.filter.FilterFragment
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class GalleryFragment : Fragment(), ProductDetailFragment.OnProductDeletedListener, FilterFragment.FilterFragmentListener {

    private var binding: FragmentGalleryBinding? = null
    private val ids = mutableListOf<String>()
    private val photoUrls = mutableListOf<String>()
    private val titles = mutableListOf<String>()
    private val descriptions = mutableListOf<String>()
    private val latitudes = mutableListOf<Double>()
    private val longitudes = mutableListOf<Double>()

    private val client = OkHttpClient()
    private lateinit var adapter: ImageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val galleryViewModel = ViewModelProvider(this).get(GalleryViewModel::class.java)
        binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root = binding?.root

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

    private fun addCoordinatesToList(jsonArray: JSONArray, list: MutableList<Double>) {
        for (i in 0 until jsonArray.length()) {
            try {
                list.add(jsonArray.getDouble(i))
            } catch (e: JSONException) {
                e.printStackTrace()
                list.add(0.0) // Default value if error
            }
        }
    }

    private fun getPhotoUrlsFromServer() {
        val url = "https://claimbes.store/buy_tickets/admin_api/return.php"
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

                            // Add these lines if your API returns coordinates
//                            addCoordinatesToList(jsonObject.getJSONArray("latitude"), latitudes)
//                            addCoordinatesToList(jsonObject.getJSONArray("longitude"), longitudes)

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
                    latitudes,
                    longitudes
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
                                    latitudes.getOrElse(position) { 55.179902 },
                                    longitudes.getOrElse(position) { 30.213778 }
                                )
                                detailFragment.setOnProductDeletedListener(this@GalleryFragment)
                                detailFragment.show(parentFragmentManager, "product_detail")
                            }
                        }
                    })
                }
            }
        }
    }

    override fun onFilterApplied(minPrice: Double, maxPrice: Double, category: String) {
        TODO("Not yet implemented")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}