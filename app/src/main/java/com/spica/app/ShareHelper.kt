package com.spica.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class ShareHelper(private val context: Context) {

    // WhatsApp pe share karo (kisi specific number pe)
    fun shareToWhatsApp(file: File, phoneNumber: String) {
        try {
            val uri = getFileUri(file)
            val cleanNumber = phoneNumber.replace("+", "").replace(" ", "")

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (file.name.endsWith(".mp4")) "video/mp4" else "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra("jid", "$cleanNumber@s.whatsapp.net")
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // WhatsApp nahi hai toh general share kholo
            shareToAny(file)
        }
    }

    // Koi bhi app pe share karo (Instagram, WhatsApp, etc. menu)
    fun shareToAny(file: File) {
        try {
            val uri = getFileUri(file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (file.name.endsWith(".mp4")) "video/mp4" else "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share Recording")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getFileUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
