package com.charukalakshan.careconnect  // Package name of the app

// OtpHelper is a singleton object (only one instance exists)
object OtpHelper {

    // Function to generate a random OTP
    fun generateOtp(): String {

        // Generates a random number between 10000 and 99999
        // This ensures the OTP is always 5 digits
        // Converts the number to String before returning
        return (10000..99999).random().toString()
    }
}
