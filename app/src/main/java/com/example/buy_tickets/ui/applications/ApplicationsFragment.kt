package com.example.buy_tickets.ui.applications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.buy_tickets.databinding.FragmentApplicationsBinding
import com.example.buy_tickets.ui.home.HomeViewModel
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class ApplicationsFragment : Fragment() {

    private var binding: FragmentApplicationsBinding? = null
    private val client = OkHttpClient()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentApplicationsBinding.inflate(inflater, container, false)
        val root = binding!!.root

        getPhotoUrlsFromServer()
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
        if (!isAdded) return // Проверяем, не был ли фрагмент уничтожен

        val url = "https://decadances.store/telestock/api/add_application/return.php" // Замените на ваш URL-адрес сервера

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    try {
                        val jsonObject = JSONObject(json)
                        val user_idArray = jsonObject.getJSONArray("user_id")
                        val service_idArray = jsonObject.getJSONArray("id_service")
                        val titleArray = jsonObject.getJSONArray("title")
                        val nameArray = jsonObject.getJSONArray("name")
                        val surnameArray = jsonObject.getJSONArray("surname")
                        val phoneArray = jsonObject.getJSONArray("phone")
                        val datesArray = jsonObject.getJSONArray("dates")
                        val timesArray = jsonObject.getJSONArray("times")
                        val product_quantityArray = jsonObject.getJSONArray("product_quantity")

                        val ids = mutableListOf<String>()
                        val service_ids = mutableListOf<String>()
                        val titles = mutableListOf<String>()
                        val names = mutableListOf<String>()
                        val surnames = mutableListOf<String>()
                        val phones = mutableListOf<String>()
                        val dates = mutableListOf<String>()
                        val times = mutableListOf<String>()
                        val product_quantitys = mutableListOf<String>()

                        addItemsToList(user_idArray, ids)
                        addItemsToList(service_idArray, service_ids)
                        addItemsToList(titleArray, titles)
                        addItemsToList(nameArray, names)
                        addItemsToList(surnameArray, surnames)
                        addItemsToList(phoneArray, phones)
                        addItemsToList(datesArray, dates)
                        addItemsToList(timesArray, times)
                        addItemsToList(product_quantityArray, product_quantitys)

                        displayPhotosInGrid(ids, service_ids, titles, names, surnames, phones, dates, times, product_quantitys)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun displayPhotosInGrid(
        ids: List<String>,
        service_ids: List<String>,
        titles: List<String>,
        names: List<String>,
        surnames: List<String>,
        phones: List<String>,
        dates: List<String>,
        times: List<String>,
        product_quantitys: List<String>
    ) {
        if (activity == null || binding == null) {
            return // Предотвращение краша, если фрагмент уничтожен
        }
        if (!isAdded) return // Проверяем, не был ли фрагмент уничтожен

        activity?.runOnUiThread {
            if (binding == null) return@runOnUiThread // Проверяем повторно

            val gridView = binding!!.gridView
            val adapter = ImageAdapter(
                requireContext(),
                ids as MutableList<String>,
                service_ids as MutableList<String>,
                titles as MutableList<String>,
                names as MutableList<String>,
                surnames as MutableList<String>,
                phones as MutableList<String>,
                dates as MutableList<String>,
                times as MutableList<String>,
                product_quantitys as MutableList<String>
            )
            gridView.adapter = adapter


            gridView.onItemClickListener =
                AdapterView.OnItemClickListener { parent, view, position, id ->
                    if (binding == null) return@OnItemClickListener // Проверяем, не уничтожен ли фрагмент

                    val selectedUserId = ids[position]
                    val selectedServiceId = service_ids[position]
                    val selectedTitle = titles[position]
                    val selectedNames = names[position]
                    val selectedSurnames = surnames[position]
                    val selectedPhones = phones[position]
                    val selectedDates = dates[position]
                    val selectedTimes = times[position]
                    val selectedProduct_quantitys = product_quantitys[position]

                    val detailFragment = ApplicationDetailFragment(
                        requireContext(),
                        selectedUserId,
                        selectedServiceId,
                        selectedTitle,
                        selectedNames,
                        selectedSurnames,
                        selectedPhones,
                        selectedDates,
                        selectedTimes,
                        selectedProduct_quantitys
                    )

                    parentFragmentManager?.let {
                        detailFragment.show(it, "application_detail")
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null

        // Отменяем все асинхронные запросы
        client.dispatcher.cancelAll()
    }

}