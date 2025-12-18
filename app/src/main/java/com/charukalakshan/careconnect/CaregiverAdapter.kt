package com.charukalakshan.careconnect

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CaregiverAdapter(private val items: MutableList<Caregiver>) : RecyclerView.Adapter<CaregiverAdapter.VH>() {

    // Keep a copy of all items for filtering
    private val allItems = mutableListOf<Caregiver>().apply { addAll(items) }
    private val filtered = mutableListOf<Caregiver>().apply { addAll(items) }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProfile: ImageView = itemView.findViewById(R.id.ivProfile)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
        val btnView: Button = itemView.findViewById(R.id.btnView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_itemuserhomeitem, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = filtered[position]

        // Title: the caregiver's name (from User node) or service fallback
        holder.tvTitle.text = c.name ?: c.service ?: "Caregiver"

        // Subtitle: show email when available, otherwise education and city
        holder.tvSubtitle.text = when {
            !c.email.isNullOrBlank() -> c.email
            else -> {
                val parts = listOfNotNull(c.education, c.city).filter { it.isNotBlank() }
                if (parts.isNotEmpty()) parts.joinToString(" • ") else "No details"
            }
        }

        // Profile image: keep default drawable for now
        holder.ivProfile.setImageResource(R.drawable.ic_profile)

        holder.btnView.setOnClickListener {
            val ctx = holder.itemView.context
            val intent = Intent(ctx, UsersideCaregiverProfileActivity::class.java)
            intent.putExtra("uid", c.uid)
            intent.putExtra("name", c.name)
            intent.putExtra("email", c.email)
            intent.putExtra("rating", c.rating ?: 0.0)
            ctx.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = filtered.size

    fun update(newItems: List<Caregiver>) {
        // Update both lists
        allItems.clear()
        allItems.addAll(newItems)
        filtered.clear()
        filtered.addAll(newItems)
        // update the public items list reference as well for backwards compatibility
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    /**
     * Filter caregivers by query matching service, city, education, or nic (case-insensitive)
     */
    fun filter(query: String) {
        val q = query.trim().lowercase()
        filtered.clear()
        if (q.isEmpty()) {
            filtered.addAll(allItems)
        } else {
            for (c in allItems) {
                val fields = listOfNotNull(c.name, c.service, c.city, c.education, c.nic, c.phone)
                val hay = fields.joinToString(" ").lowercase()
                if (hay.contains(q)) filtered.add(c)
            }
        }
        notifyDataSetChanged()
    }
}
