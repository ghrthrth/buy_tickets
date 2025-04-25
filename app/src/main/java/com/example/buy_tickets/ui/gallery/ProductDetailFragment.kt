package com.example.buy_tickets.ui.gallery

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.buy_tickets.R
import com.example.buy_tickets.ui.user.UserPreferences
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.Animation
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ProductDetailFragment(
    private val context: Context,
    private val id: Int,
    private val title: String,
    private val description: String,
    private val imageUrl: String,
    private val latitude: Double,
    private val longitude: Double,
    private var onProductDeleteListener: OnProductDeleteListener? = null
) : BottomSheetDialogFragment() {

    private lateinit var userPreferences: UserPreferences
    private var mapView: MapView? = null
    private var onProductBuyListener: OnProductBuyListener? = null
    private var onProductEditListener: OnProductEditListener? = null
    private var onProductDeletedListener: OnProductDeletedListener? = null
    private var currentPhotoPath: String? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            dispatchTakePictureIntent()
        } else {
            Toast.makeText(context, "Разрешение на камеру необходимо для изменения фото", Toast.LENGTH_SHORT).show()
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_product_detail, container, false)

        userPreferences = UserPreferences(context)

        // Инициализация View-элементов
        val imageView = view.findViewById<ImageView>(R.id.product_image)
        val titleTextView = view.findViewById<TextView>(R.id.title)
        val descriptionTextView = view.findViewById<TextView>(R.id.description)
        val appointmentButton = view.findViewById<Button>(R.id.button_appointment)
        val editButton = view.findViewById<Button>(R.id.button_edit)
        val deleteButton = view.findViewById<Button>(R.id.button_delete)
        mapView = view.findViewById(R.id.mapview)

        // Установка данных
        Picasso.get().load(imageUrl).into(imageView)
        titleTextView.text = "Название услуги: $title"
        descriptionTextView.text = "Описание услуги: $description"

        // Показываем кнопки редактирования и удаления только для администратора
        if (userPreferences.isAdmin()) {
            editButton.visibility = View.VISIBLE
            deleteButton.visibility = View.VISIBLE
        } else {
            editButton.visibility = View.GONE
            deleteButton.visibility = View.GONE
        }

        // Настройка обработчиков кнопок
        deleteButton.setOnClickListener { deleteProduct() }
        editButton.setOnClickListener { showEditDialog() }
        appointmentButton.setOnClickListener {
            onProductBuyListener?.onProductBuy(id.toString(), title)
        }

        // Настройка карты
        mapView?.getMap()?.let { map ->
            val point = Point(latitude, longitude)
            val placemark = map.mapObjects.addPlacemark(point)
            placemark.setIcon(ImageProvider.fromResource(requireContext(), R.drawable.pin_green))

            map.move(
                CameraPosition(point, 14.0f, 0.0f, 0.0f),
                Animation(Animation.Type.SMOOTH, 1f),
                null
            )
        }

        // Настройка BottomSheet
        dialog?.setOnShowListener { dialogInterface ->
            val bottomSheet = (dialogInterface as BottomSheetDialog).findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                            behavior.state = BottomSheetBehavior.STATE_EXPANDED
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                })
            }
        }

        return view
    }

    private fun showEditDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_product, null)
        val dialog = AlertDialog.Builder(context)
            .setTitle("Редактирование услуги")
            .setView(dialogView)
            .setNegativeButton("Отмена", null)
            .create()

        val imageView = dialogView.findViewById<ImageView>(R.id.edit_product_image)
        val titleEdit = dialogView.findViewById<TextInputEditText>(R.id.edit_product_title)
        val descEdit = dialogView.findViewById<TextInputEditText>(R.id.edit_product_description)
        val changeImageBtn = dialogView.findViewById<Button>(R.id.button_change_image)
        val saveBtn = dialogView.findViewById<Button>(R.id.button_save_changes)

        // Заполняем текущими значениями
        Picasso.get().load(imageUrl).into(imageView)
        titleEdit?.setText(title)
        descEdit?.setText(description)

        changeImageBtn?.setOnClickListener {
            showImagePickerOptions()
        }

        saveBtn?.setOnClickListener {
            val newTitle = titleEdit?.text.toString().trim()
            val newDescription = descEdit?.text.toString().trim()

            if (newTitle.isEmpty() || newDescription.isEmpty()) {
                Toast.makeText(context, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Если есть новое изображение (currentPhotoPath), используем его, иначе передаем null
            val imageUri = currentPhotoPath?.let { path ->
                Uri.fromFile(File(path))
            }
            updateProduct(id, newTitle, newDescription, imageUri)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showImagePickerOptions() {
        val options = arrayOf<CharSequence>("Сделать фото", "Выбрать из галереи", "Отмена")
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Выберите изображение")
        builder.setItems(options) { dialog, item ->
            when {
                options[item] == "Сделать фото" -> checkCameraPermissionAndTakePhoto()
                options[item] == "Выбрать из галереи" -> pickImageFromGallery()
                options[item] == "Отмена" -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun checkCameraPermissionAndTakePhoto() {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                dispatchTakePictureIntent()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(context.packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePictureResult.launch(takePictureIntent)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = context.getExternalFilesDir(null)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageResult.launch(intent)
    }

    private fun updateProduct(
        productId: Int,
        title: String,
        description: String,
        imageUri: Uri? // Uri файла изображения (может быть null, если не меняется)
    ) {
        val url = "https://decadances.ru/buy_tickets/admin_api/update.php"

        // Создаем MultipartBody.Builder
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("id_service", productId.toString())
            .addFormDataPart("title", title)
            .addFormDataPart("description", description)

        // Если есть новое изображение, добавляем его
        imageUri?.let { uri ->
            val file = File(uri.path ?: return@let)
            if (file.exists()) {
                val filePart = RequestBody.create("image/*".toMediaType(), file)
                requestBody.addFormDataPart(
                    "photo",
                    file.name,
                    filePart
                )
            }
        }

        val request = Request.Builder()
            .url(url)
            .post(requestBody.build())
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "Ошибка при обновлении", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                activity?.runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Изменения сохранены", Toast.LENGTH_SHORT).show()
                        // Закрываем фрагмент
                        dismiss()
                        // Уведомляем слушателя о необходимости обновить данные
                        onProductEditListener?.onProductEdited()
                    } else {
                        Toast.makeText(context, "Ошибка сервера", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun deleteProduct() {
        val url = "https://decadances.ru/buy_tickets/admin_api/delete.php"

        val json = JSONObject().apply {
            put("service_id", id)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Log.e("DeleteProduct", "Ошибка при удалении", e)
                    Toast.makeText(context, "Ошибка при удалении", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: "Пустой ответ"
                Log.d("DeleteProduct", "Ответ сервера: $responseBody")

                activity?.runOnUiThread {
                    if (response.isSuccessful) {
                        Log.i("DeleteProduct", "Успешное удаление, ID: $id")
                        onProductDeletedListener?.onProductDeleted(id)
                        dismiss()
                        Toast.makeText(context, "Удалено успешно", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("DeleteProduct", "Ошибка сервера: ${response.code}, тело ответа: $responseBody")
                        Toast.makeText(context, "Ошибка сервера", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
    private val takePictureResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let { path ->
                val imageView = dialog?.findViewById<ImageView>(R.id.edit_product_image)
                imageView?.setImageURI(Uri.fromFile(File(path)))
            }
        }
    }

    private val pickImageResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                currentPhotoPath = getPathFromUri(uri)
                val imageView = dialog?.findViewById<ImageView>(R.id.edit_product_image)
                imageView?.setImageURI(uri)
            }
        }
    }

    @SuppressLint("Range")
    private fun getPathFromUri(uri: Uri): String {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndex(MediaStore.Images.Media.DATA))
            }
        }
        return uri.path ?: ""
    }
    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        mapView?.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    fun setOnProductDeletedListener(listener: OnProductDeletedListener) {
        this.onProductDeletedListener = listener
    }

    interface OnProductDeletedListener {
        fun onProductDeleted(productId: Int)
    }

    fun setOnProductBuyListener(listener: OnProductBuyListener) {
        this.onProductBuyListener = listener
    }

    fun setOnProductEditListener(listener: OnProductEditListener) {
        this.onProductEditListener = listener
    }
    fun setOnProductDeleteListener(listener: OnProductDeleteListener) {
        this.onProductDeleteListener = listener
    }

    interface OnProductDeleteListener {
        fun onProductDeleted(productId: Int)
    }

    interface OnProductBuyListener {
        fun onProductBuy(productId: String, productName: String)
    }

    interface OnProductEditListener {
        fun onProductEdited() // Теперь без параметров
    }

    companion object {
        const val TAG = "ProductDetailFragment"
    }
}