package com.re9ant.peacekeeper

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnSetup: Button
    private lateinit var cbMuteUnknown: CheckBox
    private lateinit var btnAddContact: Button
    private lateinit var lvMutedContacts: ListView

    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var mutedContactsAdapter: ArrayAdapter<String>
    private val mutedContactsList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPrefs = getSharedPreferences("muter_prefs", Context.MODE_PRIVATE)

        tvStatus = findViewById(R.id.tvStatus)
        btnSetup = findViewById(R.id.btnSetup)
        cbMuteUnknown = findViewById(R.id.cbMuteUnknown)
        btnAddContact = findViewById(R.id.btnAddContact)
        lvMutedContacts = findViewById(R.id.lvMutedContacts)

        // Setup Checkbox
        cbMuteUnknown.isChecked = sharedPrefs.getBoolean("mute_unknown", false)
        cbMuteUnknown.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("mute_unknown", isChecked).apply()
        }

        // Setup ListView
        mutedContactsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutedContactsList)
        lvMutedContacts.adapter = mutedContactsAdapter
        loadMutedContacts()

        // Long click to remove contact
        lvMutedContacts.setOnItemLongClickListener { _, _, position, _ ->
            val contact = mutedContactsList[position]
            mutedContactsList.removeAt(position)
            saveMutedContacts()
            mutedContactsAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Removed $contact", Toast.LENGTH_SHORT).show()
            true
        }

        btnSetup.setOnClickListener {
            requestPermissionsAndRole()
        }

        btnAddContact.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                pickContactLauncher.launch(null)
            } else {
                Toast.makeText(this, "Please setup permissions first", Toast.LENGTH_SHORT).show()
                requestPermissionsAndRole()
            }
        }
    }

    private fun loadMutedContacts() {
        mutedContactsList.clear()
        mutedContactsList.addAll(sharedPrefs.getStringSet("muted_contacts", emptySet()) ?: emptySet())
        mutedContactsAdapter.notifyDataSetChanged()
    }

    private fun saveMutedContacts() {
        sharedPrefs.edit().putStringSet("muted_contacts", mutedContactsList.toSet()).apply()
    }

    private val pickContactLauncher = registerForActivityResult(ActivityResultContracts.PickContact()) { contactUri ->
        contactUri?.let { uri ->
            try {
                var contactId: String? = null
                var hasPhoneNumber = 0
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        contactId = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                        hasPhoneNumber = it.getInt(it.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER))
                    }
                }

                if (contactId != null && hasPhoneNumber > 0) {
                    val phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )

                    phoneCursor?.use { pCursor ->
                        if (pCursor.moveToFirst()) {
                            val number = pCursor.getString(pCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                            val name = pCursor.getString(pCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                            val normalizedNumber = number.replace(Regex("[^0-9+]"), "")
                            val entry = "$name ($normalizedNumber)"

                            if (!mutedContactsList.contains(entry)) {
                                mutedContactsList.add(entry)
                                saveMutedContacts()
                                mutedContactsAdapter.notifyDataSetChanged()
                                Toast.makeText(this, "Added $name to muted list", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Contact already in list", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Contact does not have a phone number", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error selecting contact", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val hasContactsPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
        val hasRole = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        } else {
            false
        }

        if (hasContactsPermission && hasRole) {
            tvStatus.text = "Ready and Access Granted"
            tvStatus.setTextColor(Color.parseColor("#4CAF50"))
            btnSetup.isEnabled = false
        } else {
            tvStatus.text = "Permissions Required"
            tvStatus.setTextColor(Color.parseColor("#F44336"))
            btnSetup.isEnabled = true
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            requestRole()
        } else {
            updateStatus()
        }
    }

    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updateStatus()
    }

    private fun requestPermissionsAndRole() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            requestRole()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun requestRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                requestRoleLauncher.launch(intent)
            } else {
                updateStatus()
            }
        }
    }
}
