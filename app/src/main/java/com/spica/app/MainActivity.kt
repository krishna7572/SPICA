package com.spica.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var startBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var sosBtn: Button
    private lateinit var contactsBtn: Button

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        startBtn = findViewById(R.id.startListeningBtn)
        stopBtn = findViewById(R.id.stopListeningBtn)
        sosBtn = findViewById(R.id.sosBtn)
        contactsBtn = findViewById(R.id.contactsBtn)

        askPermissions()

        startBtn.setOnClickListener { startListening() }
        stopBtn.setOnClickListener { stopListening() }
        sosBtn.setOnClickListener {
            Toast.makeText(this, "SOS Triggered!", Toast.LENGTH_SHORT).show()
            statusText.text = "● SOS ACTIVATED"
        }
        contactsBtn.setOnClickListener {
            Toast.makeText(this, "Contacts (coming soon)", Toast.LENGTH_SHORT).show()
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

        isListening = true
        statusText.text = "● LISTENING..."
        statusText.setTextColor(0xFF4D9DE0.toInt())

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    processCommand(matches[0].lowercase())
                }
                if (isListening) restartListening()
            }

            override fun onError(error: Int) {
                if (isListening) restartListening()
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        beginRecognition()
    }

    private fun beginRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        speechRecognizer?.startListening(intent)
    }

    private fun restartListening() {
        speechRecognizer?.cancel()
        beginRecognition()
    }

    private fun stopListening() {
        isListening = false
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        statusText.text = "● SYSTEM READY"
        statusText.setTextColor(0xFF4CAF50.toInt())
        Toast.makeText(this, "Listening stopped", Toast.LENGTH_SHORT).show()
    }

    private fun processCommand(text: String) {
        when {
            Commands.START_AUDIO.any { text.contains(it) } -> {
                Toast.makeText(this, "Audio Recording Started", Toast.LENGTH_SHORT).show()
                statusText.text = "● RECORDING AUDIO"
            }
            Commands.STOP_AUDIO.any { text.contains(it) } -> {
                Toast.makeText(this, "Audio Recording Stopped", Toast.LENGTH_SHORT).show()
                statusText.text = "● LISTENING..."
            }
            Commands.START_VIDEO.any { text.contains(it) } -> {
                Toast.makeText(this, "Video Recording Started", Toast.LENGTH_SHORT).show()
                statusText.text = "● RECORDING VIDEO"
            }
            Commands.STOP_VIDEO.any { text.contains(it) } -> {
                Toast.makeText(this, "Video Recording Stopped", Toast.LENGTH_SHORT).show()
                statusText.text = "● LISTENING..."
            }
            Commands.EMERGENCY.any { text.contains(it) } -> {
                Toast.makeText(this, "EMERGENCY ACTIVATED", Toast.LENGTH_SHORT).show()
                statusText.text = "● SOS ACTIVATED"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}
