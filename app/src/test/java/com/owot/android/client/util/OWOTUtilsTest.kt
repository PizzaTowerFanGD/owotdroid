package com.owot.android.client.util

import org.junit.Test
import org.junit.Assert.*
import android.graphics.Color

class OWOTUtilsTest {
    
    @Test
    fun testGenerateEditId() {
        val editId1 = OWOTUtils.generateEditId()
        val editId2 = OWOTUtils.generateEditId()
        
        assertNotEquals(editId1, editId2)
        assertTrue(editId1.contains("android_"))
        assertTrue(editId2.contains("android_"))
    }
    
    @Test
    fun testParseServerTimestamp() {
        val timestamp1 = "1609459200000" // Unix timestamp
        val timestamp2 = "2021-01-01T00:00:00" // ISO format
        val timestamp3 = "" // Empty timestamp
        
        val result1 = OWOTUtils.parseServerTimestamp(timestamp1)
        val result2 = OWOTUtils.parseServerTimestamp(timestamp2)
        val result3 = OWOTUtils.parseServerTimestamp(timestamp3)
        
        assertTrue(result1 > 0)
        assertTrue(result2 > 0)
        assertTrue(result3 > 0)
    }
    
    @Test
    fun testColorToHexAndHexToColor() {
        val originalColor = Color.rgb(255, 128, 0)
        val hex = OWOTUtils.colorToHex(originalColor)
        val convertedColor = OWOTUtils.hexToColor(hex)
        
        assertEquals(originalColor, convertedColor)
        assertTrue(hex.startsWith("#"))
        assertEquals(8, hex.length)
    }
    
    @Test
    fun testIsValidCoordinate() {
        assertTrue(OWOTUtils.isValidCoordinate(0, 0))
        assertTrue(OWOTUtils.isValidCoordinate(100, -200))
        assertTrue(OWOTUtils.isValidCoordinate(999999, 999999))
        assertFalse(OWOTUtils.isValidCoordinate(2000000, 0)) // Too large
        assertFalse(OWOTUtils.isValidCoordinate(0, -2000000)) // Too large
    }
    
    @Test
    fun testClamp() {
        assertEquals(5f, OWOTUtils.clamp(5f, 0f, 10f))
        assertEquals(0f, OWOTUtils.clamp(-5f, 0f, 10f))
        assertEquals(10f, OWOTUtils.clamp(15f, 0f, 10f))
        
        assertEquals(5, OWOTUtils.clamp(5, 0, 10))
        assertEquals(0, OWOTUtils.clamp(-5, 0, 10))
        assertEquals(10, OWOTUtils.clamp(15, 0, 10))
    }
    
    @Test
    fun testTileKeyOperations() {
        val tileX = 10
        val tileY = -5
        val tileKey = OWOTUtils.getTileKey(tileX, tileY)
        
        assertEquals("10,-5", tileKey)
        
        val (parsedX, parsedY) = OWOTUtils.parseTileKey(tileKey)!!
        assertEquals(tileX, parsedX)
        assertEquals(tileY, parsedY)
    }
    
    @Test
    fun testIsPrintable() {
        assertTrue(OWOTUtils.isPrintable('A'))
        assertTrue(OWOTUtils.isPrintable(' ')) // Space
        assertTrue(OWOTUtils.isPrintable('ñ')) // Extended ASCII
        assertFalse(OWOTUtils.isPrintable('\u0000')) // Null character
        assertFalse(OWOTUtils.isPrintable('\u001F')) // Control character
    }
    
    @Test
    fun testSanitizeText() {
        val dirtyText = "Hello\u0000World\u001F!"
        val cleanText = OWOTUtils.sanitizeText(dirtyText)
        
        assertEquals("HelloWorld!", cleanText)
    }
}