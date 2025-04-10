package com.example.buy_tickets.ui.applications

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.example.buy_tickets.R

class ImageAdapter (
    private val mContext: Context,
    private val mIds: MutableList<String>,
    private val mServiceIds: MutableList<String>,
    private val mTitles: MutableList<String>,
    private val mNames: MutableList<String>,
    private val mSurnames: MutableList<String>,
    private val mPhones: MutableList<String>,
    private val mDates: MutableList<String>,
    private val mTimes: MutableList<String>,
    private val mProduct_quantitys: MutableList<String>
) : BaseAdapter(), Filterable {

    private var mFilteredTitles = ArrayList(mTitles)
    private val mInflater = LayoutInflater.from(mContext)

    override fun getCount(): Int = mFilteredTitles.size

    override fun getItem(position: Int): Any? = null

    override fun getItemId(position: Int): Long = 0

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: mInflater.inflate(R.layout.grid_item_applications, parent, false)

        val titleTextView = view.findViewById<TextView>(R.id.TITLE_text_view)
        val nameTextView = view.findViewById<TextView>(R.id.NAME_text_view)
        val surnameTextView = view.findViewById<TextView>(R.id.SURNAME_text_view)
        val phoneTextView = view.findViewById<TextView>(R.id.PHONE_text_view)
        val datesTextView = view.findViewById<TextView>(R.id.DATE_text_view)
        val timesTextView = view.findViewById<TextView>(R.id.TIME_text_view)
        val product_quantitysTextView = view.findViewById<TextView>(R.id.PRODUCT_QUANTITY_text_view)

        val title = mFilteredTitles[position]
        val originalPosition = mTitles.indexOf(title)

        val name = mNames[position]
        val surname = mSurnames[position]
        val phone = mPhones[position]
        val date = mDates[position]
        val time = mTimes[position]
        val product_quantity = mProduct_quantitys[position]

        titleTextView.text = "Продукт: $title"
        nameTextView.text = "Имя заказчика: $name"
        surnameTextView.text = "Фамилия заказчика: $surname"
        phoneTextView.text = "Телефон заказчика: $phone"
        datesTextView.text = "Дата заказа: $date"
        timesTextView.text = "Время заказа: $time"
        product_quantitysTextView.text = "Количество: $product_quantity"

        return view
    }

    override fun getFilter(): Filter? {
        TODO("Not yet implemented")
    }

}