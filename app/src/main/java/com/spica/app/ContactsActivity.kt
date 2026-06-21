package com.spica.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ContactsActivity : AppCompatActivity() {

    private lateinit var storage: ContactStorage
    private lateinit var adapter: ContactAdapter
    private lateinit var contacts: MutableList<Contact>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        storage = ContactStorage(this)
        contacts = storage.getContacts()

        val nameInput = findViewById<EditText>(R.id.nameInput)
        val numberInput = findViewById<EditText>(R.id.numberInput)
        val addBtn = findViewById<Button>(R.id.addContactBtn)
        val list = findViewById<RecyclerView>(R.id.contactsList)

        adapter = ContactAdapter(contacts) { position ->
            storage.removeContact(position)
            contacts.removeAt(position)
            adapter.notifyItemRemoved(position)
            Toast.makeText(this, "Contact removed", Toast.LENGTH_SHORT).show()
        }

        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        addBtn.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val number = numberInput.text.toString().trim()

            if (name.isEmpty() || number.isEmpty()) {
                Toast.makeText(this, "Fill both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val contact = Contact(name, number)
            storage.addContact(contact)
            contacts.add(contact)
            adapter.notifyItemInserted(contacts.size - 1)

            nameInput.text.clear()
            numberInput.text.clear()
            Toast.makeText(this, "Contact added", Toast.LENGTH_SHORT).show()
        }
    }
}
