package com.spica.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AccountActivity : AppCompatActivity() {

    private lateinit var nameInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var saveBtn: Button
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        prefs = getSharedPreferences("spica_prefs", Context.MODE_PRIVATE)

        nameInput = findViewById(R.id.nameInput)
        phoneInput = findViewById(R.id.phoneInput)
        saveBtn = findViewById(R.id.saveAccountBtn)

        // Load saved data
        nameInput.setText(prefs.getString("user_name", ""))
        phoneInput.setText(prefs.getString("user_phone", ""))

        saveBtn.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()

            prefs.edit()
                .putString("user_name", name)
                .putString("user_phone", phone)
                .apply()

            Toast.makeText(this, "Account saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
