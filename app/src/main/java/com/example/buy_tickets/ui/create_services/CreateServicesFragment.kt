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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.buy_tickets.R
import com.example.buy_tickets.databinding.FragmentCreateServicesBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.IOException

class CreateServicesFragment : Fragment() {

    private val REQUEST_CODE_PICK_IMAGE = 1
    private val REQUEST_CODE_PERMISSION = 2

    private val client = OkHttpClient()

    private var selectedImageUri: Uri? = null

    private var _binding: FragmentCreateServicesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val createServicesViewModel = ViewModelProvider(this).get(CreateServicesViewModel::class.java)

        _binding = FragmentCreateServicesBinding.inflate(inflater, container, false)
        val root = binding.root

        val title = binding.title
        val description = binding.description
        val send = binding.send
        val selectPhoto = binding.selectPhotos

        selectPhoto.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_MEDIA_IMAGES), REQUEST_CODE_PERMISSION)
                } else {
                    openGallery()
                }
            } else {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE_PERMISSION)
                } else {
                    openGallery()
                }
            }
        }

        send.setOnClickListener {
            val titles = title.text.toString()
            val descriptions = description.text.toString()
            HttpRequestTask().execute(titles, descriptions)
        }

        return root
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == android.app.Activity.RESULT_OK) {
            selectedImageUri = data?.data
        }
    }

    private fun getRealPathFromUri(uri: Uri): String? {
        var realPath: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = requireActivity().contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            it.moveToFirst()
            realPath = it.getString(columnIndex)
        }
        return realPath ?: uri.path
    }

    private inner class HttpRequestTask : AsyncTask<String, Void, String>() {

        override fun onPreExecute() {
            super.onPreExecute()
            binding.loadingOverlay.visibility = View.VISIBLE
            binding.send.isEnabled = false
        }

        override fun doInBackground(vararg params: String): String {
            val title = params[0]
            val description = params[1]

            return try {
                val builder = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("title", title)
                    .addFormDataPart("description", description)

                selectedImageUri?.let { uri ->
                    val filePath = getRealPathFromUri(uri)
                    val file = File(filePath)
                    val mediaType = "image/*".toMediaTypeOrNull()
                    if (mediaType != null) {
                        builder.addFormDataPart("photo", file.name, RequestBody.create(mediaType, file))
                    }
                }

                val requestBody = builder.build()
                val request = Request.Builder()
                    .url("https://claimbes.store/buy_tickets/admin_api/add.php")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                response.body?.string() ?: "No response"
            } catch (e: IOException) {
                e.printStackTrace()
                "Error: ${e.message}"
            }
        }

        override fun onPostExecute(result: String) {
            binding.loadingOverlay.visibility = View.GONE
            binding.send.isEnabled = true

            if (result.contains("Error")) {
                Toast.makeText(context, "Ошибка: $result", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Данные успешно отправлены", Toast.LENGTH_SHORT).show()

                binding.title.setText("") // Используем setText() для установки текста
                binding.description.setText("")
                selectedImageUri = null
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        client.dispatcher.cancelAll()
    }
}