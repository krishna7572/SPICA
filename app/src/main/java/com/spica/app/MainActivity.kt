package com.spica.app

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity(), RecognitionListener {

    private lateinit var statusText: TextView
    private lateinit var startBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var sosBtn: Button
    private lateinit var contactsBtn: Button
    private lateinit var menuBtn: ImageButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerName: TextView
    private lateinit var drawerPhone: TextView
    private lateinit var navAccount: TextView
    private lateinit var navEditSms: TextView
    private lateinit var navContacts: TextView

    private var speechService: SpeechService? = null
    private var isListening = false

    private lateinit var audioRecorder: AudioRecorder
    private var isRecordingAudio = false

    private lateinit var videoRecorder: VideoRecorder
    private var isRecordingVideo = false

    private lateinit var smsSender: SmsSender
    private lateinit var shareHelper: ShareHelper
    private lateinit var prefs: SharedPreferences

    private var currentLang = "en"

    companion object {
        const val DEFAULT_SMS = "🆘 EMERGENCY ALERT! I am in danger and need immediate help. Please contact me or send help to my location as soon as possible. This is an automated message from SPICA Emergency App. My current location: {location}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("spica_prefs", Context.MODE_PRIVATE)

        statusText = findViewById(R.id.statusText)
        startBtn = findViewById(R.id.startListeningBtn)
        stopBtn = findViewById(R.id.stopListeningBtn)
        sosBtn = findViewById(R.id.sosBtn)
        contactsBtn = findViewById(R.id.contactsBtn)
        menuBtn = findViewById(R.id.menuBtn)
        drawerLayout = findViewById(R.id.drawerLayout)
        drawerName = findViewById(R.id.drawerName)
        drawerPhone = findViewById(R.id.drawerPhone)
        navAccount = findViewById(R.id.navAccount)
        navEditSms = findViewById(R.id.navEditSms)
        navContacts = findViewById(R.id.navContacts)

        audioRecorder = AudioRecorder(this)
        videoRecorder = VideoRecorder(this, this)
        smsSender = SmsSender(this)
        shareHelper = ShareHelper(this)

        askPermissions()
        updateDrawerInfo()

        menuBtn.setOnClickListener { drawerLayout.openDrawer(findViewById(R.id.navDrawer)) }

        navAccount.setOnClickListener {
            drawerLayout.closeDrawers()
            startActivity(Intent(this, AccountActivity::class.java))
        }

        navEditSms.setOnClickListener {
            drawerLayout.closeDrawers()
            showEditSmsDialog()
        }

        navContacts.setOnClickListener {
            drawerLayout.closeDrawers()
            startActivity(Intent(this, ContactsActivity::class.java))
        }

        startBtn.setOnClickListener { startListening() }
        stopBtn.setOnClickListener { stopListening() }
        sosBtn.setOnClickListener { triggerEmergency() }
        contactsBtn.setOnClickListener {
            startActivity(Intent(this, ContactsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateDrawerInfo()
    }

    private fun updateDrawerInfo() {
        val name = prefs.getString("user_name", "") ?: ""
        val phone = prefs.getString("user_phone", "") ?: ""
        drawerName.text = if (name.isNotEmpty()) name else "Tap to set name"
        drawerPhone.text = if (phone.isNotEmpty()) phone else "No number set"
    }

    private fun showEditSmsDialog() {
        val currentMsg = prefs.getString("emergency_sms", DEFAULT_SMS) ?: DEFAULT_SMS
        val input = EditText(this)
        input.setText(currentMsg)
        input.setTextColor(0xFFFFFFFF.toInt())
        input.setBackgroundColor(0xFF161B33.toInt())
        input.setPadding(24, 24, 24, 24)
        input.minLines = 4
        input.maxLines = 8
        input.setHorizontallyScrolling(false)

        AlertDialog.Builder(this)
            .setTitle("Edit Emergency SMS")
            .setMessage("Use {location} where you want location to appear.")
            .setView(input)
            .setPositiveButton("SAVE") { _, _ ->
                val msg = input.text.toString().trim()
                if (msg.isNotEmpty()) {
                    prefs.edit().putString("emergency_sms", msg).apply()
                    Toast.makeText(this, "SMS message updated!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("RESET DEFAULT") { _, _ ->
                prefs.edit().putString("emergency_sms", DEFAULT_SMS).apply()
                Toast.makeText(this, "Reset to default!", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("CANCEL", null)
            .show()
    }

    private fun askPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS
        )
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 1)
        }
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission needed", Toast.LENGTH_SHORT).show()
            askPermissions()
            return
        }

        statusText.text = "● LOADING MODEL..."
        statusText.setTextColor(0xFF4D9DE0.toInt())

        Thread {
            try {
                val modelPath = copyModelToCache(currentLang)
                val model = Model(modelPath)
                val recognizer = Recognizer(model, 16000.0f)
                speechService = SpeechService(recognizer, 16000.0f)
                speechService?.startListening(this)
                runOnUiThread {
                    isListening = true
                    statusText.text = "● LISTENING..."
                    statusText.setTextColor(0xFF4D9DE0.toInt())
                }
            } catch (e: IOException) {
                runOnUiThread {
                    statusText.text = "● MODEL ERROR"
                    Toast.makeText(this, "Model load failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun copyModelToCache(lang: String): String {
    val modelFolder = if (lang == "hi") "model-hi" else "model-en"
    val outDir = File(cacheDir, modelFolder)
    if (outDir.exists() && outDir.list()?.isNotEmpty() == true) return outDir.absolutePath
    copyAssetFolder(modelFolder, outDir)
    return outDir.absolutePath
}

private fun copyAssetFolder(assetPath: String, outDir: File) {
    outDir.mkdirs()
    val list = assets.list(assetPath) ?: return
    for (item in list) {
        val subAsset = "$assetPath/$item"
        val outFile = File(outDir, item)
        val subList = assets.list(subAsset)
        if (subList != null && subList.isNotEmpty()) {
            copyAssetFolder(subAsset, outFile)
        } else {
            assets.open(subAsset).use { input ->
                outFile.outputStream().use { input.copyTo(it) }
            }
        }
    }
}
    private fun stopListening() {
        isListening = false
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
        statusText.text = "● SYSTEM READY"
        statusText.setTextColor(0xFF4CAF50.toInt())
        Toast.makeText(this, "Listening stopped", Toast.LENGTH_SHORT).show()
    }

    override fun onResult(hypothesis: String?) {
        hypothesis ?: return
        val text = hypothesis.lowercase()
        if (text.contains("\"text\"")) {
            val extracted = text.substringAfter("\"text\" : \"").substringBefore("\"").trim()
            if (extracted.isNotEmpty()) processCommand(extracted)
        }
    }

    override fun onPartialResult(hypothesis: String?) {}
    override fun onFinalResult(hypothesis: String?) {}
    override fun onError(e: Exception?) {
        runOnUiThread {
            Toast.makeText(this, "Voice error", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onTimeout() {}

    private fun processCommand(text: String) {
        if (Commands.SWITCH_TO_HINDI.any { text.contains(it) }) {
            currentLang = "hi"
            stopListening()
            startListening()
            Toast.makeText(this, "Hindi mode", Toast.LENGTH_SHORT).show()
            return
        }
        if (Commands.SWITCH_TO_ENGLISH.any { text.contains(it) }) {
            currentLang = "en"
            stopListening()
            startListening()
            Toast.makeText(this, "English mode", Toast.LENGTH_SHORT).show()
            return
        }

        when {
            Commands.START_AUDIO.any { text.contains(it) } -> startAudio()
            Commands.STOP_AUDIO.any { text.contains(it) } -> stopAudio()
            Commands.START_VIDEO.any { text.contains(it) } -> startVideo()
            Commands.STOP_VIDEO.any { text.contains(it) } -> stopVideo()
            Commands.EMERGENCY.any { text.contains(it) } -> triggerEmergency()
        }
    }

    private fun startAudio() {
        if (isRecordingAudio) return
        val started = audioRecorder.startRecording()
        if (started) {
            isRecordingAudio = true
            statusText.text = "● RECORDING AUDIO"
            statusText.setTextColor(0xFFE63946.toInt())
            Toast.makeText(this, "Audio Recording Started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopAudio() {
        if (!isRecordingAudio) return
        val file = audioRecorder.stopRecording()
        isRecordingAudio = false
        statusText.text = "● LISTENING..."
        statusText.setTextColor(0xFF4D9DE0.toInt())
        if (file != null) {
            showShareDialog(file)
        } else {
            Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startVideo() {
        if (isRecordingVideo) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Camera permission needed", Toast.LENGTH_SHORT).show()
            askPermissions()
            return
        }
        videoRecorder.startRecording { success ->
            if (success) {
                isRecordingVideo = true
                statusText.text = "● RECORDING VIDEO"
                statusText.setTextColor(0xFFE63946.toInt())
                Toast.makeText(this, "Video Recording Started", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Video failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopVideo() {
        if (!isRecordingVideo) return
        videoRecorder.stopRecording {
            isRecordingVideo = false
            statusText.text = "● LISTENING..."
            statusText.setTextColor(0xFF4D9DE0.toInt())
            Toast.makeText(this, "Video Saved to Movies/SPICA", Toast.LENGTH_LONG).show()
        }
    }

    private fun showShareDialog(file: java.io.File) {
        AlertDialog.Builder(this)
            .setTitle("Recording Saved")
            .setMessage("Share this recording?")
            .setPositiveButton("SHARE") { _, _ -> shareHelper.shareToAny(file) }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun triggerEmergency() {
        statusText.text = "● SOS ACTIVATED"
        statusText.setTextColor(0xFFE63946.toInt())
        Toast.makeText(this, "EMERGENCY ACTIVATED!", Toast.LENGTH_SHORT).show()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            if (!isRecordingAudio) {
                val started = audioRecorder.startRecording()
                if (started) isRecordingAudio = true
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            if (!isRecordingVideo) {
                videoRecorder.startRecording { success ->
                    if (success) isRecordingVideo = true
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            == PackageManager.PERMISSION_GRANTED) {
            smsSender.sendEmergencySms { count ->
                if (count > 0) {
                    Toast.makeText(this, "SOS: SMS sent to $count contact(s)",
                        Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "SOS: No contacts! Add contacts first",
                        Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(this, "SMS permission needed", Toast.LENGTH_SHORT).show()
            askPermissions()
        }

        statusText.text = "● SOS: RECORDING + ALERT SENT"
    }

    override fun onDestroy() {
        super.onDestroy()
        speechService?.shutdown()
    }
}
