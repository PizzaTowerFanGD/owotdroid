package com.owot.android.client.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.owot.android.client.data.models.UserPreferences
import com.owot.android.client.util.OWOTUtils

/**
 * Manager for handling user preferences and settings
 */
class PreferenceManager(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        PreferenceManager.getDefaultSharedPreferences(context)
    
    companion object {
        private const val PREF_NICKNAME = "nickname"
        private const val PREF_TEXT_COLOR = "text_color"
        private const val PREF_BG_COLOR = "bg_color"
        private const val PREF_FONT_SIZE = "font_size"
        private const val PREF_SHOW_GRID = "show_grid"
        private const val PREF_SHOW_CURSORS = "show_cursors"
        private const val PREF_AUTO_SCROLL = "auto_scroll"
        private const val PREF_CHAT_ENABLED = "chat_enabled"
        private const val PREF_AUTO_CONNECT = "auto_connect"
        private const val PREF_ZOOM_LEVEL = "zoom_level"
        private const val PREF_LAST_WORLD = "last_world"
    }
    
    fun getUserPreferences(): UserPreferences {
        return UserPreferences(
            nickname = getNickname(),
            textColor = getTextColor(),
            bgColor = getBgColor(),
            showGrid = getShowGrid(),
            showCursors = getShowCursors(),
            autoScroll = getAutoScroll(),
            chatEnabled = getChatEnabled()
        )
    }
    
    fun saveUserPreferences(preferences: UserPreferences) {
        sharedPreferences.edit().apply {
            putString(PREF_NICKNAME, preferences.nickname)
            putInt(PREF_TEXT_COLOR, preferences.textColor)
            putInt(PREF_BG_COLOR, preferences.bgColor)
            putBoolean(PREF_SHOW_GRID, preferences.showGrid)
            putBoolean(PREF_SHOW_CURSORS, preferences.showCursors)
            putBoolean(PREF_AUTO_SCROLL, preferences.autoScroll)
            putBoolean(PREF_CHAT_ENABLED, preferences.chatEnabled)
            apply()
        }
    }
    
    fun getNickname(): String {
        return sharedPreferences.getString(PREF_NICKNAME, "Anonymous") ?: "Anonymous"
    }
    
    fun setNickname(nickname: String) {
        sharedPreferences.edit().putString(PREF_NICKNAME, nickname).apply()
    }
    
    fun getTextColor(): Int {
        return sharedPreferences.getInt(PREF_TEXT_COLOR, android.graphics.Color.BLACK)
    }
    
    fun setTextColor(color: Int) {
        sharedPreferences.edit().putInt(PREF_TEXT_COLOR, color).apply()
    }
    
    fun getBgColor(): Int {
        return sharedPreferences.getInt(PREF_BG_COLOR, -1)
    }
    
    fun setBgColor(color: Int) {
        sharedPreferences.edit().putInt(PREF_BG_COLOR, color).apply()
    }
    
    fun getFontSize(): Float {
        return sharedPreferences.getFloat(PREF_FONT_SIZE, 14f)
    }
    
    fun setFontSize(fontSize: Float) {
        sharedPreferences.edit().putFloat(PREF_FONT_SIZE, fontSize).apply()
    }
    
    fun getShowGrid(): Boolean {
        return sharedPreferences.getBoolean(PREF_SHOW_GRID, true)
    }
    
    fun setShowGrid(showGrid: Boolean) {
        sharedPreferences.edit().putBoolean(PREF_SHOW_GRID, showGrid).apply()
    }
    
    fun getShowCursors(): Boolean {
        return sharedPreferences.getBoolean(PREF_SHOW_CURSORS, true)
    }
    
    fun setShowCursors(showCursors: Boolean) {
        sharedPreferences.edit().putBoolean(PREF_SHOW_CURSORS, showCursors).apply()
    }
    
    fun getAutoScroll(): Boolean {
        return sharedPreferences.getBoolean(PREF_AUTO_SCROLL, true)
    }
    
    fun setAutoScroll(autoScroll: Boolean) {
        sharedPreferences.edit().putBoolean(PREF_AUTO_SCROLL, autoScroll).apply()
    }
    
    fun getChatEnabled(): Boolean {
        return sharedPreferences.getBoolean(PREF_CHAT_ENABLED, true)
    }
    
    fun setChatEnabled(chatEnabled: Boolean) {
        sharedPreferences.edit().putBoolean(PREF_CHAT_ENABLED, chatEnabled).apply()
    }
    
    fun getAutoConnect(): Boolean {
        return sharedPreferences.getBoolean(PREF_AUTO_CONNECT, false)
    }
    
    fun setAutoConnect(autoConnect: Boolean) {
        sharedPreferences.edit().putBoolean(PREF_AUTO_CONNECT, autoConnect).apply()
    }
    
    fun getZoomLevel(): Float {
        return sharedPreferences.getFloat(PREF_ZOOM_LEVEL, 1.0f)
    }
    
    fun setZoomLevel(zoomLevel: Float) {
        sharedPreferences.edit().putFloat(PREF_ZOOM_LEVEL, zoomLevel).apply()
    }
    
    fun getLastWorld(): String? {
        return sharedPreferences.getString(PREF_LAST_WORLD, null)
    }
    
    fun setLastWorld(worldName: String) {
        sharedPreferences.edit().putString(PREF_LAST_WORLD, worldName).apply()
    }
    
    fun clearLastWorld() {
        sharedPreferences.edit().remove(PREF_LAST_WORLD).apply()
    }
    
    fun resetToDefaults() {
        sharedPreferences.edit().clear().apply()
    }
}