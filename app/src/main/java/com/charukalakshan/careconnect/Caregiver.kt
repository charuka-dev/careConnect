package com.charukalakshan.careconnect

// Simple data model matching the keys saved under the "Caregiver" node in the database
data class Caregiver(
    var phone: String? = null,
    var dob: String? = null,
    var nic: String? = null,
    var city: String? = null,
    var gender: String? = null,
    var education: String? = null,
    var service: String? = null,
    var uid: String? = null,
    // populated from User node
    var name: String? = null,
    var email: String? = null,
    // average rating (0..5)
    var rating: Double? = 0.0
)
