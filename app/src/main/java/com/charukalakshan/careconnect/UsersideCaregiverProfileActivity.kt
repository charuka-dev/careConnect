package com.charukalakshan.careconnect

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class UsersideCaregiverProfileActivity : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvService: TextView
    private lateinit var tvExperience: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvRatingValue: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var progress1: ProgressBar
    private lateinit var progress2: ProgressBar
    private lateinit var progress3: ProgressBar
    private lateinit var progress4: ProgressBar
    private lateinit var progress5: ProgressBar
    private lateinit var tvHourlyRate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usersidecaregiverprofile)

        profileImage = findViewById(R.id.profileImage)
        tvName = findViewById(R.id.tvName)
        tvService = findViewById(R.id.tvService)
        tvExperience = findViewById(R.id.tvExperience)
        tvDescription = findViewById(R.id.tvDescription)
        tvRatingValue = findViewById(R.id.tvRatingValue)
        ratingBar = findViewById(R.id.ratingBarSmall)
        progress1 = findViewById(R.id.progress1)
        progress2 = findViewById(R.id.progress2)
        progress3 = findViewById(R.id.progress3)
        progress4 = findViewById(R.id.progress4)
        progress5 = findViewById(R.id.progress5)
        tvHourlyRate = findViewById(R.id.tvHourlyRate)

        // Back button (top-left) wiring
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        // Book caregiver button
        val btnBook = findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btnBookCaregiver)

        // read extras
        val uid = intent.getStringExtra("uid")
        val nameExtra = intent.getStringExtra("name")
        val ratingExtra = intent.getDoubleExtra("rating", 0.0)

        // populate quick fields if available
        tvName.text = nameExtra ?: "N/A"
        tvRatingValue.text = String.format(Locale.US, "%.1f", ratingExtra)
        ratingBar.rating = ratingExtra.toFloat()
        tvHourlyRate.text = getString(R.string.hourly_rate)

        // set default placeholders for other fields
        tvService.text = "-"
        tvExperience.text = "-"
        tvDescription.text = "-"

        // set progress bars based on rating (map 0..5 to 0..100)
        setRatingProgress(ratingExtra)

        // If we have uid, fetch caregiver full details from Caregiver node, and user name/email from User node if needed
        if (!uid.isNullOrBlank()) {
            val dbCare = FirebaseDatabase.getInstance().getReference("Caregiver").child(uid)
            dbCare.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val c = snapshot.getValue(Caregiver::class.java)
                    if (c != null) {
                        // populate UI
                        tvService.text = c.service ?: "-"
                        tvExperience.text = getString(R.string.experience_and_city, c.education ?: "-", c.city ?: "-")
                        // craft description using resources
                        tvDescription.text = getString(R.string.caregiver_description, c.name ?: "", c.service ?: "care")

                        val r = c.rating ?: 0.0
                        tvRatingValue.text = String.format(Locale.US, "%.1f", r)
                        ratingBar.rating = r.toFloat()
                        setRatingProgress(r)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

            // fetch user details for the name/email (in case not passed)
            val userRef = FirebaseDatabase.getInstance().getReference("User").child(uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val u = snapshot.getValue(User::class.java)
                    if (u != null) {
                        tvName.text = u.name ?: tvName.text
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }

        // Book button logic: open SendRequestActivity immediately and let it create the booking row on load
        btnBook.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Toast.makeText(this, "Please log in to book", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // open SendRequestActivity with caregiver uid; SendRequestActivity will create the DB row on load
            val intent = Intent(this, SendRequestActivity::class.java)
            intent.putExtra("caregiverUid", uid)
            startActivity(intent)
        }

        // bottom navigation: navigate to bookings when calendar icon tapped
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.setOnItemSelectedListener { item ->
            val calendarId = bottomNav.menu.getItem(2).itemId
            val profileId = bottomNav.menu.getItem(4).itemId
            when (item.itemId) {
                calendarId -> {
                    startActivity(Intent(this, BookingsActivity::class.java))
                    true
                }
                profileId -> {
                    startActivity(Intent(this, UserMyProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setRatingProgress(rating: Double) {
        // rating is 0..5, we map each progress bar to percentages for 5->100, 4->80 etc.
        val p1 = ((rating / 5.0) * 100).toInt()
        val p2 = (((rating - 1).coerceAtLeast(0.0) / 4.0) * 100).toInt()
        val p3 = (((rating - 2).coerceAtLeast(0.0) / 3.0) * 100).toInt()
        val p4 = (((rating - 3).coerceAtLeast(0.0) / 2.0) * 100).toInt()
        val p5 = (((rating - 4).coerceAtLeast(0.0) / 1.0) * 100).toInt()

        progress1.progress = p1.coerceIn(0, 100)
        progress2.progress = p2.coerceIn(0, 100)
        progress3.progress = p3.coerceIn(0, 100)
        progress4.progress = p4.coerceIn(0, 100)
        progress5.progress = p5.coerceIn(0, 100)
    }
}
