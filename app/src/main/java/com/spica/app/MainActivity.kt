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
    private lateinit var navAbout: TextView

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
        navAbout = findViewById(R.id.navAbout)

        audioRecorder = AudioRecorder(this)
        videoRecorder = VideoRecorder(this, this)
        smsSender = SmsSender(this)
        shareHelper = ShareHelper(this)

        askPermissions()
        updateDrawerInfo()

        menuBtn.setOnClickListener {
            drawerLayout.openDrawer(findViewById(R.id.navDrawer))
        }

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

        navAbout.setOnClickListener
