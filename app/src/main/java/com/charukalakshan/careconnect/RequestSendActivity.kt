package com.charukalakshan.careconnect

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class RequestSendActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_requestsend)

        val btnViewBookings = findViewById<Button>(R.id.btnViewBookings)
        val btnBackHome = findViewById<Button>(R.id.btnBackHome)

        val bookingId = intent.getStringExtra("bookingId")

        btnViewBookings.setOnClickListener {
            val intent = Intent(this, com.charukalakshan.careconnect.BookingsActivity::class.java)
            intent.putExtra("bookingId", bookingId)
            startActivity(intent)
            finish()
        }

        btnBackHome.setOnClickListener {
            val intent = Intent(this, com.charukalakshan.careconnect.ActivityUserHome::class.java)
            startActivity(intent)
            finish()
        }

        // bottom navigation in this screen: open Bookings when calendar tapped
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.setOnItemSelectedListener { item ->
            val calendarItemId = bottomNav.menu.getItem(2).itemId
            val profileItemId = bottomNav.menu.getItem(4).itemId
            when (item.itemId) {
                calendarItemId -> {
                    startActivity(android.content.Intent(this, com.charukalakshan.careconnect.BookingsActivity::class.java))
                    true
                }
                profileItemId -> {
                    startActivity(android.content.Intent(this, com.charukalakshan.careconnect.UserMyProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
