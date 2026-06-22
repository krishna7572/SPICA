package com.spica.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Contact(val name: String, val phone: String)

class ContactsManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("spica_contacts", Context.MODE_PRIVATE)

    fun getContacts(): List<Contact> {
        val json = prefs.getString("contacts", "[]") ?: "[]"
        val arr = JSONArray(json)
        val list = mutableListOf<Contact>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(Contact(obj.getString("name"), obj.getString("phone")))
        }
        return list
    }

    fun addContact(contact: Contact) {
        val list = getContacts().toMutableList()
        if (list.none { it.phone == contact.phone }) {
            list.add(contact)
            saveContacts(list)
        }
    }

    fun removeContact(phone: String) {
        val list = getContacts().filter { it.phone != phone }
        saveContacts(list)
    }

    private fun saveContacts(list: List<Contact>) {
        val arr = JSONArray()
        list.forEach {
            val obj = JSONObject()
            obj.put("name", it.name)
            obj.put("phone", it.phone)
            arr.put(obj)
        }
        prefs.edit().putString("contacts", arr.toString()).apply()
    }
}
