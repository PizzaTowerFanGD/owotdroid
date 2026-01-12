package com.owot.android.client.test

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import com.owot.android.client.ui.MainActivity
import com.owot.android.client.ui.WorldActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UITest {
    
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Test
    fun testMainActivityLaunch() {
        // Test that main activity launches properly
        onView(withId(R.id.recycler_view_worlds))
            .check(matches(isDisplayed()))
            
        onView(withId(R.id.fab_add_world))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testAddWorldFlow() {
        // Test adding a new world
        onView(withId(R.id.fab_add_world))
            .perform(click())
            
        onView(withId(R.id.layout_world_name))
            .check(matches(isDisplayed()))
            
        onView(withId(R.id.input_world_name))
            .perform(typeText("testworld"))
            
        onView(withId(R.id.button_connect))
            .perform(click())
            
        // Should navigate to WorldActivity
        onView(withId(R.id.surface_owot))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testWorldConnection() {
        // Test connection to a world
        onView(withId(R.id.input_world_name))
            .perform(typeText("test"))
            
        onView(withId(R.id.button_connect))
            .perform(click())
            
        // Verify connection status
        onView(withId(R.id.text_info))
            .check(matches(withText(containsString("World: test"))))
    }
    
    @Test
    fun testChatInterface() {
        // Test chat functionality
        onView(withId(R.id.input_world_name))
            .perform(typeText("test"))
            
        onView(withId(R.id.button_connect))
            .perform(click())
            
        // Test chat input
        onView(withId(R.id.input_chat))
            .perform(typeText("Hello World"))
            
        onView(withId(R.id.button_send_chat))
            .perform(click())
            
        // Verify chat message appears
        onView(withText("Hello World"))
            .check(matches(isDisplayed()))
    }
}