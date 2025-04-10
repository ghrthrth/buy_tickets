package com.example.buy_tickets.ui.gallery

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.buy_tickets.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.squareup.picasso.Picasso
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider

class ProductDetailFragment(
    private val context: Context,
    private val id: Int,
    private val title: String,
    private val description: String,
    private val imageUrl: String,
    private val latitude: Double,
    private val longitude: Double
) : BottomSheetDialogFragment() {

    private var mapView: MapView? = null
    private var onProductDeletedListener: OnProductDeletedListener? = null

    private var onProductBuyListener: OnProductBuyListener? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_product_detail, container, false)

        // Инициализация View-элементов
        val imageView = view.findViewById<ImageView>(R.id.product_image)
        val titleTextView = view.findViewById<TextView>(R.id.title)
        val descriptionTextView = view.findViewById<TextView>(R.id.description)
        val appointmentButton = view.findViewById<Button>(R.id.button_appointment)
        mapView = view.findViewById(R.id.mapview)

        // Установка данных
        Picasso.get().load(imageUrl).into(imageView)
        titleTextView.text = "Название услуги: $title"
        descriptionTextView.text = "Описание услуги: $description"

        // Настройка обработки касаний
        mapView?.apply {
            // Разрешаем карте обрабатывать все касания
            (parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(true)
        }

        view.setOnTouchListener { v, event ->
            val rect = Rect()
            appointmentButton.getGlobalVisibleRect(rect)
            if (!rect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                mapView?.dispatchTouchEvent(event)
            }
            true
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

        appointmentButton.setOnClickListener {
            onProductBuyListener?.onProductBuy(id.toString(), title)
        }

        return view
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

    fun setOnProductBuyListener(listeners: OnProductBuyListener) {
        this.onProductBuyListener = listeners
    }

    interface OnProductDeletedListener {
        fun onProductDeleted(productId: Int)
    }

    fun interface OnProductBuyListener {
        fun onProductBuy(productId: String, productName: String)
    }

    companion object {
        const val TAG = "ProductDetailFragment"
    }
}