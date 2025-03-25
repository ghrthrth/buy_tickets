

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
    private val photoUrls: List<String>,
    private val titles: List<String>,
    private val descriptions: List<String>,
) : RecyclerView.Adapter<ImageAdapter.ViewHolder>(), Filterable {

    private var filteredPhotoUrls = ArrayList(photoUrls)
    private var filteredTitles = ArrayList(titles)
    private var filteredDescriptions = ArrayList(descriptions)

    private var listener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.grid_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val photoUrl = filteredPhotoUrls[position]
        val title = filteredTitles[position]
        val description = filteredDescriptions[position]

        Picasso.get().load(photoUrl).into(holder.imageView)
        holder.titleTextView.text = title
        holder.titleDescription.text = description


        holder.itemView.setOnClickListener {
            listener?.onItemClick(position) // Передаем позицию напрямую
        }
    }

    override fun getItemCount(): Int {
        return filteredTitles.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.service_image)
        val titleTextView: TextView = itemView.findViewById(R.id.service_title)
        val titleDescription: TextView = itemView.findViewById(R.id.service_description)
        // val descriptionTextView: TextView = itemView.findViewById(R.id.description_text_view)
    }



    override fun getFilter(): Filter {
        TODO("Not yet implemented")
    }
}