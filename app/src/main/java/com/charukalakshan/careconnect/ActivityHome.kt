package com.charukalakshan.careconnect

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ActivityHome : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)  // <-- Make sure you have activity_home.xml inside res/layout
    }
}
