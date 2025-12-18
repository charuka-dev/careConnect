package com.charukalakshan.careconnect

data class Booking(
    var bookingId: String? = null,
    var userId: String? = null,
    var caregiverUserId: String? = null,
    var status: String? = null,
    var payment: String? = null,
    var dateTime: String? = null,
    var note: String? = null,
    var createdAt: Long? = null
)

