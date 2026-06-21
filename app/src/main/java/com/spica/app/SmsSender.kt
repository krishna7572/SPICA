package com.spica.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

class SmsSender(private val context: Context) {

    @SuppressLint("MissingPermission")
    fun sendEmergencySms(onDone: (Int) -> Unit) {
        val storage = ContactStorage(context)
        val contacts = storage.getContacts()

        if (contacts.isEmpty()) {
            onDone(0)
            return
        }

        val location = getLocationLink()
        val message = Settings.EMERGENCY_MESSAGE.replace("{location}", location)

        var sentCount = 0
        try {
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= 31) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            for (contact in contacts) {
                try {
                    val parts = smsManager.divideMessage(message)
                    smsManager.sendMultipartTextMessage(
                        contact.number, null, parts, null, null
                    )
                    sentCount++
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onDone(sentCount)
    }

    @SuppressLint("MissingPermission")
    private fun getLocationLink(): String {
        return try {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                return "Location unavailable"
            }

            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (location != null) {
                "https://maps.google.com/?q=${location.latitude},${location.longitude}"
            } else {
                "Location unavailable"
            }
        } catch (e: Exception) {
            "Location unavailable"
        }
    }
}
