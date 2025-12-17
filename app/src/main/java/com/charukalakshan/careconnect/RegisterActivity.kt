package com.charukalakshan.careconnect   // Package name of the application

// Required imports for Android UI, Firebase, and text styling
import android.content.Intent               // Used to switch between activities
import android.graphics.Color               // Used to set text color
import android.os.Bundle                    // Used to pass data to activity
import android.text.SpannableString         // Allows text styling
import android.text.Spanned                 // Interface for styled text
import android.text.method.LinkMovementMethod // Enables clickable text
import android.text.style.ClickableSpan     // Makes part of text clickable
import android.text.style.ForegroundColorSpan // Changes text color
import android.text.style.UnderlineSpan     // Underlines text
import android.view.View                    // Base class for UI components
import android.widget.EditText              // Input field
import android.widget.TextView              // Displays text
import android.widget.Toast                 // Shows short popup messages
import androidx.appcompat.app.AppCompatActivity // Base activity class
import com.google.android.material.button.MaterialButton // Material Design button
import com.google.firebase.auth.FirebaseAuth // Firebase Authentication
import com.google.firebase.database.FirebaseDatabase // Firebase Realtime Database

// RegisterActivity handles new user registration
class RegisterActivity : AppCompatActivity() {

    // Firebase Authentication instance
    private val auth = FirebaseAuth.getInstance()

    // Called when the activity is first created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Connect this activity with activity_register.xml layout
        setContentView(R.layout.activity_register)

        // Get references to input fields and UI components
        val name = findViewById<EditText>(R.id.editTextName) // Name input
        val email = findViewById<EditText>(R.id.editTextTextEmailAddress) // Email input
        val password = findViewById<EditText>(R.id.editTextTextEmailAddress2) // Password input
        val confirmPassword = findViewById<EditText>(R.id.editTextTextEmailAddress4) // Confirm password
        val buttonRegister = findViewById<MaterialButton>(R.id.buttonr) // Register button
        val textView = findViewById<TextView>(R.id.textView6) // Login text view

        // Make "Login" text clickable
        setupLoginLink(textView)

        // Triggered when Register button is clicked
        buttonRegister.setOnClickListener {

            // Get user input and remove unnecessary spaces
            val name2 = name.text.toString().trim()
            val email2 = email.text.toString().trim()
            val password2 = password.text.toString().trim()
            val confirm2 = confirmPassword.text.toString().trim()

            // Validate user input before registration
            when {
                name2.isEmpty() -> showToast("Please enter the name")
                email2.isEmpty() -> showToast("Please enter the email")
                password2.isEmpty() -> showToast("Please enter the password")
                password2.length < 6 -> showToast("Password must be at least 6 characters")
                confirm2.isEmpty() -> showToast("Please confirm your password")
                password2 != confirm2 -> showToast("Passwords do not match")
                else -> registerUser(name2, email2, password2) // All inputs valid
            }
        }
    }

    // Registers user using Firebase Authentication
    private fun registerUser(name: String, email: String, password: String) {

        // Create a new user with email and password
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                // Check if registration was successful
                if (task.isSuccessful) {

                    // Get the unique Firebase user ID
                    val userId = auth.currentUser!!.uid

                    // Generate a random 5-digit OTP
                    val otp = (10000..99999).random().toString()

                    // Create User object to store in database
                    val user = User(
                        userId = userId,
                        name = name,
                        email = email
                    )

                    // Get reference to Firebase Realtime Database
                    val database = FirebaseDatabase.getInstance().reference

                    // Save user details under "User" node
                    database.child("User").child(userId).setValue(user)
                        .addOnSuccessListener {

                            // Save OTP under "OTP" node
                            database.child("OTP").child(userId).setValue(otp)
                                .addOnSuccessListener {

                                    // Display OTP (testing purpose only)
                                    showToast("OTP sent to your email: $otp")

                                    // Navigate to OTP verification screen
                                    val intent = Intent(this, VerifyActivity::class.java)
                                    intent.putExtra("userId", userId)
                                    startActivity(intent)

                                    // Close RegisterActivity
                                    finish()
                                }
                                .addOnFailureListener {
                                    showToast("Failed to save OTP")
                                }
                        }
                        .addOnFailureListener {
                            showToast("Failed to save user data")
                        }

                } else {
                    // Show error message if registration fails
                    showToast("Registration failed: ${task.exception?.message}")
                }
            }
    }

    // Makes "Login" text clickable and styled
    private fun setupLoginLink(textView: TextView) {

        // Text displayed in TextView
        val text = "Already registered? Login"

        // Convert text into SpannableString for styling
        val spannable = SpannableString(text)

        // Define click action for "Login"
        val loginClickable = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Navigate to LoginActivity
                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
            }
        }

        // Find start and end index of "Login"
        val startIndex = text.indexOf("Login")
        val endIndex = startIndex + "Login".length

        // Apply clickable, color, and underline effects
        spannable.setSpan(loginClickable, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#2E8791")),
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            UnderlineSpan(),
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Set styled text to TextView
        textView.text = spannable

        // Enable clicking on styled text
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    // Helper function to show short messages on screen
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
