package com.example.buy_tickets.ui.gallery

import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.example.buy_tickets.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.squareup.picasso.Picasso
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException

class ProductDetailFragment(
    private val context: Context,
    private val id: Int,
    private val title: String,
    private val description: String,
    private val imageUrl: String
) : BottomSheetDialogFragment() {

    private var onProductDeletedListener: OnProductDeletedListener? = null

    fun setOnProductDeletedListener(listener: OnProductDeletedListener) {
        this.onProductDeletedListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_product_detail, container, false)

        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val userLogin = sharedPreferences.getString("login", "") // Получаем логин пользователя

        val imageView = view.findViewById<ImageView>(R.id.product_image)
        val titleTextView = view.findViewById<TextView>(R.id.title)
        val descriptionTextView = view.findViewById<TextView>(R.id.description)
        val addToCartButton = view.findViewById<Button>(R.id.button_appointment)
        Picasso.get().load(imageUrl).into(imageView)
        titleTextView.text = "Название услуги: $title"
        descriptionTextView.text = "Описание услуги: $description"


        return view
    }




    interface OnProductDeletedListener {
        fun onProductDeleted(productId: Int)
    }
}