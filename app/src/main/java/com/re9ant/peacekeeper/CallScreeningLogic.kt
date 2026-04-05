package com.re9ant.peacekeeper

/**
 * Pure, framework-independent logic for deciding whether an incoming call
 * should be silenced. Extracted from [UnknownCallerScreeningService] so it
 * can be tested without any Android system services.
 */
object CallScreeningLogic {

    /**
     * The result of evaluating an incoming call against the user's preferences.
     */
    data class Decision(
        val silenceCall: Boolean,
        val skipNotification: Boolean
    )

    /**
     * Decides whether [incomingNumber] should be silenced.
     *
     * @param incomingNumber  Raw phone number from the call handle (may be null/blank for
     *                        hidden / private numbers).
     * @param muteUnknown     Whether "mute unknown callers" option is enabled.
     * @param mutedContacts   The set of contact entries stored in SharedPreferences, each in
     *                        the format "Display Name (normalized_number)".
     * @param isInContacts    A lambda that performs the actual ContentProvider lookup.
     *                        Receives the raw incoming number, returns true if found.
     *                        Kept as a parameter so tests can inject a fake without Robolectric.
     */
    fun evaluate(
        incomingNumber: String?,
        muteUnknown: Boolean,
        mutedContacts: Set<String>,
        isInContacts: (String) -> Boolean
    ): Decision {
        // Both options off → allow the call
        if (!muteUnknown && mutedContacts.isEmpty()) {
            return Decision(silenceCall = false, skipNotification = false)
        }

        // Hidden / private caller
        if (incomingNumber.isNullOrEmpty()) {
            return if (muteUnknown) {
                Decision(silenceCall = true, skipNotification = true)
            } else {
                Decision(silenceCall = false, skipNotification = false)
            }
        }

        val normalizedIncoming = normalize(incomingNumber)

        // Explicitly muted contact takes priority
        val isExplicitlyMuted = mutedContacts.any { it.contains(normalizedIncoming) }
        if (isExplicitlyMuted) {
            return Decision(silenceCall = true, skipNotification = true)
        }

        // Fall back to unknown-caller muting
        if (muteUnknown && !isInContacts(incomingNumber)) {
            return Decision(silenceCall = true, skipNotification = true)
        }

        return Decision(silenceCall = false, skipNotification = false)
    }

    /** Strips everything except digits and leading '+'. */
    fun normalize(number: String): String = number.replace(Regex("[^0-9+]"), "")

    /**
     * Builds the contact-list entry string that is stored in SharedPreferences
     * and displayed in the ListView.
     */
    fun buildContactEntry(displayName: String, normalizedNumber: String): String =
        "$displayName ($normalizedNumber)"

    /**
     * Parses a contact entry string back into its components.
     * Returns null if the format is unexpected.
     */
    fun parseContactEntry(entry: String): Pair<String, String>? {
        val openParen = entry.lastIndexOf('(')
        val closeParen = entry.lastIndexOf(')')
        if (openParen < 0 || closeParen <= openParen) return null
        val name = entry.substring(0, openParen).trim()
        val number = entry.substring(openParen + 1, closeParen)
        return Pair(name, number)
    }
}
