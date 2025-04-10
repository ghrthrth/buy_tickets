package com.example.buy_tickets.ui.applications


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.NonNull
import com.example.buy_tickets.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ApplicationDetailFragment(
    private val mContext: Context,
    private val user_id: String,
    private val service_id: String,
    private val title: String,
    private val name: String,
    private val surname: String,
    private val phone: String,
    private val date: String,
    private val time: String,
    private val product_quantity: String
) : BottomSheetDialogFragment() {

    private lateinit var adapter: ImageAdapter
    private var position: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_application_detail, container, false)

        // Заполнение полей информацией о товаре
        val titleTextView = view.findViewById<TextView>(R.id.title)
        val nameTextView = view.findViewById<TextView>(R.id.name)
        val surnameTextView = view.findViewById<TextView>(R.id.surname)
        val phoneTextView = view.findViewById<TextView>(R.id.phone)
        val dateTextView = view.findViewById<TextView>(R.id.date)
        val timeTextView = view.findViewById<TextView>(R.id.time)
        val productQuantityTextView = view.findViewById<TextView>(R.id.product_quantity)
        val sendDataButton = view.findViewById<Button>(R.id.button_appointment)

        titleTextView.text = "Продукт: $title"
        nameTextView.text = "Имя заказчика: $name"
        surnameTextView.text = "Фамилия заказчика: $surname"
        phoneTextView.text = "Телефон: $phone"
        dateTextView.text = "Дата заказа: $date"
        timeTextView.text = "Время заказа: $time"
        productQuantityTextView.text = "Количество: $product_quantity"

        val params = HashMap<String, String>().apply {
            put("user_id", user_id)
            put("service_id", service_id)
            put("product_quantity", product_quantity)
            put("date", date)
            put("time", time)
        }

        sendDataButton.setOnClickListener {
            HttpRequestTask(mContext, "https://decadances.store/telestock/api/add_application/delete.php", params).execute()
            dismiss()
        }

        return view
    }
}