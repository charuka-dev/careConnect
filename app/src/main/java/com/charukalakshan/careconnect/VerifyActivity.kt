// Package name of the application
package com.charukalakshan.careconnect

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class VerifyActivity : AppCompatActivity() {

    private lateinit var userId: String

    // ✅ OTP fields declared at class level so they can be cleared
    private lateinit var otp1: EditText
    private lateinit var otp2: EditText
    private lateinit var otp3: EditText
    private lateinit var otp4: EditText
    private lateinit var otp5: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify)

        userId = intent.getStringExtra("userId") ?: return

        val resendTextView = findViewById<TextView>(R.id.textView12)
        val confirmButton = findViewById<Button>(R.id.button10)

        // OTP digit input fields
        otp1 = findViewById(R.id.editTextOTP1)
        otp2 = findViewById(R.id.editTextOTP2)
        otp3 = findViewById(R.id.editTextOTP3)
        otp4 = findViewById(R.id.editTextOTP4)
        otp5 = findViewById(R.id.editTextOTP5)

        setupOtpAutoMove(otp1, otp2, otp3, otp4, otp5)

        // ---------------- Resend OTP ----------------

        val resendClickable = object : ClickableSpan() {
            override fun onClick(widget: View) {

                // ✅ CLEAR old OTP values
                clearOtpFields()

                // Generate new random 5-digit OTP
                val newOtp = (10000..99999).random().toString()

                FirebaseDatabase.getInstance()
                    .getReference("OTP")
                    .child(userId)
                    .setValue(newOtp)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this@VerifyActivity,
                            "New OTP: $newOtp",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }

        val spannable = SpannableString("Resend Code")
        spannable.setSpan(resendClickable, 0, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#2E8791")),
            0,
            spannable.length,
            0
        )
        spannable.setSpan(UnderlineSpan(), 0, spannable.length, 0)

        resendTextView.text = spannable
        resendTextView.movementMethod = LinkMovementMethod.getInstance()

        // ---------------- Confirm OTP ----------------

        confirmButton.setOnClickListener {

            val enteredOtp =
                otp1.text.toString() +
                        otp2.text.toString() +
                        otp3.text.toString() +
                        otp4.text.toString() +
                        otp5.text.toString()

            if (enteredOtp.length != 5) {
                Toast.makeText(this, "Enter all 5 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseDatabase.getInstance()
                .getReference("OTP")
                .child(userId)
                .get()
                .addOnSuccessListener { snapshot ->

                    val correctOtp = snapshot.getValue(String::class.java)

                    if (enteredOtp == correctOtp) {
                        Toast.makeText(this, "OTP Verified!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, CategoryActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Wrong OTP!", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    // ---------------- OTP Auto-Move ----------------

    private fun setupOtpAutoMove(vararg editTexts: EditText) {
        for (i in editTexts.indices) {
            editTexts[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && i < editTexts.size - 1) {
                        editTexts[i + 1].requestFocus()
                    } else if (s?.isEmpty() == true && i > 0) {
                        editTexts[i - 1].requestFocus()
                    }
                }
            })
        }
    }

    // ✅ NEW FUNCTION TO CLEAR OTP FIELDS
    private fun clearOtpFields() {
        otp1.text.clear()
        otp2.text.clear()
        otp3.text.clear()
        otp4.text.clear()
        otp5.text.clear()
        otp1.requestFocus()
    }
}
