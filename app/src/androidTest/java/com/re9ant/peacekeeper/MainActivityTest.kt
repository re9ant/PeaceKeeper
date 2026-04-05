package com.re9ant.peacekeeper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var prefs: android.content.SharedPreferences

    @Before
    fun setUp() {
        prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("muter_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @After
    fun tearDown() {
        prefs.edit().clear().commit()
    }

    @Test
    fun statusTextView_isDisplayed() {
        onView(withId(R.id.tvStatus)).check(matches(isDisplayed()))
    }

    @Test
    fun setupButton_isDisplayed() {
        onView(withId(R.id.btnSetup)).check(matches(isDisplayed()))
    }

    @Test
    fun muteUnknownCheckbox_isDisplayed() {
        onView(withId(R.id.cbMuteUnknown)).check(matches(isDisplayed()))
    }

    @Test
    fun addContactButton_isDisplayed() {
        onView(withId(R.id.btnAddContact)).check(matches(isDisplayed()))
    }

    @Test
    fun mutedContactsListView_isDisplayed() {
        onView(withId(R.id.lvMutedContacts)).check(matches(isDisplayed()))
    }

    @Test
    fun muteUnknownCheckbox_defaultsToUnchecked_whenPrefsEmpty() {
        onView(withId(R.id.cbMuteUnknown)).check(matches(isNotChecked()))
    }

    @Test
    fun muteUnknownCheckbox_restoresCheckedState_fromPrefs() {
        prefs.edit().putBoolean("mute_unknown", true).commit()
        activityRule.scenario.recreate()
        onView(withId(R.id.cbMuteUnknown)).check(matches(isChecked()))
    }

    @Test
    fun clickingCheckbox_togglesState() {
        onView(withId(R.id.cbMuteUnknown)).check(matches(isNotChecked()))
        onView(withId(R.id.cbMuteUnknown)).perform(click())
        onView(withId(R.id.cbMuteUnknown)).check(matches(isChecked()))
        onView(withId(R.id.cbMuteUnknown)).perform(click())
        onView(withId(R.id.cbMuteUnknown)).check(matches(isNotChecked()))
    }

    @Test
    fun clickingCheckbox_savesStateToPrefs() {
        onView(withId(R.id.cbMuteUnknown)).perform(click())
        val saved = prefs.getBoolean("mute_unknown", false)
        assert(saved) { "Expected pref 'mute_unknown' to be true after checking the checkbox" }
    }

    @Test
    fun uncheckingCheckbox_savesFalseToPrefs() {
        prefs.edit().putBoolean("mute_unknown", true).commit()
        activityRule.scenario.recreate()
        onView(withId(R.id.cbMuteUnknown)).perform(click())
        val saved = prefs.getBoolean("mute_unknown", true)
        assert(!saved) { "Expected pref 'mute_unknown' to be false after unchecking" }
    }

    @Test
    fun statusText_showsPermissionsRequired_initially() {
        onView(withId(R.id.tvStatus))
            .check(matches(isDisplayed()))
            .check(matches(withText("Permissions Required")))
    }

    @Test
    fun setupButton_isEnabled_whenPermissionsNotGranted() {
        onView(withId(R.id.btnSetup)).check(matches(isEnabled()))
    }

    @Test
    fun clickingSetupButton_doesNotCrash() {
        onView(withId(R.id.btnSetup)).perform(click())
        onView(withId(R.id.tvStatus)).check(matches(isDisplayed()))
    }
}
