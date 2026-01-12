package com.owot.android.client.util

import android.graphics.Color
import android.util.Log
import java.util.*

/**
 * Utility functions for OWOT client
 */
object OWOTUtils {
    
    private const val TAG = "OWOTUtils"
    
    /**
     * Generate a unique edit ID for write operations
     */
    fun generateEditId(clientId: String? = null): String {
        val timestamp = System.currentTimeMillis()
        val random = Random().nextInt(10000)
        return "${clientId ?: "android"}_${timestamp}_$random"
    }
    
    /**
     * Parse timestamp from server
     */
    fun parseServerTimestamp(timestamp: String): Long {
        return try {
            // Server sends timestamps in various formats, try to parse them
            when {
                timestamp.isEmpty() -> System.currentTimeMillis()
                timestamp.matches(Regex("\\d+")) -> timestamp.toLong() // Unix timestamp
                else -> {
                    // Try ISO format
                    try {
                        val date = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(timestamp)
                        date?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse timestamp: $timestamp", e)
                        System.currentTimeMillis()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse timestamp: $timestamp", e)
            System.currentTimeMillis()
        }
    }
    
    /**
     * Format timestamp for display
     */
    fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            else -> "${diff / 86_400_000}d ago"
        }
    }
    
    /**
     * Convert RGB color to hex string
     */
    fun colorToHex(color: Int): String {
        return String.format("#%08X", color)
    }
    
    /**
     * Convert hex string to RGB color
     */
    fun hexToColor(hex: String): Int {
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse color: $hex", e)
            Color.BLACK
        }
    }
    
    /**
     * Check if a URL is safe to open
     */
    fun isSafeUrl(url: String): Boolean {
        val safeProtocols = listOf("http:", "https:", "ftp:", "ftps:")
        return safeProtocols.any { url.startsWith(it, ignoreCase = true) }
    }
    
    /**
     * Validate coordinate range
     */
    fun isValidCoordinate(x: Int, y: Int): Boolean {
        // OWOT coordinates can be very large, but we limit to reasonable bounds
        return kotlin.math.abs(x) <= 1000000 && kotlin.math.abs(y) <= 1000000
    }
    
    /**
     * Calculate distance between two coordinates
     */
    fun distanceBetween(x1: Int, y1: Int, x2: Int, y2: Int): Double {
        val dx = x2 - x1
        val dy = y2 - y1
        return kotlin.math.sqrt((dx * dx + dy * dy).toDouble())
    }
    
    /**
     * Clamp value to range
     */
    fun clamp(value: Float, min: Float, max: Float): Float {
        return kotlin.math.max(min, kotlin.math.min(max, value))
    }
    
    /**
     * Clamp value to range
     */
    fun clamp(value: Int, min: Int, max: Int): Int {
        return kotlin.math.max(min, kotlin.math.min(max, value))
    }
    
    /**
     * Check if character is whitespace
     */
    fun isWhitespace(char: Char): Boolean {
        return char.isWhitespace() || char.code == 0
    }
    
    /**
     * Check if character is printable
     */
    fun isPrintable(char: Char): Boolean {
        return char.code in 32..126 || char.code >= 160
    }
    
    /**
     * Sanitize text for display
     */
    fun sanitizeText(text: String): String {
        return text.filter { isPrintable(it) }
    }
    
    /**
     * Get tile key from coordinates
     */
    fun getTileKey(tileX: Int, tileY: Int): String {
        return "$tileX,$tileY"
    }
    
    /**
     * Parse tile key into coordinates
     */
    fun parseTileKey(tileKey: String): Pair<Int, Int>? {
        return try {
            val parts = tileKey.split(",")
            if (parts.size == 2) {
                val tileX = parts[0].toInt()
                val tileY = parts[1].toInt()
                Pair(tileX, tileY)
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse tile key: $tileKey", e)
            null
        }
    }
    
    /**
     * Get character index in tile from coordinates
     */
    fun getCharIndex(x: Int, y: Int, tileWidth: Int = 16): Int {
        return y * tileWidth + x
    }
    
    /**
     * Get coordinates from character index in tile
     */
    fun getCharCoordinates(index: Int, tileWidth: Int = 16): Pair<Int, Int> {
        val x = index % tileWidth
        val y = index / tileWidth
        return Pair(x, y)
    }
    
    /**
     * Check if character position is within tile bounds
     */
    fun isValidTilePosition(x: Int, y: Int, tileWidth: Int = 16, tileHeight: Int = 8): Boolean {
        return x in 0 until tileWidth && y in 0 until tileHeight
    }
    
    /**
     * Convert world coordinates to tile coordinates
     */
    fun worldToTile(worldX: Int, worldY: Int, tileWidth: Int = 16, tileHeight: Int = 8): Pair<Int, Int> {
        val tileX = kotlin.math.floorDiv(worldX, tileWidth)
        val tileY = kotlin.math.floorDiv(worldY, tileHeight)
        return Pair(tileX, tileY)
    }
    
    /**
     * Convert tile coordinates to world coordinates
     */
    fun tileToWorld(tileX: Int, tileY: Int, charX: Int = 0, charY: Int = 0, tileWidth: Int = 16, tileHeight: Int = 8): Pair<Int, Int> {
        val worldX = tileX * tileWidth + charX
        val worldY = tileY * tileHeight + charY
        return Pair(worldX, worldY)
    }
}