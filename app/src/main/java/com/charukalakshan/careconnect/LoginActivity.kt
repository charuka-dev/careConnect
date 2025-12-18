// Package name of the application
package com.charukalakshan.careconnect

// Suppresses lint warnings (used here for layout ID issues)
import android.annotation.SuppressLint

// Used to move from one activity to another
import android.content.Intent

// Used to set text color
import android.graphics.Color

// Used to pass saved state to activity
import android.os.Bundle

// Allows text with multiple styles (clickable, colored, underlined)
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan

// Base class for UI elements
import android.view.View

// UI components
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

// Base class for activities
import androidx.appcompat.app.AppCompatActivity

// Firebase Authentication
import com.google.firebase.auth.FirebaseAuth

// Firebase Realtime Database
import com.google.firebase.database.*

// LoginActivity class
class LoginActivity : AppCompatActivity() {

    // Reference to Firebase Realtime Database
    private lateinit var database: DatabaseReference

    // Firebase Authentication object
    private lateinit var auth: FirebaseAuth

    // Called when activity is created
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Connects activity to XML layout
        setContentView(R.layout.activity_login)

        // Initialize Firebase database reference ("Register" node)
        database = FirebaseDatabase.getInstance().getReference("User")

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // ---------------- Clickable "Sign Up" Text ----------------

        // Get TextView reference
        val textView = findViewById<TextView>(R.id.textView6)

        // Text to display
        val text = "Don’t have an account? Sign Up"

        // Make text spannable (styled)
        val spannable = SpannableString(text)

        // Define clickable action for "Sign Up"
        val signUpClickable = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Navigate to RegisterActivity
                startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
            }
        }

        // Find start and end position of "Sign Up"
        val startIndex = text.indexOf("Sign Up")
        val endIndex = startIndex + "Sign Up".length

        // Apply clickable span
        spannable.setSpan(
            signUpClickable,
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Apply color to "Sign Up"
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#2E8791")),
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Underline "Sign Up"
        spannable.setSpan(
            UnderlineSpan(),
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Set styled text to TextView
        textView.text = spannable

        // Enable click handling
        textView.movementMethod = LinkMovementMethod.getInstance()

        // ---------------- Login Fields ----------------

        // Email input field
        val email = findViewById<EditText>(R.id.editTextTextEmailAddress)

        // Password input field
        val password = findViewById<EditText>(R.id.editTextPassword)

        // Login button
        val btnlogin = findViewById<Button>(R.id.button3)

        // Handle login button click
        btnlogin.setOnClickListener {

            // Get user input and remove extra spaces
            val email2 = email.text.toString().trim()
            val password2 = password.text.toString().trim()

            // Validate input fields
            if (email2.isEmpty() || password2.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please enter email and password",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Attempt login
                loginUser(email2, password2)
            }
        }
    }

    // Function to authenticate user with Firebase
    private fun loginUser(email2: String, password2: String) {

        // Firebase email & password login
        auth.signInWithEmailAndPassword(email2, password2)
            .addOnCompleteListener(this) { task ->

                // If login successful
                if (task.isSuccessful) {

                    // Get logged-in user's ID
                    val userId = auth.currentUser?.uid

                    // Check if user exists in database
                    if (userId != null) {
                        // Determine role and route accordingly.
                        val userRef = FirebaseDatabase.getInstance().getReference("User").child(userId)
                        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshotUser: DataSnapshot) {
                                // read role flexibly (could be stored as Long, Int, or String)
                                val roleNode = snapshotUser.child("role").value
                                val roleVal = when (roleNode) {
                                    is Long -> roleNode.toInt()
                                    is Int -> roleNode
                                    is String -> roleNode.toIntOrNull()
                                    else -> null
                                }

                                if (roleVal != null) {
                                    // role explicitly set: 1 => customer, 0 => caregiver
                                    if (roleVal == 1) {
                                        startActivity(Intent(this@LoginActivity, ActivityUserHome::class.java))
                                        finish()
                                    } else {
                                        startActivity(Intent(this@LoginActivity, CaregiverHomeActivity::class.java))
                                        finish()
                                    }
                                } else {
                                    // role not set; check Caregiver node first, then customers
                                    checkCaregiverThenCustomers(userId, userRef)
                                }
                             }

                             override fun onCancelled(error: DatabaseError) {
                                 // fallback
                                 checkUserInDatabase(userId)
                             }
                         })
                     }

                } else {
                    // Login failed
                    Toast.makeText(
                        this,
                        "Invalid email or password",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    // helper: check caregiver node, then customers node, write role back to User/{uid}/role
    private fun checkCaregiverThenCustomers(userId: String, userRef: DatabaseReference) {
        val caregiverRef = FirebaseDatabase.getInstance().getReference("Caregiver").child(userId)
        caregiverRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(cgSnap: DataSnapshot) {
                if (cgSnap.exists()) {
                    // user is a caregiver
                    userRef.child("role").setValue(0)
                    startActivity(Intent(this@LoginActivity, CaregiverHomeActivity::class.java))
                    finish()
                } else {
                    // not found under caregiver; check customers node
                    val customersRef = FirebaseDatabase.getInstance().getReference("customers")
                    customersRef.orderByChild("userId").equalTo(userId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    // user is a customer
                                    userRef.child("role").setValue(1)
                                    startActivity(Intent(this@LoginActivity, ActivityUserHome::class.java))
                                    finish()
                                } else {
                                    // fallback: if there's a User entry, route to user home; else sign out
                                    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(uSnap: DataSnapshot) {
                                            if (uSnap.exists()) {
                                                userRef.child("role").setValue(1)
                                                startActivity(Intent(this@LoginActivity, ActivityUserHome::class.java))
                                                finish()
                                            } else {
                                                Toast.makeText(this@LoginActivity, "User record not found", Toast.LENGTH_SHORT).show()
                                                FirebaseAuth.getInstance().signOut()
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Toast.makeText(this@LoginActivity, "DB error", Toast.LENGTH_SHORT).show()
                                        }
                                    })
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // fallback
                                startActivity(Intent(this@LoginActivity, ActivityUserHome::class.java))
                                finish()
                            }
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // fallback
                startActivity(Intent(this@LoginActivity, ActivityUserHome::class.java))
                finish()
            }
        })
    }

    // Function to verify user data exists in Realtime Database
    private fun checkUserInDatabase(userId: String) {

        // Read user data once
        database.child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {

                // Called when data is read
                override fun onDataChange(snapshot: DataSnapshot) {

                    // If user exists in database
                    if (snapshot.exists()) {

                        Toast.makeText(
                            this@LoginActivity,
                            "Login Successful!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Navigate to Home Activity
                        startActivity(
                            Intent(this@LoginActivity, ActivityUserHome::class.java)
                        )

                        // Close LoginActivity
                        finish()

                    } else {
                        // User not found in database
                        Toast.makeText(
                            this@LoginActivity,
                            "User not found in database",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Logout user
                        auth.signOut()
                    }
                }

                // Called if database read fails
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Database Error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
