package com.spica.app

import android.content.Context
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ContactsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactsAdapter
    private lateinit var contactsManager: ContactsManager
    private lateinit var searchInput: EditText
    private lateinit var deviceContactsList: ListView
    private lateinit var deviceAdapter: ArrayAdapter<String>

    private val deviceContacts = mutableListOf<Pair<String, String>>() // name, phone

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        contactsManager = ContactsManager(this)

        recyclerView = findViewById(R.id.contactsRecyclerView)
        searchInput = findViewById(R.id.searchInput)
        deviceContactsList = findViewById(R.id.deviceContactsList)

        // SPICA saved contacts
        adapter = ContactsAdapter(
            contactsManager.getContacts().toMutableList(),
            onDelete = { contact ->
                contactsManager.removeContact(contact.phone)
                refreshSavedContacts()
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Load device contacts
        loadDeviceContacts("")

        // Search filter
        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                loadDeviceContacts(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        // Tap device contact to add to SPICA
        deviceContactsList.setOnItemClickListener { _, _, position, _ ->
            val selected = deviceContacts[position]
            val name = selected.first
            val phone = selected.second

            if (phone.isEmpty()) {
                Toast.makeText(this, "No phone number for $name", Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }

            val existing = contactsManager.getContacts().map { it.phone }
            if (existing.contains(phone)) {
                Toast.makeText(this, "$name already added!", Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }

            contactsManager.addContact(Contact(name, phone))
            refreshSavedContacts()
            Toast.makeText(this, "$name added to SPICA!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDeviceContacts(query: String) {
        deviceContacts.clear()
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            if (query.isNotEmpty())
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            else null,
            if (query.isNotEmpty()) arrayOf("%$query%") else null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val name = it.getString(nameIdx) ?: ""
                val phone = it.getString(phoneIdx) ?: ""
                deviceContacts.add(Pair(name, phone))
            }
        }

        val displayList = deviceContacts.map { "${it.first}  |  ${it.second}" }
        deviceAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        deviceContactsList.adapter = deviceAdapter
    }

    private fun refreshSavedContacts() {
        adapter.updateList(contactsManager.getContacts().toMutableList())
    }
}
