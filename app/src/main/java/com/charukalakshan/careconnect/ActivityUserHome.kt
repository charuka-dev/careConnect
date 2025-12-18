package com.charukalakshan.careconnect

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ActivityUserHome : AppCompatActivity() {

    private lateinit var rvCaregivers: RecyclerView
    private lateinit var adapter: CaregiverAdapter
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_userhome)

        rvCaregivers = findViewById(R.id.rvCaregivers)
        tvEmpty = findViewById(R.id.tvEmpty)

        rvCaregivers.layoutManager = LinearLayoutManager(this)
        adapter = CaregiverAdapter(mutableListOf())
        rvCaregivers.adapter = adapter

        // select home icon and scale it
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
        bottomNav?.selectedItemId = R.id.home
        bottomNav?.let { nav ->
            scaleBottomNavIcons(nav, R.id.home, 1.2f)
        }

        // navigation behavior: open BookingsActivity when calendar icon tapped
        bottomNav?.setOnItemSelectedListener { item ->
            val calendarItemId = bottomNav.menu.getItem(2).itemId
            val profileItemId = bottomNav.menu.getItem(4).itemId
            when (item.itemId) {
                calendarItemId -> {
                    startActivity(android.content.Intent(this, BookingsActivity::class.java))
                    true
                }
                profileItemId -> {
                    startActivity(android.content.Intent(this, UserMyProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Read caregivers from Firebase Realtime Database
        val dbRef = FirebaseDatabase.getInstance().getReference("Caregiver")
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Caregiver>()
                for (child in snapshot.children) {
                    val c = child.getValue(Caregiver::class.java)
                    if (c != null) {
                        c.uid = child.key
                        list.add(c)
                    }
                }
                adapter.update(list)
                tvEmpty.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ActivityUserHome, "Failed to load caregivers: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

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
            e.printStackTrace()
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
