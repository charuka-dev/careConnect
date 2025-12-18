package com.charukalakshan.careconnect

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.bumptech.glide.Glide

class BookingAdapter(private var items: MutableList<Booking>) : RecyclerView.Adapter<BookingAdapter.VH>() {

    // simple in-memory cache for caregiver names and photos to reduce DB reads
    private val nameCache = mutableMapOf<String, String?>()
    private val photoCache = mutableMapOf<String, String?>()

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvCaregiver: TextView = view.findViewById(R.id.tvCaregiverName)
        val tvDate: TextView = view.findViewById(R.id.tvBookingDate)
        val ivStatusDot: ImageView = view.findViewById(R.id.ivStatusDot)
        val tvStatus: TextView = view.findViewById(R.id.tvBookingStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_booking, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val adapterPos = holder.bindingAdapterPosition
        if (adapterPos == RecyclerView.NO_POSITION) return
        val b = items[adapterPos]
        holder.tvDate.text = b.dateTime ?: "-"

        // status indicator
        if (b.status == "accepted") {
            holder.ivStatusDot.visibility = View.VISIBLE
            holder.ivStatusDot.setImageResource(R.drawable.green_dot)
            holder.tvStatus.text = holder.itemView.context.getString(R.string.accepted_label)
        } else {
            holder.ivStatusDot.visibility = View.GONE
            holder.tvStatus.text = holder.itemView.context.getString(R.string.pending_label)
        }

        // show caregiver name and photo (fetch from User node if needed)
        val cgUid = b.caregiverUserId
        if (cgUid.isNullOrEmpty()) {
            holder.tvCaregiver.text = holder.itemView.context.getString(R.string.unknown_label)
            Glide.with(holder.itemView).load(R.drawable.ic_profile).circleCrop().into(holder.itemView.findViewById(R.id.ivProfile))
        } else {
            val cachedName = nameCache[cgUid]
            val cachedPhoto = photoCache[cgUid]
            if (cachedName != null) {
                holder.tvCaregiver.text = cachedName
            } else {
                holder.tvCaregiver.text = holder.itemView.context.getString(R.string.loading_label)
            }

            if (cachedPhoto != null) {
                Glide.with(holder.itemView).load(cachedPhoto).placeholder(R.drawable.ic_profile).circleCrop().into(holder.itemView.findViewById(R.id.ivProfile))
            }

            if (cachedName == null || cachedPhoto == null) {
                val currentPos = holder.bindingAdapterPosition
                if (currentPos == RecyclerView.NO_POSITION) return
                FirebaseDatabase.getInstance().getReference("User").child(cgUid)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val name = snapshot.child("name").getValue(String::class.java) ?: cgUid
                            val photo = snapshot.child("photoUrl").getValue(String::class.java)
                            nameCache[cgUid] = name
                            photoCache[cgUid] = photo
                            val nowPos = holder.bindingAdapterPosition
                            if (nowPos != RecyclerView.NO_POSITION && nowPos < items.size && items[nowPos].caregiverUserId == cgUid) {
                                holder.tvCaregiver.text = name
                                val iv = holder.itemView.findViewById<ImageView>(R.id.ivProfile)
                                if (!photo.isNullOrEmpty()) {
                                    Glide.with(holder.itemView).load(photo).placeholder(R.drawable.ic_profile).circleCrop().into(iv)
                                } else {
                                    Glide.with(holder.itemView).load(R.drawable.ic_profile).circleCrop().into(iv)
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            nameCache[cgUid] = cgUid
                            photoCache[cgUid] = null
                            val nowPos = holder.bindingAdapterPosition
                            if (nowPos != RecyclerView.NO_POSITION && nowPos < items.size && items[nowPos].caregiverUserId == cgUid) {
                                holder.tvCaregiver.text = cgUid
                                Glide.with(holder.itemView).load(R.drawable.ic_profile).circleCrop().into(holder.itemView.findViewById(R.id.ivProfile))
                            }
                        }
                    })
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun update(list: List<Booking>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }
}
