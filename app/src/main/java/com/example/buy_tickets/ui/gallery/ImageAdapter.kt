package com.example.buy_tickets.ui.gallery

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.buy_tickets.R
import com.squareup.picasso.Picasso

class ImageAdapter(
    private val context: Context,
    photoUrls: List<String>,
    titles: List<String>,
    descriptions: List<String>,
    latitudes: List<Double>,
    longitudes: List<Double>
) : RecyclerView.Adapter<ImageAdapter.ViewHolder>(), Filterable {

    private var filteredPhotoUrls = photoUrls.toMutableList()
    private var filteredTitles = titles.toMutableList()
    private var filteredDescriptions = descriptions.toMutableList()
    private var filteredLatitudes = latitudes.toMutableList()
    private var filteredLongitudes = longitudes.toMutableList()

    private var listener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    // Метод для удаления элемента
    fun removeItem(position: Int) {
        filteredPhotoUrls.removeAt(position)
        filteredTitles.removeAt(position)
        filteredDescriptions.removeAt(position)
        filteredLatitudes.removeAt(position)
        filteredLongitudes.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, filteredTitles.size)
    }

    fun updateAllData(
        newPhotoUrls: List<String>,
        newTitles: List<String>,
        newDescriptions: List<String>,
        newLatitudes: List<Double>,
        newLongitudes: List<Double>
    ) {
        filteredPhotoUrls.clear()
        filteredTitles.clear()
        filteredDescriptions.clear()
        filteredLatitudes.clear()
        filteredLongitudes.clear()

        filteredPhotoUrls.addAll(newPhotoUrls)
        filteredTitles.addAll(newTitles)
        filteredDescriptions.addAll(newDescriptions)
        filteredLatitudes.addAll(newLatitudes)
        filteredLongitudes.addAll(newLongitudes)

        notifyDataSetChanged()
        Log.d("ImageAdapter", "Данные адаптера обновлены")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.grid_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Picasso.get().load(filteredPhotoUrls[position]).into(holder.imageView)
        holder.titleTextView.text = filteredTitles[position]
        holder.titleDescription.text = filteredDescriptions[position]

        holder.itemView.setOnClickListener {
            listener?.onItemClick(position)
        }
    }

    override fun getItemCount(): Int = filteredTitles.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.service_image)
        val titleTextView: TextView = itemView.findViewById(R.id.service_title)
        val titleDescription: TextView = itemView.findViewById(R.id.service_description)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                // Implement your filtering logic here
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                // Update filtered lists and notify adapter
            }
        }
    }
}