package com.example.buy_tickets.ui.applications

import android.os.Bundle
import android.util.Log
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

class ApplicationsFragment : Fragment(),
    ApplicationDetailFragment.OnApplicationDeletedListener {

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

    override fun onApplicationDeleted() {
        // Обновляем список заявок
        getPhotoUrlsFromServer()
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
        if (!isAdded) {
            Log.d("PARSE_DEBUG", "Fragment not attached, aborting")
            return
        }

        val url = "https://decadances.ru/buy_tickets/api/add_application/return.php"
        Log.d("PARSE_DEBUG", "Starting request to: $url")

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("PARSE_ERROR", "Network request failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (!response.isSuccessful) {
                        Log.e("PARSE_ERROR", "Unsuccessful response. Code: ${response.code}")
                        return
                    }

                    val json = response.body?.string()
                    if (json.isNullOrEmpty()) {
                        Log.e("PARSE_ERROR", "Response body is null or empty")
                        return
                    }

                    Log.d("PARSE_DEBUG", "Raw JSON response: $json")

                    val jsonObject = JSONObject(json)
                    logJsonStructure(jsonObject) // Логируем структуру JSON

                    val ids = parseAndLogArray(jsonObject, "user_id", "user_ids")
                    val serviceIds = parseAndLogArray(jsonObject, "service_id", "service_ids")
                    val productNames = parseAndLogArray(jsonObject, "product_name", "product_names")
                    val firstNames = parseAndLogArray(jsonObject, "first_name", "first_names")
                    val lastNames = parseAndLogArray(jsonObject, "last_name", "last_names")
                    val phones = parseAndLogArray(jsonObject, "phone", "phones")
                    val dates = parseAndLogArray(jsonObject, "dates", "dates")
                    val times = parseAndLogArray(jsonObject, "times", "times")
                    val email = parseAndLogArray(jsonObject, "email", "email")
                    // Проверка согласованности массивов
                    if (!checkArraysConsistency(ids, serviceIds, productNames, firstNames, lastNames, phones, dates, times, email)) {
                        Log.e("PARSE_ERROR", "Arrays have inconsistent lengths")
                        return
                    }

                    Log.d("PARSE_SUCCESS", "Successfully parsed ${ids.size} items")

                    // Вывод первых 3 элементов для проверки
                    logSampleData(ids, serviceIds, productNames, firstNames, lastNames, phones, dates, times)

                    activity?.runOnUiThread {
                        displayPhotosInGrid(ids, serviceIds, productNames, firstNames, lastNames, phones, dates, times, email)
                    }

                } catch (e: JSONException) {
                    Log.e("PARSE_ERROR", "JSON parsing error", e)
                } catch (e: Exception) {
                    Log.e("PARSE_ERROR", "Unexpected error", e)
                }
            }
        })
    }

    private fun logJsonStructure(jsonObject: JSONObject) {
        try {
            val keys = jsonObject.keys()
            val keyList = mutableListOf<String>()
            while (keys.hasNext()) {
                keyList.add(keys.next())
            }
            Log.d("PARSE_DEBUG", "JSON structure - available keys: $keyList")

            // Логируем тип и размер каждого массива
            keyList.forEach { key ->
                try {
                    val array = jsonObject.getJSONArray(key)
                    Log.d("PARSE_DEBUG", "Key '$key' is array with ${array.length()} elements")
                } catch (e: JSONException) {
                    Log.d("PARSE_DEBUG", "Key '$key' is not an array or missing")
                }
            }
        } catch (e: Exception) {
            Log.e("PARSE_ERROR", "Error logging JSON structure", e)
        }
    }

    private fun parseAndLogArray(jsonObject: JSONObject, key: String, debugName: String): List<String> {
        return try {
            val array = jsonObject.getJSONArray(key)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            Log.d("PARSE_DEBUG", "Parsed $debugName: ${list.size} items")
            list
        } catch (e: JSONException) {
            Log.e("PARSE_ERROR", "Failed to parse array '$key'", e)
            emptyList()
        }
    }

    private fun checkArraysConsistency(vararg arrays: List<Any>): Boolean {
        if (arrays.isEmpty()) return true
        val firstSize = arrays[0].size
        arrays.forEachIndexed { index, list ->
            if (list.size != firstSize) {
                Log.e("PARSE_ERROR", "Array at position $index has size ${list.size}, expected $firstSize")
                return false
            }
        }
        return true
    }

    private fun logSampleData(
        ids: List<String>,
        serviceIds: List<String>,
        productNames: List<String>,
        firstNames: List<String>,
        lastNames: List<String>,
        phones: List<String>,
        dates: List<String>,
        times: List<String>
    ) {
        val sampleSize = minOf(3, ids.size)
        if (sampleSize == 0) {
            Log.d("PARSE_DEBUG", "No data to display")
            return
        }

        Log.d("PARSE_SAMPLE", "=== Sample data (first $sampleSize items) ===")
        for (i in 0 until sampleSize) {
            Log.d("PARSE_SAMPLE", """
            Item $i:
            - user_id: ${ids.getOrNull(i)}
            - service_id: ${serviceIds.getOrNull(i)}
            - product_name: ${productNames.getOrNull(i)}
            - first_name: ${firstNames.getOrNull(i)}
            - last_name: ${lastNames.getOrNull(i)}
            - phone: ${phones.getOrNull(i)}
            - date: ${dates.getOrNull(i)}
            - time: ${times.getOrNull(i)}
        """.trimIndent())
        }
    }
    fun displayPhotosInGrid(
        ids: List<String>,
        service_ids: List<String>,
        product_name: List<String>,
        firstNames: List<String>,
        lastNames: List<String>,
        phones: List<String>,
        dates: List<String>,
        times: List<String>,
        email: List<String>
    ) {
        if (activity == null || binding == null) {
            return
        }
        if (!isAdded) return

        // Создаем новый адаптер с обновленными данными
        val gridView = binding!!.gridView
        val adapter = ImageAdapter(
            requireContext(),
            ids.toMutableList(),
            service_ids.toMutableList(),
            product_name.toMutableList(),
            firstNames.toMutableList(),
            lastNames.toMutableList(),
            phones.toMutableList(),
            dates.toMutableList(),
            times.toMutableList(),
            email.toMutableList()
        )
        gridView.adapter = adapter
        gridView.setOnItemClickListener { parent, view, position, id ->
            val selectedUserId = ids[position]
            val selectedServiceId = service_ids[position]
            val selectedProductName = product_name[position]
            val selectedFirstName = firstNames[position]
            val selectedLastName = lastNames[position]
            val selectedPhone = phones[position]
            val selectedDate = dates[position]
            val selectedTime = times[position]
            val selectedEmail = email[position]
            val bottomSheet = ApplicationDetailFragment(
                requireContext(),
                selectedUserId,
                selectedServiceId,
                selectedProductName,
                selectedFirstName,
                selectedLastName,
                selectedPhone,
                selectedDate,
                selectedTime,
                selectedEmail
            )
            bottomSheet.setDeletionListener(this) // Устанавливаем слушателя
            bottomSheet.show(parentFragmentManager, bottomSheet.tag)
        }
    }

    fun updateApplications() {
        // Повторно запрашиваем данные с сервера
        getPhotoUrlsFromServer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null

        // Отменяем все асинхронные запросы
        client.dispatcher.cancelAll()
    }
}