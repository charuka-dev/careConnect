package com.charukalakshan.careconnect

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SendRequestActivity : AppCompatActivity() {

    private lateinit var calendarView: android.widget.CalendarView
    private lateinit var tvHour: EditText
    private lateinit var tvMinute: EditText
    private lateinit var etNotes: EditText
    private lateinit var btnSendRequest: Button
    private lateinit var timeSelectorContainer: LinearLayout

    private var selectedDateMillis: Long = 0
    private var bookingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sendrequest)

        calendarView = findViewById(R.id.calendarView)
        tvHour = findViewById(R.id.tvHour)
        tvMinute = findViewById(R.id.tvMinute)
        etNotes = findViewById(R.id.etNotes)
        btnSendRequest = findViewById(R.id.btnSendRequest)
        timeSelectorContainer = findViewById(R.id.timeSelectorContainer)

        // wire back button
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        // bottom navigation: open bookings when calendar tapped
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.setOnItemSelectedListener { item ->
            val calendarId = bottomNav.menu.getItem(2).itemId
            val profileId = bottomNav.menu.getItem(4).itemId
            when (item.itemId) {
                calendarId -> {
                    startActivity(android.content.Intent(this, BookingsActivity::class.java))
                    true
                }
                profileId -> {
                    startActivity(android.content.Intent(this, UserMyProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // read intent extras
        bookingId = intent.getStringExtra("bookingId")
        val caregiverUid = intent.getStringExtra("caregiverUid")

        // If bookingId was passed, allow sending immediately; otherwise disable until created
        btnSendRequest.isEnabled = !bookingId.isNullOrBlank()

        // If bookingId wasn't passed, create the booking row now with userId and caregiverUid
        if (bookingId.isNullOrBlank()) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Toast.makeText(this, "Please log in to book", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            val bookingRef = FirebaseDatabase.getInstance().getReference("BookCaregiver").push()
            val newId = bookingRef.key
            val bookingData = mapOf(
                "userId" to currentUser.uid,
                "caregiverUserId" to caregiverUid,
                "status" to "pending",
                "payment" to "not_paid",
                "dateTime" to "",
                "note" to "",
                "createdAt" to System.currentTimeMillis()
            )

            // create the booking row and enable the send button when done
            bookingRef.setValue(bookingData).addOnSuccessListener {
                bookingId = newId
                btnSendRequest.isEnabled = true
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to create booking: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // default selected date
        selectedDateMillis = calendarView.date

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            selectedDateMillis = cal.timeInMillis
        }

        // open time picker when time container clicked
        timeSelectorContainer.setOnClickListener {
            val now = Calendar.getInstance()
            val tpd = TimePickerDialog(this, { _, hourOfDay, minute ->
                tvHour.setText(String.format(Locale.getDefault(), "%02d", hourOfDay))
                tvMinute.setText(String.format(Locale.getDefault(), "%02d", minute))
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true)
            tpd.show()
        }

        btnSendRequest.setOnClickListener {
            val idToUse = bookingId
            if (idToUse.isNullOrBlank()) {
                Toast.makeText(this, "Booking is not yet ready, please wait a moment.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val note = etNotes.text.toString().trim()
            val hourStr = tvHour.text.toString().trim()
            val minuteStr = tvMinute.text.toString().trim()

            if (hourStr.isEmpty() || minuteStr.isEmpty()) {
                Toast.makeText(this, "Enter time (hour and minute)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val hour = hourStr.toIntOrNull()
            val minute = minuteStr.toIntOrNull()

            if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59) {
                Toast.makeText(this, "Enter valid time (HH:MM)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // build dateTime string in ISO-like format
            val cal = Calendar.getInstance()
            cal.timeInMillis = selectedDateMillis
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val dateTimeStr = sdf.format(cal.time)

            // update booking record
            val updates = mapOf(
                "dateTime" to dateTimeStr,
                "note" to note,
                "status" to "pending"
            )

            FirebaseDatabase.getInstance().getReference("BookCaregiver").child(idToUse)
                .updateChildren(updates).addOnSuccessListener {
                    // On success, navigate to the Request Send confirmation screen
                    val intent = android.content.Intent(this, RequestSendActivity::class.java)
                    intent.putExtra("bookingId", idToUse)
                    startActivity(intent)
                    finish()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to send request: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
