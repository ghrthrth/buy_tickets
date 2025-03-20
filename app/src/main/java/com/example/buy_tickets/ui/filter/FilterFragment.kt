package com.example.buy_tickets.ui.filter


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import com.example.buy_tickets.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.ArrayList

class FilterFragment : BottomSheetDialogFragment() {

    private var listener: FilterFragmentListener? = null
    private lateinit var categorySpinner: Spinner
    private var categories: List<String> = ArrayList()

    interface FilterFragmentListener {
        fun onFilterApplied(minPrice: Double, maxPrice: Double, category: String)
    }

    fun setFilterFragmentListener(listener: FilterFragmentListener) {
        this.listener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_filter, container, false)

        val minPriceEditText = view.findViewById<EditText>(R.id.minPriceEditText)
        val maxPriceEditText = view.findViewById<EditText>(R.id.maxPriceEditText)
        categorySpinner = view.findViewById(R.id.categorySpinner)
        val applyFilterButton = view.findViewById<Button>(R.id.applyFilterButton)

        // Заполняем Spinner категориями
        val uniqueCategories = ArrayList<String>().apply {
            add("Все категории") // Добавляем опцию "Все категории"
            addAll(getUniqueCategories()) // Добавляем уникальные категории
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, uniqueCategories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        applyFilterButton.setOnClickListener {
            // Получаем значения из полей ввода
            val minPriceText = minPriceEditText.text.toString()
            val maxPriceText = maxPriceEditText.text.toString()

            // Устанавливаем значения по умолчанию, если поля пустые
            val minPrice = if (minPriceText.isEmpty()) 0.0 else minPriceText.toDouble()
            val maxPrice = if (maxPriceText.isEmpty()) Double.MAX_VALUE else maxPriceText.toDouble()

            // Получаем выбранную категорию
            val selectedCategory = categorySpinner.selectedItem.toString()

            // Передаем выбранные значения обратно в GalleryFragment
            listener?.onFilterApplied(minPrice, maxPrice, selectedCategory)

            // Закрываем фрагмент
            dismiss()
        }

        return view
    }

    fun setCategories(categories: List<String>) {
        this.categories = categories
    }

    // Метод для получения уникальных категорий
    private fun getUniqueCategories(): List<String> {
        val uniqueCategories = ArrayList<String>()
        for (category in categories) {
            if (!uniqueCategories.contains(category)) {
                uniqueCategories.add(category)
            }
        }
        return uniqueCategories
    }
}