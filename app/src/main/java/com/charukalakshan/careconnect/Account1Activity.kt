package com.charukalakshan.careconnect   // Package name

// Required Android permissions and components
import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*

// AndroidX & Material components
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar

// Account1Activity class
class Account1Activity : AppCompatActivity() {

    // UI components
    private lateinit var button: Button          // Camera button
    private lateinit var imageView: ImageView    // Profile image view

    // Bitmap to store captured image
    private var bitmapImage: Bitmap? = null

    // Request codes
    private val CAMERA_REQUEST_CODE = 100        // Camera request
    private val PERMISSION_CODE = 101            // Permission request

    // Root view for Snackbar
    private lateinit var rootView: View

    // Called when activity starts
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Attach XML layout
        setContentView(R.layout.activity_account1)

        // Get root view
        rootView = findViewById(android.R.id.content)

        // Initialize UI elements
        button = findViewById(R.id.button7)       // Camera button
        imageView = findViewById(R.id.imageView2) // ImageView for profile picture

        val btn = findViewById<MaterialButton>(R.id.button3) // Next button

        // Input fields
        val phone = findViewById<EditText>(R.id.editTextTextEmailAddress)
        val dob = findViewById<EditText>(R.id.editTextTextEmailAddress20)
        val nic = findViewById<EditText>(R.id.editTextTextEmailAddress21)
        val city = findViewById<EditText>(R.id.editTextTextEmailAddress22)

        // Gender radio buttons
        val male = findViewById<RadioButton>(R.id.radioButtonm)
        val female = findViewById<RadioButton>(R.id.radioButtonf)

        // Ensure ImageView is fully loaded before using width/height
        imageView.post { }

        // Next button click listener
        btn.setOnClickListener {

            // Get user inputs
            val phoneValue = phone.text.toString().trim()
            val dobValue = dob.text.toString().trim()
            val nicValue = nic.text.toString().trim()
            val cityValue = city.text.toString().trim()

            // Get selected gender
            val genderValue =
                if (male.isChecked) "Male"
                else if (female.isChecked) "Female"
                else ""

            // Input validation
            when {
                phoneValue.isEmpty() -> {
                    phone.error = "Enter phone number"
                    return@setOnClickListener
                }
                !phoneValue.matches(Regex("^[0-9]{10}$")) -> {
                    phone.error = "Phone number must be 10 digits"
                    return@setOnClickListener
                }
                dobValue.isEmpty() -> {
                    dob.error = "Enter Date of Birth"
                    return@setOnClickListener
                }
                nicValue.isEmpty() -> {
                    nic.error = "Enter NIC"
                    return@setOnClickListener
                }
                !nicValue.matches(Regex("^[0-9]{9}[vVxX]$|^[0-9]{12}$")) -> {
                    nic.error = "Invalid NIC format"
                    return@setOnClickListener
                }
                cityValue.isEmpty() -> {
                    city.error = "Enter your city"
                    return@setOnClickListener
                }
                genderValue.isEmpty() -> {
                    Toast.makeText(this, "Select gender", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Move to next activity and pass data
            val intent = Intent(this, Account2Activity::class.java)
            intent.putExtra("phone", phoneValue)
            intent.putExtra("dob", dobValue)
            intent.putExtra("nic", nicValue)
            intent.putExtra("city", cityValue)
            intent.putExtra("gender", genderValue)

            startActivity(intent)
        }

        // Camera button click
        button.setOnClickListener {
            if (checkPermissions()) {
                openCamera()
            } else {
                requestPermission()
            }
        }
    }

    // ---------------- PERMISSION HANDLING ----------------

    // Check if camera & storage permissions are granted
    private fun checkPermissions(): Boolean {
        val cameraPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)

        val storagePermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                        storagePermission == PackageManager.PERMISSION_GRANTED)
    }

    // Request permissions at runtime
    private fun requestPermission() {
        val permissions = mutableListOf(Manifest.permission.CAMERA)

        // Storage permission needed only for Android < 10
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        ActivityCompat.requestPermissions(
            this,
            permissions.toTypedArray(),
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

        // Check camera result
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            // Get captured image as Bitmap
            val bitmap = data?.extras?.get("data") as? Bitmap

            bitmap?.let {

                // 1️⃣ Crop image into a circle
                val circularBitmap = cropToCircle(it)

                // 2️⃣ Resize image to ImageView size
                val finalBitmap = Bitmap.createScaledBitmap(
                    circularBitmap,
                    imageView.width,
                    imageView.height,
                    true
                )

                // Display image
                bitmapImage = finalBitmap
                imageView.setImageBitmap(finalBitmap)

                // Save image to gallery
                saveImageToGallery(finalBitmap)
            }
        }
    }

    // ---------------- CIRCULAR IMAGE CROP ----------------

    // Converts a square image into a circular image
    private fun cropToCircle(bitmap: Bitmap): Bitmap {

        // Determine smallest side
        val size = minOf(bitmap.width, bitmap.height)

        // Center crop coordinates
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2

        // Create square bitmap
        val squaredBitmap = Bitmap.createBitmap(bitmap, x, y, size, size)

        // Output bitmap
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Paint with bitmap shader
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(
                squaredBitmap,
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP
            )
        }

        // Draw circular bitmap
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        return output
    }

    // ---------------- SAVE IMAGE ----------------

    // Save captured image to device gallery
    private fun saveImageToGallery(bitmap: Bitmap) {

        // Generate unique filename
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
        val uri: Uri? = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        // Write bitmap data
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
}
