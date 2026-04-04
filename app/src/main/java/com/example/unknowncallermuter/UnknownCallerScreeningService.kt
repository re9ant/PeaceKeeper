package com.example.unknowncallermuter

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

class UnknownCallerScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            return
        }

        val sharedPrefs = getSharedPreferences("muter_prefs", Context.MODE_PRIVATE)
        val muteUnknown = sharedPrefs.getBoolean("mute_unknown", false)
        val mutedContacts = sharedPrefs.getStringSet("muted_contacts", emptySet()) ?: emptySet()

        // Optimization: If both options are OFF, exit immediately
        if (!muteUnknown && mutedContacts.isEmpty()) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val handle: Uri? = callDetails.handle
        val phoneNumber = handle?.schemeSpecificPart
        val responseBuilder = CallResponse.Builder()

        if (phoneNumber.isNullOrEmpty()) {
            if (muteUnknown) {
                responseBuilder.setSilenceCall(true)
                responseBuilder.setSkipCallLog(false)
                responseBuilder.setSkipNotification(true)
            }
        } else {
            // Optimization: Normalize only once
            val normalizedIncoming = phoneNumber.replace(Regex("[^0-9+]"), "")
            
            // Check if explicitly muted first (fastest check)
            var isExplicitlyMuted = false
            if (mutedContacts.isNotEmpty()) {
                for (contactEntry in mutedContacts) {
                    if (contactEntry.contains(normalizedIncoming)) {
                        isExplicitlyMuted = true
                        break
                    }
                }
            }

            if (isExplicitlyMuted) {
                responseBuilder.setSilenceCall(true)
                responseBuilder.setSkipNotification(true)
            } else if (muteUnknown) {
                // Only query the Contact provider if we actually intend to mute unknown callers
                if (!isNumberInContacts(this, phoneNumber)) {
                    responseBuilder.setSilenceCall(true)
                    responseBuilder.setSkipNotification(true)
                }
            }
        }

        respondToCall(callDetails, responseBuilder.build())
    }

    private fun isNumberInContacts(context: Context, phoneNumber: String): Boolean {
        return try {
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            
            context.contentResolver.query(
                lookupUri,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.e("UnknownCallerMuter", "Error querying contacts", e)
            false
        }
    }
}
