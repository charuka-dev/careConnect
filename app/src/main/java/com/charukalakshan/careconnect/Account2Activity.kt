package com.charukalakshan.careconnect   // Package name of the app

// Required Android imports
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast

// AndroidX & Material components
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

// Firebase imports
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

// Account2Activity class
class Account2Activity : AppCompatActivity() {

    // Firebase database reference
    private lateinit var database: DatabaseReference

    // Firebase authentication reference
    private lateinit var auth: FirebaseAuth

    // Called when activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Attach XML layout
        setContentView(R.layout.activity_account2)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Initialize Firebase Realtime Database reference ("Caregiver" node)
        database = FirebaseDatabase.getInstance().getReference("Caregiver")

        // ---------------- Get Data from Previous Activity ----------------

        // Retrieve data passed from Account1Activity
        val phone = intent.getStringExtra("phone")
        val dob = intent.getStringExtra("dob")
        val nic = intent.getStringExtra("nic")
        val city = intent.getStringExtra("city")
        val gender = intent.getStringExtra("gender")

        // ---------------- UI Elements ----------------

        // Education / experience input
        val educationEditText = findViewById<EditText>(R.id.editTextTextEmailAddress7)

        // Service type radio buttons
        val personal = findViewById<RadioButton>(R.id.radioButton)
        val housekeeping = findViewById<RadioButton>(R.id.radioButton4)
        val childCare = findViewById<RadioButton>(R.id.radioButton5)
        val elderly = findViewById<RadioButton>(R.id.radioButton6)

        // Complete setup button
        val completeSetupBtn = findViewById<MaterialButton>(R.id.button3)

        // ---------------- Button Click Logic ----------------

        completeSetupBtn.setOnClickListener {

            // Get education value
            val educationValue = educationEditText.text.toString().trim()

            // Validate education field
            if (educationValue.isEmpty()) {
                educationEditText.error = "Enter your education / experience"
                return@setOnClickListener
            }

            // Determine selected caregiver service
            val serviceValue = when {
                personal.isChecked -> "Personal Caregiver"
                housekeeping.isChecked -> "Housekeeping Caregiver"
                childCare.isChecked -> "Child Caregiver"
                elderly.isChecked -> "Elderly Caregiver"
                else -> ""
            }

            // Validate service selection
            if (serviceValue.isEmpty()) {
                Toast.makeText(this, "Select a service", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get currently logged-in user's UID
            val uid = auth.currentUser?.uid

            // Check if user is logged in
            if (uid == null) {
                Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ---------------- Save Data to Firebase ----------------

            // Create caregiver data map
            val caregiverData = mapOf(
                "phone" to phone,
                "dob" to dob,
                "nic" to nic,
                "city" to city,
                "gender" to gender,
                "education" to educationValue,
                "service" to serviceValue
            )

            // Save caregiver data using UID as key
            database.child(uid)
                .setValue(caregiverData)
                .addOnSuccessListener {

                    // Show success message
                    Toast.makeText(
                        this,
                        "✅ Setup Completed!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Navigate to Home screen
                    startActivity(Intent(this, ActivityHome::class.java))

                    // Close Account2Activity
                    finish()
                }
                .addOnFailureListener { e ->

                    // Show error message
                    Toast.makeText(
                        this,
                        "❌ Failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
}
