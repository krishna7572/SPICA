package com.spica.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ContactStorage(context: Context) {

    private val prefs = context.getSharedPreferences("spica_contacts", Context.MODE_PRIVATE)

    fun getContacts(): MutableList<Contact> {
        val list = mutableListOf<Contact>()
        val data = prefs.getString("contacts", "[]")
        val array = JSONArray(data)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val name = obj.getString("name")
            val phone = obj.optString("phone", obj.optString("number", ""))
            list.add(Contact(name, phone))
        }
        return list
    }

    fun addContact(contact: Contact) {
        val list = getContacts()
        list.add(contact)
        saveAll(list)
    }

    fun removeContact(index: Int) {
        val list = getContacts()
        if (index in list.indices) {
            list.removeAt(index)
            saveAll(list)
        }
    }

    private fun saveAll(list: List<Contact>) {
        val array = JSONArray()
        for (c in list) {
            val obj = JSONObject()
            obj.put("name", c.name)
            obj.put("phone", c.phone)
            array.put(obj)
        }
        prefs.edit().putString("contacts", array.toString()).apply()
    }
}
