package org.helium.tv.browser.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.helium.tv.browser.R

class TvSpeedDialAdapter(
    private val items: List<SpeedDialItem>,
    private val onItemClick: (SpeedDialItem) -> Unit
) : RecyclerView.Adapter<TvSpeedDialAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emojiText: TextView = view.findViewById(R.id.tvBookmarkEmoji)
        val titleText: TextView = view.findViewById(R.id.tvBookmarkTitle)
        val categoryText: TextView = view.findViewById(R.id.tvBookmarkCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_speed_dial, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.emojiText.text = item.emoji
        holder.titleText.text = item.title
        holder.categoryText.text = item.category

        // Apple TV / Android TV Leanback focus animation
        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate().scaleX(1.15f).scaleY(1.15f).translationZ(12f).setDuration(160).start()
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).translationZ(0f).setDuration(160).start()
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
