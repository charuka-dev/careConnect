package com.charukalakshan.careconnect

import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class CategoryActivity : AppCompatActivity() {

    private lateinit var radioButton1: RadioButton
    private lateinit var radioButton2: RadioButton
    private lateinit var buttonNext: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        radioButton1 = findViewById(R.id.radioButton)    // Looking for services
        radioButton2 = findViewById(R.id.radioButton2)   // Offer services
        buttonNext = findViewById(R.id.button3)

        // ✅ Ensure only ONE RadioButton is checked at a time
        radioButton1.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                radioButton2.isChecked = false
            }
        }

        radioButton2.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                radioButton1.isChecked = false
            }
        }

        buttonNext.setOnClickListener {
            when {
                radioButton1.isChecked -> {
                    startActivity(Intent(this, Account3Activity::class.java))
                }
                radioButton2.isChecked -> {
                    startActivity(Intent(this, Account1Activity::class.java))
                }
                else -> {
                    Toast.makeText(this, "Please select an option", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

