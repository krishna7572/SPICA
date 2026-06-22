package com.spica.app

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast

class SmsSender(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("spica_prefs", Context.MODE_PRIVATE)

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    companion object {
        const val DEFAULT_SMS = "🆘 EMERGENCY ALERT! I am in danger and need immediate help. Please contact me or send help to my location as soon as possible. This is an automated message from SPICA Emergency App. My current location: {location}"
    }

    fun sendEmergencySms(onDone: (Int) -> Unit) {
        try {
            val provider = when {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                    LocationManager.GPS_PROVIDER
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                    LocationManager.NETWORK_PROVIDER
                else -> null
            }

            if (provider != null) {
                locationManager.requestSingleUpdate(provider, object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        val locationStr = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                        sendSmsToContacts(locationStr, onDone)
                    }
                    override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
                    override fun onProviderEnabled(p: String) {}
                    override fun onProviderDisabled(p: String) {}
                }, null)

                // Fallback after 5 seconds if GPS slow
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    sendSmsToContacts("Location unavailable", onDone)
                }, 5000)

            } else {
                sendSmsToContacts("Location unavailable", onDone)
            }
        } catch (e: SecurityException) {
            sendSmsToContacts("Location permission denied", onDone)
        }
    }

    private fun sendSmsToContacts(locationStr: String, onDone: (Int) -> Unit) {
        val contacts = ContactsManager(context).getContacts()
        if (contacts.isEmpty()) {
            onDone(0)
            return
        }

        val template = prefs.getString("emergency_sms", DEFAULT_SMS) ?: DEFAULT_SMS
        val message = template.replace("{location}", locationStr)

        var sentCount = 0
        val smsManager = SmsManager.getDefault()

        contacts.forEach { contact ->
            try {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(
                    contact.phone, null, parts, null, null
                )
                sentCount++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        onDone(sentCount)
    }
}
