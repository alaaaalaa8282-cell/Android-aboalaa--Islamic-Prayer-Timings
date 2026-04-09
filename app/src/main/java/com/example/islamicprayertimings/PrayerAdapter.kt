package com.example.islamicprayertimings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

data class PrayerItem(
    val name: String,
    val time: String,
    val iconResId: Int
)

class PrayerAdapter(
    private val prayers: List<PrayerItem>
) : RecyclerView.Adapter<PrayerAdapter.PrayerViewHolder>() {

    class PrayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.prayerCard)
        val prayerName: TextView = itemView.findViewById(R.id.prayerName)
        val prayerTime: TextView = itemView.findViewById(R.id.prayerTime)
        val prayerIcon: ImageView = itemView.findViewById(R.id.prayerIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrayerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_prayer, parent, false)
        return PrayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PrayerViewHolder, position: Int) {
        val prayer = prayers[position]
        holder.prayerName.text = prayer.name
        holder.prayerTime.text = prayer.time
        holder.prayerIcon.setImageResource(prayer.iconResId)
    }

    override fun getItemCount() = prayers.size
}
