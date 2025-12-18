package com.charukalakshan.careconnect

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BookingsActivity : AppCompatActivity() {
    private lateinit var rvBookings: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: BookingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookings)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        rvBookings = findViewById(R.id.rvBookings)
        tvEmpty = findViewById(R.id.tvEmptyBookings)

        rvBookings.layoutManager = LinearLayoutManager(this)
        adapter = BookingAdapter(mutableListOf())
        rvBookings.adapter = adapter

        // select the bookings/calendar icon
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        // calendar is the 3rd item in the menu (index 2) per bottom_nav_menu.xml
        val calendarIndex = 2
        val calendarMenuId = bottomNav?.menu?.getItem(calendarIndex)?.itemId
        calendarMenuId?.let { bottomNav.selectedItemId = it }

        // scale the selected icon to make it pop (use safe drawable scaling instead of internal classes)
        bottomNav?.let { nav ->
            calendarMenuId?.let { id -> scaleBottomNavIcons(nav, id, 1.2f) }
        }

        // navigation handling: open home, payment, messages, profile from bottom nav
        bottomNav?.setOnItemSelectedListener { item ->
            val menu = bottomNav.menu
            val homeId = menu.getItem(0).itemId
            val paymentId = menu.getItem(1).itemId
            val calendarId = menu.getItem(2).itemId
            val messagesId = menu.getItem(3).itemId
            val profileId = menu.getItem(4).itemId
            when (item.itemId) {
                homeId -> {
                    startActivity(android.content.Intent(this, ActivityUserHome::class.java))
                    true
                }
                paymentId -> {
                    startActivity(android.content.Intent(this, SettleMoneyActivity::class.java))
                    true
                }
                calendarId -> {
                    // already here
                    true
                }
                messagesId -> {
                    startActivity(android.content.Intent(this, UMessageActivity::class.java))
                    true
                }
                profileId -> {
                    startActivity(android.content.Intent(this, UserMyProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // load bookings for current user
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Please log in to see bookings"
            return
        }

        val db = FirebaseDatabase.getInstance().getReference("BookCaregiver")
        db.orderByChild("userId").equalTo(currentUser.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Booking>()
                    for (child in snapshot.children) {
                        val b = child.getValue(Booking::class.java)
                        if (b != null) {
                            b.bookingId = child.key
                            list.add(b)
                        }
                    }
                    adapter.update(list)
                    tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "Failed to load bookings: ${error.message}"
                }
            })
    }

    // Helper: safely scale BottomNavigationView menu item icons by rendering drawables into scaled bitmaps
    private fun scaleBottomNavIcons(bottomNav: com.google.android.material.bottomnavigation.BottomNavigationView, selectedItemId: Int, selectedScale: Float) {
        try {
            val menu = bottomNav.menu
            for (i in 0 until menu.size()) {
                val item = menu.getItem(i)
                val icon = item.icon ?: continue
                val scale = if (item.itemId == selectedItemId) selectedScale else 1.0f

                val w = if (icon.intrinsicWidth > 0) icon.intrinsicWidth else dpToPx(24)
                val h = if (icon.intrinsicHeight > 0) icon.intrinsicHeight else dpToPx(24)
                val newW = (w * scale).toInt().coerceAtLeast(1)
                val newH = (h * scale).toInt().coerceAtLeast(1)

                val bitmap = android.graphics.Bitmap.createBitmap(newW, newH, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                icon.setBounds(0, 0, newW, newH)
                icon.draw(canvas)

                val bd = android.graphics.drawable.BitmapDrawable(resources, bitmap)
                item.icon = bd
            }
        } catch (e: Exception) {
            // fallback: do nothing if scaling fails
            e.printStackTrace()
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
