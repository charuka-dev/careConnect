package com.charukalakshan.careconnect

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserMyProfileActivity : AppCompatActivity() {
    private lateinit var imgProfile: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvDob: TextView
    private lateinit var tvGender: TextView
    private lateinit var tvCity: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usermyprofile)

        imgProfile = findViewById(R.id.imgProfile)
        tvUserName = findViewById(R.id.tvUserName)
        tvEmail = findViewById(R.id.tvEmail)
        tvPhone = findViewById(R.id.tvPhone)
        tvDob = findViewById(R.id.tvDob)
        tvGender = findViewById(R.id.tvGender)
        tvCity = findViewById(R.id.tvCity)

        val btnLogout = findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btnLogout)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            // not logged in - show defaults
            tvUserName.text = "Guest"
            tvEmail.text = "Email: -"
            tvPhone.text = "Phone: -"
            tvDob.text = "Date of Birth: -"
            tvGender.text = "Gender: -"
            tvCity.text = "City: -"
            btnLogout.isEnabled = false
            return
        }

        // initialize UI with placeholders until data loads
        tvUserName.text = "-"
        tvEmail.text = "Email: -"
        tvPhone.text = "Phone: -"
        tvDob.text = "Date of Birth: -"
        tvGender.text = "Gender: -"
        tvCity.text = "City: -"

        val uid = currentUser.uid
        val userRef = FirebaseDatabase.getInstance().getReference("User").child(uid)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java)
                val email = snapshot.child("email").getValue(String::class.java)
                val phone = snapshot.child("phone").getValue(String::class.java)
                val dob = snapshot.child("dob").getValue(String::class.java)
                val city = snapshot.child("city").getValue(String::class.java)
                val photo = snapshot.child("photoUrl").getValue(String::class.java)

                // apply user values (if present)
                if (!name.isNullOrBlank()) tvUserName.text = name
                if (!email.isNullOrBlank()) tvEmail.text = "Email: $email"
                if (!phone.isNullOrBlank()) tvPhone.text = "Phone: $phone"
                if (!dob.isNullOrBlank()) tvDob.text = "Date of Birth: $dob"
                if (!city.isNullOrBlank()) tvCity.text = "City: $city"

                if (!photo.isNullOrEmpty()) {
                    Glide.with(this@UserMyProfileActivity).load(photo).circleCrop().into(imgProfile)
                }

                // lookup customers: prefer userId match, then email, then phone
                mergeCustomerIfExists(uid, email, phone)
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // back button
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        // logout behaviour: sign out and go to LoginActivity with cleared back stack
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // bottom nav: handle all menu items: home, payment, calendar, messages, profile
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.setOnItemSelectedListener { item ->
            val menu = bottomNav.menu
            val homeId = menu.getItem(0).itemId
            val paymentId = menu.getItem(1).itemId
            val calendarId = menu.getItem(2).itemId
            val messagesId = menu.getItem(3).itemId
            val profileId = menu.getItem(4).itemId
            when (item.itemId) {
                homeId -> {
                    startActivity(Intent(this, ActivityUserHome::class.java))
                    true
                }
                paymentId -> {
                    startActivity(Intent(this, com.charukalakshan.careconnect.SettleMoneyActivity::class.java))
                    true
                }
                calendarId -> {
                    startActivity(Intent(this, BookingsActivity::class.java))
                    true
                }
                messagesId -> {
                    startActivity(Intent(this, com.charukalakshan.careconnect.UMessageActivity::class.java))
                    true
                }
                profileId -> {
                    // already here
                    true
                }
                else -> false
            }
        }
    }

    // new approach: query customers by userId first, then by email, then by phone
    private fun mergeCustomerIfExists(uid: String, email: String?, phone: String?) {
        // Try a few common customer node names to handle schema differences
        val possiblePaths = listOf("customers", "Customer", "customer", "Customers")

        fun tryPath(pathIndex: Int) {
            if (pathIndex >= possiblePaths.size) {
                // no matching customer path found
                runOnUiThread {
                    Toast.makeText(this@UserMyProfileActivity, "No customer record found", Toast.LENGTH_SHORT).show()
                }
                return
            }
            val path = possiblePaths[pathIndex]
            val ref = FirebaseDatabase.getInstance().getReference(path)
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        // try next path
                        tryPath(pathIndex + 1)
                        return
                    }

                    // prefer userId match, then email, then phone
                    var applied = false

                    // helper: normalize phone to digits-only
                    fun normalizePhone(p: String?): String? {
                        if (p.isNullOrEmpty()) return null
                        val digits = p.filter { it.isDigit() }
                        return if (digits.isEmpty()) null else digits
                    }

                    fun phonesMatch(a: String?, b: String?): Boolean {
                        val na = normalizePhone(a) ?: return false
                        val nb = normalizePhone(b) ?: return false
                        if (na == nb) return true
                        // match by suffix: allow country code differences - compare last 9 digits
                        val la = if (na.length > 9) na.takeLast(9) else na
                        val lb = if (nb.length > 9) nb.takeLast(9) else nb
                        return la == lb
                    }

                     // helper to extract flexible keys
                     fun getStringFlexible(child: DataSnapshot, vararg keys: String): String? {
                         for (k in keys) {
                             val v = child.child(k).getValue(String::class.java)
                             if (!v.isNullOrEmpty()) return v
                         }
                         return null
                     }

                     // 1) try userId using multiple possible key names
                     for (ch in snapshot.children) {
                         val cUserId = getStringFlexible(ch, "userId", "userid", "user_id", "userID")
                         if (!cUserId.isNullOrEmpty() && cUserId == uid) {
                             runOnUiThread { applyCustomer(ch) }
                             Log.d("UserMyProfile", "Applied customer data from path $path for userId match")
                             applied = true
                             break
                         }
                     }

                     if (applied) return

                     // 2) try email using multiple keys
                     if (!email.isNullOrEmpty()) {
                         for (ch in snapshot.children) {
                             val cEmail = getStringFlexible(ch, "email", "Email")
                             if (!cEmail.isNullOrEmpty() && cEmail.equals(email, ignoreCase = true)) {
                                 runOnUiThread { applyCustomer(ch) }
                                 Log.d("UserMyProfile", "Applied customer data from path $path for email match")
                                 applied = true
                                 break
                             }
                         }
                     }

                     if (applied) return

                     // 3) try phone using multiple possible keys
                     if (!phone.isNullOrEmpty()) {
                         for (ch in snapshot.children) {
                             val cPhone = getStringFlexible(ch, "phone", "Phone", "mobile", "mobileNo", "phoneNumber")
                             if (!cPhone.isNullOrEmpty() && phonesMatch(cPhone, phone)) {
                                 runOnUiThread { applyCustomer(ch) }
                                 Log.d("UserMyProfile", "Applied customer data from path $path for phone match")
                                 applied = true
                                 break
                             }
                         }
                     }

                     if (applied) return

                     // 4) final fallback: check any string value in child equals uid/email/phone
                     for (ch in snapshot.children) {
                         var found = false
                         for (g in ch.children) {
                             val v = g.getValue(String::class.java)
                             if (!v.isNullOrEmpty()) {
                                 if (v == uid || (!email.isNullOrEmpty() && v.equals(email, ignoreCase = true)) || (!phone.isNullOrEmpty() && phonesMatch(v, phone))) {
                                     runOnUiThread { applyCustomer(ch) }
                                     Log.d("UserMyProfile", "Applied customer data from path $path by fallback value match")
                                     found = true
                                     break
                                 }
                             }
                         }
                         if (found) { applied = true; break }
                     }

                     if (!applied) {
                         // nothing matched in this path, try next
                         tryPath(pathIndex + 1)
                     }
                 }

                 override fun onCancelled(error: DatabaseError) {
                     // try next path on cancel as well
                     tryPath(pathIndex + 1)
                 }
             })
         }

         tryPath(0)
     }

     private fun applyCustomer(child: DataSnapshot) {
        runOnUiThread { Toast.makeText(this@UserMyProfileActivity, "Customer data merged", Toast.LENGTH_SHORT).show() }
        Log.d("UserMyProfile", "applyCustomer invoked for child=${child.key}")
         // accept multiple key names for each field as well
         fun getFlexible(child: DataSnapshot, vararg keys: String): String? {
             for (k in keys) {
                 val v = child.child(k).getValue(String::class.java)
                 if (!v.isNullOrEmpty()) return v
             }
             return null
         }

        val cPhone = getFlexible(child, "phone", "Phone", "mobile", "mobileNo", "phoneNumber")
        val cDob = getFlexible(child, "dob", "DOB", "dateOfBirth")
        val cCity = getFlexible(child, "city", "City")
        val cGender = getFlexible(child, "gender", "Gender")
        val imageBase64 = getFlexible(child, "imageBase64", "image", "image_base64")

        if (!cPhone.isNullOrEmpty()) tvPhone.text = "Phone: $cPhone"
        if (!cDob.isNullOrEmpty()) tvDob.text = "Date of Birth: $cDob"
        if (!cCity.isNullOrEmpty()) tvCity.text = "City: $cCity"
        if (!cGender.isNullOrEmpty()) tvGender.text = "Gender: $cGender"

        if (!imageBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imgProfile.setImageBitmap(bmp)
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
