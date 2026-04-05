package com.re9ant.peacekeeper

import org.junit.Assert.*
import org.junit.Test

/**
 * Local JVM unit tests for [CallScreeningLogic].
 *
 * These tests run on the JVM (no emulator / device needed) because
 * [CallScreeningLogic] has zero Android framework dependencies.
 *
 * Run with:
 *   ./gradlew :app:test
 */
class CallScreeningLogicTest {

    // ── Helper to keep tests readable ─────────────────────────────────────────

    private fun decide(
        number: String?,
        muteUnknown: Boolean,
        mutedContacts: Set<String> = emptySet(),
        inContacts: Boolean = false
    ) = CallScreeningLogic.evaluate(
        incomingNumber = number,
        muteUnknown = muteUnknown,
        mutedContacts = mutedContacts,
        isInContacts = { inContacts }
    )

    // ── normalize() ───────────────────────────────────────────────────────────

    @Test
    fun `normalize strips spaces dashes and parentheses`() {
        assertEquals("+14155550100", CallScreeningLogic.normalize("+1 (415) 555-0100"))
    }

    @Test
    fun `normalize keeps leading plus`() {
        assertEquals("+447911123456", CallScreeningLogic.normalize("+44 7911 123456"))
    }

    @Test
    fun `normalize strips all non-digit non-plus characters`() {
        assertEquals("1234567890", CallScreeningLogic.normalize("1.234.567.890"))
    }

    @Test
    fun `normalize on already clean number is idempotent`() {
        assertEquals("5550100", CallScreeningLogic.normalize("5550100"))
    }

    @Test
    fun `normalize on empty string returns empty`() {
        assertEquals("", CallScreeningLogic.normalize(""))
    }

    // ── buildContactEntry / parseContactEntry ────────────────────────────────

    @Test
    fun `buildContactEntry produces expected format`() {
        assertEquals(
            "Alice (+14155550100)",
            CallScreeningLogic.buildContactEntry("Alice", "+14155550100")
        )
    }

    @Test
    fun `parseContactEntry round-trips with buildContactEntry`() {
        val entry = CallScreeningLogic.buildContactEntry("Bob Smith", "+442012345678")
        val parsed = CallScreeningLogic.parseContactEntry(entry)
        assertNotNull(parsed)
        assertEquals("Bob Smith", parsed!!.first)
        assertEquals("+442012345678", parsed.second)
    }

    @Test
    fun `parseContactEntry returns null for malformed input`() {
        assertNull(CallScreeningLogic.parseContactEntry("No parentheses here"))
    }

    @Test
    fun `parseContactEntry handles name with parentheses`() {
        val entry = "Alice (CEO) (+14155550100)"
        val parsed = CallScreeningLogic.parseContactEntry(entry)
        assertNotNull(parsed)
        assertEquals("+14155550100", parsed!!.second)
    }

    // ── evaluate(): both options OFF ──────────────────────────────────────────

    @Test
    fun `both options OFF allows call`() {
        val d = decide(number = "+15551234567", muteUnknown = false, mutedContacts = emptySet())
        assertFalse(d.silenceCall)
        assertFalse(d.skipNotification)
    }

    @Test
    fun `both options OFF with null number allows call`() {
        val d = decide(number = null, muteUnknown = false, mutedContacts = emptySet())
        assertFalse(d.silenceCall)
    }

    // ── evaluate(): hidden / private number ───────────────────────────────────

    @Test
    fun `hidden number with muteUnknown ON silences call`() {
        val d = decide(number = null, muteUnknown = true)
        assertTrue(d.silenceCall)
        assertTrue(d.skipNotification)
    }

    @Test
    fun `empty string number with muteUnknown ON silences call`() {
        val d = decide(number = "", muteUnknown = true)
        assertTrue(d.silenceCall)
        assertTrue(d.skipNotification)
    }

    @Test
    fun `hidden number with muteUnknown OFF but muted contacts present allows call`() {
        val d = decide(
            number = null,
            muteUnknown = false,
            mutedContacts = setOf("Alice (+14155550100)")
        )
        assertFalse(d.silenceCall)
    }

    // ── evaluate(): explicitly muted contacts ─────────────────────────────────

    @Test
    fun `number matching muted contact silences regardless of muteUnknown`() {
        val mutedContacts = setOf("Alice (+14155550100)")
        val d = decide(
            number = "+1 (415) 555-0100",
            muteUnknown = false,
            mutedContacts = mutedContacts
        )
        assertTrue(d.silenceCall)
        assertTrue(d.skipNotification)
    }

    @Test
    fun `number matching muted contact when muteUnknown is also ON silences`() {
        val mutedContacts = setOf("Bob (+447911123456)")
        val d = decide(
            number = "+44 7911 123456",
            muteUnknown = true,
            mutedContacts = mutedContacts,
            inContacts = true
        )
        assertTrue(d.silenceCall)
    }

    @Test
    fun `number NOT matching any muted contact and NOT unknown muting allows`() {
        val mutedContacts = setOf("Alice (+14155550100)")
        val d = decide(
            number = "+19995550199",
            muteUnknown = false,
            mutedContacts = mutedContacts,
            inContacts = false
        )
        assertFalse(d.silenceCall)
    }

    @Test
    fun `multiple muted contacts one matches silences`() {
        val mutedContacts = setOf(
            "Alice (+14155550100)",
            "Bob (+447911123456)",
            "Charlie (+33123456789)"
        )
        val d = decide(
            number = "+44 7911 123456",
            muteUnknown = false,
            mutedContacts = mutedContacts
        )
        assertTrue(d.silenceCall)
    }

    // ── evaluate(): muteUnknown flow ─────────────────────────────────────────

    @Test
    fun `muteUnknown ON number not in contacts silences`() {
        val d = decide(
            number = "+19995551234",
            muteUnknown = true,
            mutedContacts = emptySet(),
            inContacts = false
        )
        assertTrue(d.silenceCall)
        assertTrue(d.skipNotification)
    }

    @Test
    fun `muteUnknown ON number IS in contacts allows`() {
        val d = decide(
            number = "+14155550100",
            muteUnknown = true,
            mutedContacts = emptySet(),
            inContacts = true
        )
        assertFalse(d.silenceCall)
    }

    @Test
    fun `muteUnknown OFF number not in contacts allows`() {
        val d = decide(
            number = "+19995551234",
            muteUnknown = false,
            mutedContacts = emptySet(),
            inContacts = false
        )
        assertFalse(d.silenceCall)
    }

    // ── edge cases ────────────────────────────────────────────────────────────

    @Test
    fun `number with special chars normalized before matching muted contact`() {
        val mutedContacts = setOf("Alice (+14155550100)")
        val d = decide(
            number = "+1 (415) 555-0100",
            muteUnknown = false,
            mutedContacts = mutedContacts
        )
        assertTrue("Normalized number should match stored entry", d.silenceCall)
    }

    @Test
    fun `isInContacts lambda not called when number already explicitly muted`() {
        var lambdaCalled = false
        CallScreeningLogic.evaluate(
            incomingNumber = "+14155550100",
            muteUnknown = true,
            mutedContacts = setOf("Alice (+14155550100)"),
            isInContacts = { lambdaCalled = true; false }
        )
        assertFalse("isInContacts should NOT be called when explicitly muted", lambdaCalled)
    }

    @Test
    fun `isInContacts lambda not called when both options are OFF`() {
        var lambdaCalled = false
        CallScreeningLogic.evaluate(
            incomingNumber = "+14155550100",
            muteUnknown = false,
            mutedContacts = emptySet(),
            isInContacts = { lambdaCalled = true; false }
        )
        assertFalse("isInContacts should NOT be called when both options are OFF", lambdaCalled)
    }

    @Test
    fun `isInContacts lambda not called when muteUnknown is OFF`() {
        var lambdaCalled = false
        CallScreeningLogic.evaluate(
            incomingNumber = "+19995551234",
            muteUnknown = false,
            mutedContacts = emptySet(),
            isInContacts = { lambdaCalled = true; false }
        )
        assertFalse("isInContacts should NOT be called when muteUnknown is OFF", lambdaCalled)
    }

    @Test
    fun `Decision data class equality works`() {
        val d1 = CallScreeningLogic.Decision(silenceCall = true, skipNotification = true)
        val d2 = CallScreeningLogic.Decision(silenceCall = true, skipNotification = true)
        assertEquals(d1, d2)
    }
}
