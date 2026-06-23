package com.spica.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CommandsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var startAudioInput: EditText
    private lateinit var stopAudioInput: EditText
    private lateinit var startVideoInput: EditText
    private lateinit var stopVideoInput: EditText
    private lateinit var emergencyInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_commands)

        prefs = getSharedPreferences("spica_commands", Context.MODE_PRIVATE)

        startAudioInput = findViewById(R.id.startAudioInput)
        stopAudioInput = findViewById(R.id.stopAudioInput)
        startVideoInput = findViewById(R.id.startVideoInput)
        stopVideoInput = findViewById(R.id.stopVideoInput)
        emergencyInput = findViewById(R.id.emergencyInput)

        loadCommands()

        findViewById<Button>(R.id.saveCommandsBtn).setOnClickListener {
            saveCommands()
        }

        findViewById<Button>(R.id.resetCommandsBtn).setOnClickListener {
            resetCommands()
        }
    }

    private fun loadCommands() {
        startAudioInput.setText(prefs.getString("start_audio",
            "start recording, recording shuru karo, spica start recording, audio shuru karo"))
        stopAudioInput.setText(prefs.getString("stop_audio",
            "stop recording, recording band karo, spica stop recording, audio band karo"))
        startVideoInput.setText(prefs.getString("start_video",
            "start video, video shuru karo, spica start video, camera shuru karo"))
        stopVideoInput.setText(prefs.getString("stop_video",
            "stop video, video band karo, spica stop video, camera band karo"))
        emergencyInput.setText(prefs.getString("emergency",
            "emergency, madad, spica emergency, spica madad, help me"))
    }

    private fun saveCommands() {
        prefs.edit()
            .putString("start_audio", startAudioInput.text.toString().trim())
            .putString("stop_audio", stopAudioInput.text.toString().trim())
            .putString("start_video", startVideoInput.text.toString().trim())
            .putString("stop_video", stopVideoInput.text.toString().trim())
            .putString("emergency", emergencyInput.text.toString().trim())
            .apply()
        Toast.makeText(this, "Commands saved!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun resetCommands() {
        prefs.edit().clear().apply()
        loadCommands()
        Toast.makeText(this, "Reset to default!", Toast.LENGTH_SHORT).show()
    }
}
