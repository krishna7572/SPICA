package com.spica.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactsAdapter(
    private var contacts: MutableList<Contact>,
    private val onDelete: (Contact) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.contactName)
        val phoneText: TextView = view.findViewById(R.id.contactPhone)
        val deleteBtn: ImageButton = view.findViewById(R.id.deleteBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        holder.nameText.text = contact.name
        holder.phoneText.text = contact.phone
        holder.deleteBtn.setOnClickListener { onDelete(contact) }
    }

    override fun getItemCount() = contacts.size

    fun updateList(newList: MutableList<Contact>) {
        contacts = newList
        notifyDataSetChanged()
    }
}
