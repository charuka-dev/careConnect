package com.charukalakshan.careconnect

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CaregiverHomeActivity : AppCompatActivity() {
    private lateinit var rvRequests: RecyclerView
    private lateinit var adapter: SimpleCaregiverRequestAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caregiverhome)

        rvRequests = findViewById<RecyclerView>(R.id.rvRequests)
        rvRequests.layoutManager = LinearLayoutManager(this)
        adapter = SimpleCaregiverRequestAdapter(mutableListOf())
        rvRequests.adapter = adapter

        // bottom nav behaviour: keep consistent
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                // TODO: wire caregiver-specific nav if needed
                else -> false
            }
        }

        // if logged in and this user is registered as a caregiver, load BookCaregiver requests where caregiverUserId == my uid
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            // not logged in - go to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // ensure this user is a caregiver by checking Caregiver/{uid}
        val cgRef = FirebaseDatabase.getInstance().getReference("Caregiver").child(user.uid)
        cgRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // not a caregiver - redirect to category/register flow
                    startActivity(Intent(this@CaregiverHomeActivity, Account1Activity::class.java))
                    finish()
                    return
                }

                // load requests for this caregiver
                loadRequestsForCaregiver(user.uid)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadRequestsForCaregiver(cgUid: String) {
        val db = FirebaseDatabase.getInstance().getReference("BookCaregiver")
        db.orderByChild("caregiverUserId").equalTo(cgUid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Booking>()
                    for (ch in snapshot.children) {
                        val b = ch.getValue(Booking::class.java)
                        if (b != null) {
                            b.bookingId = ch.key
                            list.add(b)
                        }
                    }
                    // refresh adapter with new list
                    runOnUiThread {
                        adapter = SimpleCaregiverRequestAdapter(list.toMutableList())
                        rvRequests.adapter = adapter
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}

// Simple local adapter to render caregiver requests. Kept minimal and self-contained.
private class SimpleCaregiverRequestAdapter(private var items: MutableList<Booking>) : androidx.recyclerview.widget.RecyclerView.Adapter<SimpleCaregiverRequestAdapter.VH>() {
    class VH(view: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val ivPatient = view.findViewById<android.widget.ImageView>(R.id.ivPatientProfile)
        val tvPatient = view.findViewById<android.widget.TextView>(R.id.tvPatientName)
        val tvTime = view.findViewById<android.widget.TextView>(R.id.tvTimeRange)
        val tvNote = view.findViewById<android.widget.TextView>(R.id.tvRequestNote)
        val btnAccept = view.findViewById<android.widget.Button>(R.id.btnAccept)
        val btnReject = view.findViewById<android.widget.Button>(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
        val v = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_caregiver_request, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val pos = holder.bindingAdapterPosition
        if (pos == androidx.recyclerview.widget.RecyclerView.NO_POSITION) return
        val b = items[pos]
        holder.tvTime.text = b.dateTime ?: "-"
        holder.tvNote.text = b.note ?: ""

        val uid = b.userId
        if (!uid.isNullOrEmpty()) {
            com.google.firebase.database.FirebaseDatabase.getInstance().getReference("User").child(uid)
                .get().addOnSuccessListener { snap ->
                    val name = snap.child("name").getValue(String::class.java) ?: "Patient"
                    val photo = snap.child("photoUrl").getValue(String::class.java)
                    holder.tvPatient.text = name
                    if (!photo.isNullOrEmpty()) com.bumptech.glide.Glide.with(holder.itemView).load(photo).circleCrop().into(holder.ivPatient)
                }
        }

        holder.btnAccept.setOnClickListener {
            val id = b.bookingId
            if (!id.isNullOrEmpty()) {
                com.google.firebase.database.FirebaseDatabase.getInstance().getReference("BookCaregiver").child(id).child("status").setValue("accepted")
                    .addOnSuccessListener {
                        holder.tvNote.text = ""
                        holder.btnReject.visibility = android.view.View.GONE
                        holder.btnAccept.isEnabled = false
                        holder.btnAccept.isClickable = false
                        holder.btnAccept.text = "Accepted"
                    }
            }
        }

        holder.btnReject.setOnClickListener {
            val id = b.bookingId
            if (!id.isNullOrEmpty()) {
                val ref = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("BookCaregiver").child(id)
                ref.child("status").setValue("rejected").addOnSuccessListener {
                    val currentPos = holder.bindingAdapterPosition
                    if (currentPos != androidx.recyclerview.widget.RecyclerView.NO_POSITION && currentPos < items.size && items[currentPos].bookingId == id) {
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
