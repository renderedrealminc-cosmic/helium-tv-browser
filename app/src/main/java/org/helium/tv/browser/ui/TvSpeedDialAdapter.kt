package org.helium.tv.browser.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.helium.tv.browser.R

class TvSpeedDialAdapter(
    private val items: List<SpeedDialItem>,
    private val onItemClick: (SpeedDialItem) -> Unit
) : RecyclerView.Adapter<TvSpeedDialAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconImage: ImageView = view.findViewById(R.id.ivBookmarkIcon)
        val letterText: TextView = view.findViewById(R.id.tvBookmarkLetter)
        val pinImage: ImageView = view.findViewById(R.id.ivPin)
        val titleText: TextView = view.findViewById(R.id.tvBookmarkTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_speed_dial, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        if (item.iconResId != null) {
            holder.iconImage.visibility = View.VISIBLE
            holder.letterText.visibility = View.GONE
            holder.iconImage.setImageResource(item.iconResId)
        } else {
            holder.iconImage.visibility = View.GONE
            holder.letterText.visibility = View.VISIBLE
            holder.letterText.setBackgroundResource(R.drawable.bg_circle_charcoal)
            holder.letterText.text = item.letter ?: "•"
        }

        holder.pinImage.visibility = if (item.isPinned) View.VISIBLE else View.GONE
        holder.titleText.text = item.displayUrl

        // Smooth TV Remote D-Pad focus zoom & highlight
        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate().scaleX(1.12f).scaleY(1.12f).translationZ(8f).setDuration(150).start()
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).translationZ(0f).setDuration(150).start()
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
