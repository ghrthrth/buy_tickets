package com.example.buy_tickets.ui.gallery

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Rect
import android.net.Uri
import android.os.Build
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
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
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
    private var onProductDeleteListener: OnProductDeleteListener? = null,
    private val REQUEST_CODE_PICK_IMAGE: Int = 1,
    private val REQUEST_CODE_PERMISSION: Int = 2,
    private var selectedImageUri: Uri? = null,
    private var editImageView: ImageView? = null



) : BottomSheetDialogFragment() {

    private lateinit var userPreferences: UserPreferences
    private var mapView: MapView? = null
    private var onProductBuyListener: OnProductBuyListener? = null
    private var onProductEditListener: OnProductEditListener? = null
    private var onProductDeletedListener: OnProductDeletedListener? = null


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

        editButton.setOnClickListener {
            showEditDialog()
        }

        return view
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            editImageView?.let { imageView ->
                Picasso.get()
                    .load(it)
                    .into(imageView)
            }
        }
    }

    // Заменяем `openGallery()` на:
    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedImageUri = uri
                // Находим ImageView в диалоге и обновляем его
                (dialog as? AlertDialog)?.findViewById<ImageView>(R.id.edit_product_image)?.let { imageView ->
                    Picasso.get()
                        .load(uri)
                        .into(imageView)
                }
            }
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
    private fun showEditDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_product, null)
        val dialog = AlertDialog.Builder(context)
            .setTitle("Редактирование услуги")
            .setView(dialogView)
            .setNegativeButton("Отмена", null)
            .create()

        editImageView = dialogView.findViewById(R.id.edit_product_image)
        val titleEdit = dialogView.findViewById<TextInputEditText>(R.id.edit_product_title)
        val descEdit = dialogView.findViewById<TextInputEditText>(R.id.edit_product_description)
        val changeImageBtn = dialogView.findViewById<Button>(R.id.button_change_image)
        val saveBtn = dialogView.findViewById<Button>(R.id.button_save_changes)

        // Функция для обновления изображения
        fun updateImage() {
            // При открытии диалога загружаем текущее изображение
            selectedImageUri?.let { uri ->
                Picasso.get().load(uri).into(editImageView)
            } ?: run {
                Picasso.get().load(imageUrl).into(editImageView)
            }
        }

        // Инициализация изображения
        updateImage()

        // Заполняем текущими значениями
        titleEdit?.setText(title)
        descEdit?.setText(description)

        changeImageBtn.setOnClickListener {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), REQUEST_CODE_PERMISSION)
            } else {
                openGallery()
            }
        }

        saveBtn?.setOnClickListener {
            val newTitle = titleEdit?.text.toString().trim()
            val newDescription = descEdit?.text.toString().trim()

            if (newTitle.isEmpty() || newDescription.isEmpty()) {
                Toast.makeText(context, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updateProduct(id, newTitle, newDescription, selectedImageUri)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateProduct(
    productId: Int,
    title: String,
    description: String,
    imageUri: Uri?
    ) {
        val url = "https://decadances.ru/buy_tickets/admin_api/update.php"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("id_service", productId.toString())
            .addFormDataPart("title", title)
            .addFormDataPart("description", description)

        imageUri?.let { uri ->
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val file = File.createTempFile("image", ".jpg", requireContext().cacheDir)
                FileOutputStream(file).use { output ->
                    stream.copyTo(output)
                }
                val filePart = file.asRequestBody("image/*".toMediaType())
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
                        Log.e("DeleteProduct", "Ошибка сервера: ${response.code}, " +
                                "тело ответа: $responseBody")
                        Toast.makeText(context, "Ошибка сервера", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
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