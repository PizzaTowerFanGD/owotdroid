package com.owot.android.client.data.models

/**
 * Data class for world list items displayed in the main activity
 */
data class WorldListItem(
    val name: String,
    val title: String,
    val description: String,
    val userCount: Int,
    val lastVisited: Long
)

/**
 * Data class for world information
 */
data class WorldInfo(
    val name: String,
    val title: String,
    val description: String,
    val userCount: Int,
    val isMember: Boolean,
    val canWrite: Boolean,
    val creationDate: Long? = null,
    val lastActivity: Long? = null
)

/**
 * User preferences for the client
 */
data class UserPreferences(
    val nickname: String,
    val textColor: Int,
    val bgColor: Int,
    val showGrid: Boolean,
    val showCursors: Boolean,
    val autoScroll: Boolean,
    val chatEnabled: Boolean
)