package com.re9ant.peacekeeper

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

        val handle: Uri? = callDetails.handle
        val phoneNumber = handle?.schemeSpecificPart

        val decision = CallScreeningLogic.evaluate(
            incomingNumber = phoneNumber,
            muteUnknown = muteUnknown,
            mutedContacts = mutedContacts,
            isInContacts = { number -> isNumberInContacts(this, number) }
        )

        val responseBuilder = CallResponse.Builder()
        if (decision.silenceCall) {
            responseBuilder.setSilenceCall(true)
            responseBuilder.setSkipCallLog(false)
            responseBuilder.setSkipNotification(decision.skipNotification)
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
            Log.e("PeaceKeeper", "Error querying contacts", e)
            false
        }
    }
}
