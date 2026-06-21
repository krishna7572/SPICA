package com.spica.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ContactStorage(context: Context) {

    private val prefs = context.getSharedPreferences("spica_contacts", Context.MODE_PRIVATE)

    // Saare contacts laao
    fun getContacts(): MutableList<Contact> {
        val list = mutableListOf<Contact>()
        val data = prefs.getString("contacts", "[]")
        val array = JSONArray(data)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(Contact(obj.getString("name"), obj.getString("number")))
        }
        return list
    }

    // Naya contact add karo
    fun addContact(contact: Contact) {
        val list = getContacts()
        list.add(contact)
        saveAll(list)
    }

    // Contact remove karo
    fun removeContact(index: Int) {
        val list = getContacts()
        if (index in list.indices) {
            list.removeAt(index)
            saveAll(list)
        }
    }

    // Sab save karo
    private fun saveAll(list: List<Contact>) {
        val array = JSONArray()
        for (c in list) {
            val obj = JSONObject()
            obj.put("name", c.name)
            obj.put("number", c.number)
            array.put(obj)
        }
        prefs.edit().putString("contacts", array.toString()).apply()
    }
}
