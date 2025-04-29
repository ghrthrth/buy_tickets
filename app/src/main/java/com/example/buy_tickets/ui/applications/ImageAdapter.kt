package com.example.buy_tickets.ui.applications

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.example.buy_tickets.R

class ImageAdapter(
    private val mContext: Context,
    private val mIds: MutableList<String>,
    private val mServiceIds: MutableList<String>,
    private val mProductName: MutableList<String>,
    private val mFirstNames: MutableList<String>,
    private val mLastNames: MutableList<String>,
    private val mPhones: MutableList<String>,
    private val mDates: MutableList<String>,
    private val mTimes: MutableList<String>
) : BaseAdapter() {

    private val mInflater = LayoutInflater.from(mContext)

    override fun getCount(): Int = mIds.size

    override fun getItem(position: Int): Any = mIds[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: mInflater.inflate(R.layout.grid_item_applications, parent, false)

        view.findViewById<TextView>(R.id.TITLE_text_view).text = "Продукт: ${mProductName[position]}"
        view.findViewById<TextView>(R.id.NAME_text_view).text = "Имя: ${mFirstNames[position]}"
        view.findViewById<TextView>(R.id.SURNAME_text_view).text = "Фамилия: ${mLastNames[position]}"
        view.findViewById<TextView>(R.id.PHONE_text_view).text = "Телефон: ${mPhones[position]}"
        view.findViewById<TextView>(R.id.DATE_text_view).text = "Дата: ${mDates[position]}"
        view.findViewById<TextView>(R.id.TIME_text_view).text = "Время: ${mTimes[position]}"

        view.findViewById<TextView>(R.id.PRODUCT_QUANTITY_text_view).visibility = View.GONE

        return view
    }

}