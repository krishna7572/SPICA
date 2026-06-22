package com.spica.app

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    private var speechService: SpeechService? = null
    private var isListening = false

    private lateinit var audioRecorder: AudioRecorder
    private var isRecordingAudio = false

    private lateinit var videoRecorder: VideoRecorder
    private var isRecordingVideo = false

    private lateinit var smsSender: SmsSender
    private lateinit var shareHelper: ShareHelper

    private var currentLang = "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        startBtn = findViewById(R.id.startListeningBtn)
        stopBtn = findViewById(R.id.stopListeningBtn)
        sosBtn = findViewById(R.id.sosBtn)
        contactsBtn = findViewById(R.id.contactsBtn)

        audioRecorder = AudioRecorder(this)
        videoRecorder = VideoRecorder(this, this)
        smsSender = SmsSender(this)
        shareHelper = ShareHelper(this)

        askPermissions()

        startBtn.setOnClickListener { startListening() }
        stopBtn.setOnClickListener { stopListening() }
        sosBtn.setOnClickListener { triggerEmergency() }
        contactsBtn.setOnClickListener {
            startActivity(Intent(this, ContactsActivity::class.java))
        }
    }

    private fun askPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS
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
        if (outDir.exists()) return outDir.absolutePath

        outDir.mkdirs()
        assets.list(modelFolder)?.forEach { fileName ->
            val input = assets.open("$modelFolder/$fileName")
            val outFile = File(outDir, fileName)
            outFile.outputStream().use { input.copyTo(it) }
        }
        return outDir.absolutePath
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
        // Auto language switch
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
