package com.charukalakshan.careconnect

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase

class CaregiverRequestAdapter(private var items: MutableList<Booking>) : RecyclerView.Adapter<CaregiverRequestAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val ivPatient = view.findViewById<ImageView>(R.id.ivPatientProfile)
        val tvPatient = view.findViewById<TextView>(R.id.tvPatientName)
        val tvTime = view.findViewById<TextView>(R.id.tvTimeRange)
        val tvNote = view.findViewById<TextView>(R.id.tvRequestNote)
        val btnAccept = view.findViewById<Button>(R.id.btnAccept)
        val btnReject = view.findViewById<Button>(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_caregiver_request, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val b = items[position]
        holder.tvTime.text = b.dateTime ?: "-"
        holder.tvNote.text = b.note ?: ""

        // load patient (user) name & photo
        val uid = b.userId
        if (!uid.isNullOrEmpty()) {
            FirebaseDatabase.getInstance().getReference("User").child(uid)
                .get().addOnSuccessListener { snap ->
                    val name = snap.child("name").getValue(String::class.java) ?: "Patient"
                    val photo = snap.child("photoUrl").getValue(String::class.java)
                    holder.tvPatient.text = name
                    if (!photo.isNullOrEmpty()) Glide.with(holder.itemView).load(photo).circleCrop().into(holder.ivPatient)
                }
        }

        holder.btnAccept.setOnClickListener {
            // update BookCaregiver/{bookingId}/status = "accepted" and keep the item
            val id = b.bookingId
            if (!id.isNullOrEmpty()) {
                FirebaseDatabase.getInstance().getReference("BookCaregiver").child(id).child("status").setValue("accepted")
                    .addOnSuccessListener {
                        // clear the request message and reflect accepted state in UI
                        holder.tvNote.text = ""
                        // hide the reject button and disable the accept button
                        holder.btnReject.visibility = View.GONE
                        holder.btnAccept.isEnabled = false
                        holder.btnAccept.isClickable = false
                        holder.btnAccept.text = "Accepted"
                    }
            }
        }

        holder.btnReject.setOnClickListener {
            val id = b.bookingId
            if (!id.isNullOrEmpty()) {
                // mark rejected and remove from list
                val ref = FirebaseDatabase.getInstance().getReference("BookCaregiver").child(id)
                ref.child("status").setValue("rejected").addOnSuccessListener {
                    // find current adapter position and remove that item if it still matches
                    val currentPos = holder.bindingAdapterPosition
                    if (currentPos != RecyclerView.NO_POSITION && currentPos < items.size && items[currentPos].bookingId == id) {
                        items.removeAt(currentPos)
                        notifyItemRemoved(currentPos)
                        notifyItemRangeChanged(currentPos, items.size)
                    }
                }
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
