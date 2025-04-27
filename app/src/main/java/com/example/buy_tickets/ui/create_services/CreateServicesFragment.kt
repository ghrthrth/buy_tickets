package com.example.buy_tickets.ui.create_services

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.buy_tickets.R
import com.example.buy_tickets.databinding.FragmentCreateServicesBinding
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.search.SuggestItem
import com.yandex.mapkit.search.SuggestOptions
import com.yandex.mapkit.search.SuggestResponse
import com.yandex.mapkit.search.SuggestSession
import com.yandex.mapkit.search.SuggestType
import com.yandex.runtime.Error
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.IOException
import java.util.Timer
import java.util.TimerTask

class CreateServicesFragment : Fragment() {

    private val REQUEST_CODE_PICK_IMAGE = 1
    private val REQUEST_CODE_PERMISSION = 2


    private lateinit var searchManager: SearchManager
    private lateinit var suggestSession: SuggestSession
    private lateinit var addressEditText: TextInputEditText

    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null

    private val client = OkHttpClient()

    private var selectedImageUri: Uri? = null

    private var _binding: FragmentCreateServicesBinding? = null
    private val binding get() = _binding!!

    private lateinit var addressAutoComplete: MaterialAutoCompleteTextView // Измените тип

// В onCreateView:

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val createServicesViewModel = ViewModelProvider(this).
        get(CreateServicesViewModel::class.java)

        _binding = FragmentCreateServicesBinding.inflate(inflater, container, false)
        val root = binding.root

        val title = binding.title
        val description = binding.description
        val send = binding.send
        val selectPhoto = binding.selectPhotos

        addressAutoComplete = binding.address as MaterialAutoCompleteTextView

        Log.d("YandexSuggest", "addressAutoComplete: ${addressAutoComplete.id}")

        SearchFactory.getInstance()
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
        suggestSession = searchManager.createSuggestSession()
        // Инициализация поискового менеджера
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
        suggestSession = searchManager.createSuggestSession()
        setupAddressSuggestions()


        send.setOnClickListener {
            val titles = title.text.toString()
            val descriptions = description.text.toString()
            HttpRequestTask().execute(titles, descriptions)
        }


        selectPhoto.setOnClickListener {
            Log.d("PhotoSelect", "Button clicked")

            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.
                PERMISSION_GRANTED) {
                // Запрашиваем разрешение
                requestPermissions(arrayOf(permission), REQUEST_CODE_PERMISSION)
            } else {
                // Разрешение уже есть - открываем галерею
                openGallery()
            }
        }


        return root
    }


    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("PhotoSelect", "onActivityResult: requestCode=$requestCode, " +
                "resultCode=$resultCode")
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == android.app.Activity.RESULT_OK) {
            selectedImageUri = data?.data
            Log.d("PhotoSelect", "Selected URI: $selectedImageUri")
        }
    }

    private fun getRealPathFromUri(uri: Uri): String? {
        var realPath: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = requireActivity().contentResolver.query(uri, projection,
            null, null, null)
        cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            it.moveToFirst()
            realPath = it.getString(columnIndex)
        }
        return realPath ?: uri.path
    }

    private fun setupAddressSuggestions() {
        addressAutoComplete.threshold = 1

        addressAutoComplete.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.adapter.getItem(position) as String
            // Здесь можно получить координаты выбранного адреса
            getCoordinatesForAddress(selectedItem)
        }

        addressAutoComplete.addTextChangedListener(object : TextWatcher {
            private var timer: Timer? = null
            private val DELAY = 500L

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                timer?.cancel()
                timer = Timer().apply {
                    schedule(object : TimerTask() {
                        override fun run() {
                            activity?.runOnUiThread {
                                if (!s.isNullOrEmpty() && s.length >= 1) {
                                    fetchSuggestions(s.toString())
                                }
                            }
                        }
                    }, DELAY)
                }
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Для выбора фото необходимо предоставить разрешение",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getCoordinatesForAddress(address: String) {
        val suggestOptions = SuggestOptions().apply {
            suggestTypes = SuggestType.GEO.value or SuggestType.BIZ.value
        }

        suggestSession.suggest(
            address,
            BoundingBox(Point(-90.0, -180.0), Point(90.0, 180.0)),
            suggestOptions,
            object : SuggestSession.SuggestListener {
                override fun onResponse(response: SuggestResponse) {
                    if (response.items.isNotEmpty()) {
                        val item = response.items[0]
                        if (item.center != null) {
                            selectedLatitude = item.center!!.latitude
                            selectedLongitude = item.center!!.longitude
                            Log.d("Coordinates", "Lat: $selectedLatitude, " +
                                    "Lon: $selectedLongitude")
                        }
                    }
                }

                override fun onError(error: Error) {
                    Log.e("SearchError", "Error getting coordinates: $error")
                }
            }
        )
    }

    private fun fetchSuggestions(query: String) {
        Log.d("YandexSuggest", "Fetching suggestions for: $query")

        val boundingBox = BoundingBox(
            Point(-90.0, -180.0), // Whole world
            Point(90.0, 180.0)
        )

        val suggestOptions = SuggestOptions().apply {
            setSuggestTypes(SuggestType.GEO.value or SuggestType.BIZ.value)
            userPosition = Point(55.751244, 37.618423) // Center of Moscow
        }

        suggestSession.suggest(
            query,
            boundingBox,
            suggestOptions,
            object : SuggestSession.SuggestListener {
                override fun onResponse(response: SuggestResponse) {
                    Log.d("YandexSuggest", "Response received")
                    activity?.runOnUiThread {
                        showSuggestions(response.items)
                    }
                }

                override fun onError(error: Error) {
                    Log.e("YandexSuggest", "API Error: ${error}")
                    activity?.runOnUiThread {
                        // Handle error if needed
                    }
                }
            })
    }

    private fun showSuggestions(suggestItems: List<SuggestItem>) {
        // Extract display texts from suggest items
        val suggestions = suggestItems.map { it.displayText }
        Log.d("YandexSuggest", "Suggestions to show: $suggestions")

        if (suggestions.isNotEmpty()) {
            val adapter = ArrayAdapter(
                requireActivity(),
                android.R.layout.simple_dropdown_item_1line,
                suggestions
            )
            addressAutoComplete.setAdapter(adapter)

            // Show dropdown if there are suggestions
            addressAutoComplete.post {
                addressAutoComplete.showDropDown()
                Log.d("YandexSuggest", "Dropdown shown with ${suggestions.size} items")
            }
        } else {
            Log.d("YandexSuggest", "No suggestions to show")
        }
    }


    private inner class HttpRequestTask : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String): String {
            val title = params[0]
            val description = params[1]

            return try {
                val builder = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("title", title)
                    .addFormDataPart("description", description)

                // Добавляем координаты, если они есть
                if (selectedLatitude != null && selectedLongitude != null) {
                    builder.addFormDataPart("latitude", selectedLatitude.toString())
                    builder.addFormDataPart("longitude", selectedLongitude.toString())
                    Log.d("HttpRequest", "Coordinates added: $selectedLatitude, " +
                            "$selectedLongitude")
                }

                selectedImageUri?.let { uri ->
                    val inputStream = requireActivity().contentResolver.openInputStream(uri)
                    inputStream?.use {
                        val fileName = getFileName(uri) ?: "image_${System.currentTimeMillis()}.jpg"
                        val fileBody = RequestBody.create(
                            "image/*".toMediaTypeOrNull(),
                            it.readBytes()
                        )
                        builder.addFormDataPart(
                            "photo",
                            fileName,
                            fileBody
                        )
                        Log.d("HttpRequest", "File added: $fileName")
                    }
                }

                val requestBody = builder.build()
                val request = Request.Builder()
                    .url("https://decadances.ru/buy_tickets/admin_api/add.php")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                Log.d("HttpRequest", "Response: ${response.code} - $responseBody")
                responseBody ?: "No response"
            } catch (e: Exception) {
                Log.e("HttpRequest", "Error: ${e.message}", e)
                "Error: ${e.message}"
            }
        }

        private fun getFileName(uri: Uri): String? {
            var name: String? = null
            val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
            val cursor = requireActivity().contentResolver.query(uri, projection,
                null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    name = it.getString(0)
                }
            }
            return name
        }

        override fun onPostExecute(result: String) {
            try {
                Log.d("HttpRequest", "Final result: $result")

                if (result.contains("error", ignoreCase = true)) {
                    Toast.makeText(context, "Ошибка: $result", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Данные успешно отправлены", Toast.LENGTH_SHORT).show()
                    binding.title.text?.clear()
                    binding.description.text?.clear()
                    selectedImageUri = null
                    selectedLatitude = null
                    selectedLongitude = null
                }
            } catch (e: Exception) {
                Log.e("HttpRequest", "Post execute error", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        client.dispatcher.cancelAll()
    }

    override fun onDetach() {
        super.onDetach()
        // Очищаем поле адреса только когда фрагмент действительно покинут
        addressAutoComplete.text?.clearSpans()
        selectedLatitude = null
        selectedLongitude = null
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        super.onStop()
        MapKitFactory.getInstance().onStop()

    }
}