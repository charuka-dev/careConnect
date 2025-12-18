package com.charukalakshan.careconnect   // Package name

// Required Android & Kotlin imports
import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.*

// AndroidX & Material components
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar

// Firebase Realtime Database
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth

// Used to convert Bitmap to Base64
import java.io.ByteArrayOutputStream

// Account3Activity class
class Account3Activity : AppCompatActivity() {

    // UI elements
    private lateinit var button: Button              // Camera button
    private lateinit var radioMale: RadioButton      // Male radio button
    private lateinit var radioFemale: RadioButton    // Female radio button
    private lateinit var editPhone: EditText         // Phone input
    private lateinit var editDOB: EditText           // Date of birth input
    private lateinit var editCity: EditText          // City input
    private lateinit var buttonNext: MaterialButton  // Submit button
    private lateinit var imageView: ImageView        // Profile image view

    // Stores captured image
    private var bitmapImage: Bitmap? = null

    // Request codes
    private val CAMERA_REQUEST_CODE = 100
    private val PERMISSION_CODE = 101

    // Root view for Snackbar
    private lateinit var rootView: View

    // Called when activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Attach layout to activity
        setContentView(R.layout.activity_account3)

        // Root view for Snackbar messages
        rootView = findViewById(android.R.id.content)

        // Initialize UI elements
        button = findViewById(R.id.button7)
        imageView = findViewById(R.id.imageView4)
        editPhone = findViewById(R.id.editTextPhone)
        editDOB = findViewById(R.id.editTextPhone4)
        editCity = findViewById(R.id.editTextPhone5)
        radioMale = findViewById(R.id.radioButtonm)
        radioFemale = findViewById(R.id.radioButtonf)
        buttonNext = findViewById(R.id.button4)

        // Ensure ImageView dimensions are ready
        imageView.post { }

        // Camera button click
        button.setOnClickListener {
            if (checkPermissions()) {
                openCamera()
            } else {
                requestPermission()
            }
        }

        // Submit button click
        buttonNext.setOnClickListener {
            saveCustomerToFirebase()
        }
    }

    // ---------------- PERMISSIONS ----------------

    // Check if camera & storage permissions are granted
    private fun checkPermissions(): Boolean {
        val cameraPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)

        val storagePermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        // Storage permission not required for Android 10+
        return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                        storagePermission == PackageManager.PERMISSION_GRANTED)
    }

    // Request required permissions
    private fun requestPermission() {
        val permissionList = mutableListOf(Manifest.permission.CAMERA)

        // Storage permission only needed for Android < 10
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        ActivityCompat.requestPermissions(
            this,
            permissionList.toTypedArray(),
            PERMISSION_CODE
        )
    }

    // ---------------- CAMERA ----------------

    // Open device camera
    private fun openCamera() {
        startActivityForResult(
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
            CAMERA_REQUEST_CODE
        )
    }

    // Receive camera result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Check camera response
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            // Get captured image as Bitmap
            val bitmap = data?.extras?.get("data") as? Bitmap

            bitmap?.let {

                // 1️⃣ Crop image into a circle
                val circleBitmap = cropToCircle(it)

                // 2️⃣ Scale image to fit ImageView
                val finalBitmap = Bitmap.createScaledBitmap(
                    circleBitmap,
                    imageView.width,
                    imageView.height,
                    true
                )

                // Display image
                bitmapImage = finalBitmap
                imageView.setImageBitmap(finalBitmap)

                // Save image to device gallery
                saveImageToGallery(finalBitmap)
            }
        }
    }

    // ---------------- CIRCLE CROP ----------------

    // Converts square image into circular image
    private fun cropToCircle(bitmap: Bitmap): Bitmap {

        // Find smallest side
        val size = minOf(bitmap.width, bitmap.height)

        // Center crop
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2

        // Create square bitmap
        val squaredBitmap = Bitmap.createBitmap(bitmap, x, y, size, size)

        // Output bitmap
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Paint with shader
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(
                squaredBitmap,
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP
            )
        }

        // Draw circular image
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        return output
    }

    // ---------------- SAVE IMAGE ----------------

    // Save image to gallery
    private fun saveImageToGallery(bitmap: Bitmap) {

        // Unique image name
        val filename = "IMG_${System.currentTimeMillis()}.jpg"

        // Image metadata
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

            // Save inside Pictures folder (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraDemo")
            }
        }

        // Insert image into MediaStore
        val uri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        // Write bitmap to storage
        uri?.let {
            contentResolver.openOutputStream(it)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)

                // Show success message
                Snackbar.make(
                    rootView,
                    "Image Saved to Gallery",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ---------------- FIREBASE ----------------

    // Convert Bitmap image to Base64 String
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }

    // Save customer data to Firebase Realtime Database
    private fun saveCustomerToFirebase() {

        // Get user input
        val phone = editPhone.text.toString().trim()
        val dob = editDOB.text.toString().trim()
        val city = editCity.text.toString().trim()

        // Determine selected gender
        val gender = when {
            radioMale.isChecked -> "Male"
            radioFemale.isChecked -> "Female"
            else -> ""
        }

        // Validate inputs
        if (phone.isEmpty() || dob.isEmpty() || city.isEmpty() || gender.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Firebase database reference
        val databaseRef = FirebaseDatabase.getInstance().getReference("customers")

        // Generate unique ID
        val customerId = databaseRef.push().key ?: return

        // Convert image to Base64
        val imageBase64 = bitmapImage?.let { bitmapToBase64(it) } ?: ""

        // Get current user
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Create customer object
        val customer = CustomerFirebase(
            phone = phone,
            dob = dob,
            city = city,
            gender = gender,
            imageBase64 = imageBase64,
            userId = currentUser?.uid ?: "",
            email = currentUser?.email ?: ""
        )

        // Save data to Firebase
        databaseRef.child(customerId)
            .setValue(customer)
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Customer saved successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                // Set role flag on the User node: 1 => customer
                val uid = currentUser?.uid
                if (!uid.isNullOrEmpty()) {
                    FirebaseDatabase.getInstance().getReference("User").child(uid).child("role").setValue(1)
                }

                // Go to Home screen and clear back stack
                startActivity(
                    Intent(this, ActivityUserHome::class.java).apply {
                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show()
            }
    }

    // Firebase data model
    data class CustomerFirebase(
        val phone: String = "",
        val dob: String = "",
        val city: String = "",
        val gender: String = "",
        val imageBase64: String = "",
        val userId: String = "",
        val email: String = ""
    )
}
