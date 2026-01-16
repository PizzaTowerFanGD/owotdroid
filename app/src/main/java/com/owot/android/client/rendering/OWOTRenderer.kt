package com.owot.android.client.rendering

import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.Log
import com.owot.android.client.data.models.*
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * OWOT rendering engine for drawing the infinite text canvas
 */
class OWOTRenderer(
    private val paint: Paint,
    private val textPaint: Paint,
    private val backgroundPaint: Paint
) {
    
    companion object {
        private const val TAG = "OWOTRenderer"
        private const val TILE_CACHE_SIZE = 100 // Maximum tiles to cache
        private const val RENDER_QUEUE_SIZE = 50 // Maximum tiles to queue for rendering
    }
    
    // Rendering state
    private var positionX: Float = 0f
    private var positionY: Float = 0f
    private var zoom: Float = 1.0f
    private var cellWidth: Float = 12f
    private var cellHeight: Float = 16f
    private var tileWidth: Float = cellWidth * Tile.TILE_WIDTH
    private var tileHeight: Float = cellHeight * Tile.TILE_HEIGHT
    
    // Font settings
    private var fontFamily: String = "monospace"
    private var fontSize: Float = 14f
    
    // Tile cache for performance
    private val tileCache = ConcurrentHashMap<String, Bitmap>()
    private val dirtyTiles = ConcurrentHashMap<String, Boolean>()
    
    // Rendering queue
    private val renderQueue = mutableListOf<String>()
    private val isRendering = AtomicBoolean(false)
    
    // Colors
    private var backgroundColor: Int = Color.WHITE
    private var textColor: Int = Color.BLACK
    private var gridColor: Int = Color.GRAY
    private var cursorColor: Int = Color.BLUE
    private var linkColor: Int = Color.BLUE
    
    // Character decorations rendering
    private val decorationPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    init {
        setupPaint()
    }
    
    /**
     * Setup paint objects with proper styling
     */
    private fun setupPaint() {
        // Main paint for text
        paint.apply {
            isAntiAlias = true
            isDither = true
            color = textColor
        }
        
        // Text paint for character rendering
        textPaint.apply {
            isAntiAlias = true
            isDither = true
            color = textColor
            textAlign = Paint.Align.LEFT
        }
        
        // Background paint
        backgroundPaint.apply {
            isAntiAlias = true
            isDither = true
            color = backgroundColor
        }
        
        // Decoration paint for text decorations
        decorationPaint.apply {
            isAntiAlias = true
            isDither = true
        }
    }
    
    /**
     * Update font settings
     */
    fun updateFont(fontFamily: String, fontSize: Float) {
        this.fontFamily = fontFamily
        this.fontSize = fontSize
        
        textPaint.apply {
            typeface = Typeface.create(fontFamily, Typeface.NORMAL)
            textSize = fontSize * zoom
        }
        
        // Update cell dimensions based on font
        val fontMetrics = textPaint.fontMetrics
        cellHeight = (fontMetrics.bottom - fontMetrics.top) * 1.2f // Add some padding
        cellWidth = textPaint.measureText("M") * 1.1f // Approximate character width
        tileWidth = cellWidth * Tile.TILE_WIDTH
        tileHeight = cellHeight * Tile.TILE_HEIGHT
        
        // Invalidate all tiles when font changes
        clearCache()
    }
    
    /**
     * Update zoom level and related dimensions
     */
    fun updateZoom(newZoom: Float) {
        zoom = newZoom.coerceIn(0.1f, 5.0f) // Clamp zoom between 0.1x and 5x
        
        textPaint.textSize = fontSize * zoom
        
        val fontMetrics = textPaint.fontMetrics
        cellHeight = (fontMetrics.bottom - fontMetrics.top) * 1.2f
        cellWidth = textPaint.measureText("M") * 1.1f
        tileWidth = cellWidth * Tile.TILE_WIDTH
        tileHeight = cellHeight * Tile.TILE_HEIGHT
        
        // Invalidate all tiles when zoom changes
        clearCache()
    }
    
    /**
     * Update color scheme
     */
    fun updateColors(
        backgroundColor: Int,
        textColor: Int,
        gridColor: Int,
        cursorColor: Int,
        linkColor: Int
    ) {
        this.backgroundColor = backgroundColor
        this.textColor = textColor
        this.gridColor = gridColor
        this.cursorColor = cursorColor
        this.linkColor = linkColor
        
        paint.color = textColor
        textPaint.color = textColor
        backgroundPaint.color = backgroundColor
        
        clearCache()
    }
    
    /**
     * Update camera position
     */
    fun updatePosition(x: Float, y: Float) {
        positionX = x
        positionY = y
    }
    
    /**
     * Get visible tile range
     */
    fun getVisibleTileRange(width: Int, height: Int): TileRange {
        val minTileX = ((-positionX) / tileWidth).toInt() - 1
        val maxTileX = (((-positionX + width) / tileWidth).toInt()) + 1
        val minTileY = ((-positionY) / tileHeight).toInt() - 1
        val maxTileY = (((-positionY + height) / tileHeight).toInt()) + 1
        
        return TileRange(minTileX, maxTileX, minTileY, maxTileY)
    }
    
    /**
     * Render visible tiles
     */
    fun renderVisibleTiles(
        canvas: Canvas,
        tiles: Map<String, Tile>,
        width: Int,
        height: Int,
        guestCursors: Map<String, CursorPosition>? = null,
        localCursor: CursorPosition? = null
    ) {
        if (isRendering.get()) {
            return // Skip if already rendering
        }
        
        canvas.drawColor(backgroundColor)
        
        val tileRange = getVisibleTileRange(width, height)
        
        // Add visible tiles to render queue
        for (tileY in tileRange.minY..tileRange.maxY) {
            for (tileX in tileRange.minX..tileRange.maxX) {
                val tileKey = "$tileX,$tileY"
                if (!dirtyTiles.containsKey(tileKey)) {
                    renderQueue.add(tileKey)
                }
            }
        }
        
        // Process render queue
        processRenderQueue(canvas, tiles, guestCursors, localCursor)
    }
    
    /**
     * Process the render queue
     */
    private fun processRenderQueue(
        canvas: Canvas,
        tiles: Map<String, Tile>,
        guestCursors: Map<String, CursorPosition>?,
        localCursor: CursorPosition?
    ) {
        isRendering.set(true)
        
        try {
            var renderedTiles = 0
            val iterator = renderQueue.iterator()
            
            while (iterator.hasNext() && renderedTiles < RENDER_QUEUE_SIZE) {
                val tileKey = iterator.next()
                iterator.remove()
                
                val tile = tiles[tileKey]
                if (tile != null) {
                    renderTile(canvas, tile, guestCursors, localCursor)
                    renderedTiles++
                }
                
                dirtyTiles.remove(tileKey)
            }
            
            // Process remaining tiles in next frame
            if (renderQueue.isNotEmpty()) {
                // Schedule next render cycle
                requestRender()
            }
            
        } finally {
            isRendering.set(false)
        }
    }
    
    /**
     * Render a single tile
     */
    private fun renderTile(
        canvas: Canvas,
        tile: Tile,
        guestCursors: Map<String, CursorPosition>?,
        localCursor: CursorPosition?
    ) {
        val tileX = tile.tileX
        val tileY = tile.tileY
        
        // Calculate screen position
        val screenX = positionX + (tileX * tileWidth)
        val screenY = positionY + (tileY * tileHeight)
        
        // Check if tile is visible
        if (screenX + tileWidth < 0 || screenX > canvas.width ||
            screenY + tileHeight < 0 || screenY > canvas.height) {
            return
        }
        
        // Create or get cached tile bitmap
        val tileBitmap = getOrCreateTileBitmap(tileKey = "$tileX,$tileY")
        
        // Render tile content to bitmap
        renderTileContent(tile, tileBitmap)
        
        // Draw tile to main canvas
        canvas.drawBitmap(
            tileBitmap,
            screenX,
            screenY,
            paint
        )
        
        // Draw guest cursors on this tile
        guestCursors?.let { cursors ->
            drawGuestCursors(canvas, cursors, tileX, tileY, screenX, screenY)
        }
        
        // Draw local cursor if on this tile
        localCursor?.let { cursor ->
            if (cursor.tileX == tileX && cursor.tileY == tileY) {
                drawLocalCursor(canvas, cursor, screenX, screenY)
            }
        }
    }
    
    /**
     * Render tile content to bitmap
     */
    private fun renderTileContent(tile: Tile, bitmap: Bitmap) {
        val tileCanvas = Canvas(bitmap)
        tileCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        
        // Draw background
        tileCanvas.drawRect(0f, 0f, tileWidth, tileHeight, backgroundPaint)
        
        // Draw each character
        for (charY in 0 until Tile.TILE_HEIGHT) {
            for (charX in 0 until Tile.TILE_WIDTH) {
                val charIndex = charY * Tile.TILE_WIDTH + charX
                val charStr = tile.content[charIndex]
                
                // Draw background color for this cell first (behind text)
                val bgColor = tile.properties.bgColor[charIndex]
                if (bgColor != -1) {
                    val bgPaint = Paint().apply {
                        color = bgColor
                        style = Paint.Style.FILL
                    }
                    tileCanvas.drawRect(
                        charX * cellWidth,
                        charY * cellHeight,
                        (charX + 1) * cellWidth,
                        (charY + 1) * cellHeight,
                        bgPaint
                    )
                }
                
                // Decode character and decorations
                val decodedChar = TextDecorations.decode(charStr)
                
                if (decodedChar.char != ' ') {
                    val screenX = charX * this.cellWidth
                    val screenY = charY * this.cellHeight + textPaint.fontMetrics.ascent
                    
                    // Set character color
                    val color = tile.properties.color[charIndex]
                    if (color != Color.BLACK) {
                        textPaint.color = color
                    } else {
                        textPaint.color = textColor
                    }
                    
                    // Apply text decorations
                    val originalTypeface = textPaint.typeface
                    if (decodedChar.bold || decodedChar.italic) {
                        val style = when {
                            decodedChar.bold && decodedChar.italic -> Typeface.BOLD_ITALIC
                            decodedChar.bold -> Typeface.BOLD
                            decodedChar.italic -> Typeface.ITALIC
                            else -> Typeface.NORMAL
                        }
                        textPaint.typeface = Typeface.create(fontFamily, style)
                    }
                    
                    // Draw character
                    tileCanvas.drawText(
                        decodedChar.char.toString(),
                        screenX,
                        screenY,
                        textPaint
                    )
                    
                    // Restore typeface
                    textPaint.typeface = originalTypeface
                    
                    // Draw underline
                    if (decodedChar.underline) {
                        val underlineY = charY * cellHeight + cellHeight - 2f
                        decorationPaint.color = textPaint.color
                        decorationPaint.strokeWidth = 1f
                        tileCanvas.drawLine(
                            screenX,
                            underlineY,
                            screenX + cellWidth,
                            underlineY,
                            decorationPaint
                        )
                    }
                    
                    // Draw strikethrough
                    if (decodedChar.strikethrough) {
                        val strikeY = charY * cellHeight + cellHeight / 2f
                        decorationPaint.color = textPaint.color
                        decorationPaint.strokeWidth = 1f
                        tileCanvas.drawLine(
                            screenX,
                            strikeY,
                            screenX + cellWidth,
                            strikeY,
                            decorationPaint
                        )
                    }
                }
            }
        }
        
        // Draw grid lines
        drawGridLines(tileCanvas, tileWidth, tileHeight)
    }
    
    /**
     * Legacy method - text decorations are now handled inline in renderTileContent
     */
    @Deprecated("Text decorations are now handled inline")
    private fun handleTextDecorations(
        tile: Tile,
        charIndex: Int,
        x: Float,
        y: Float,
        tileCanvas: Canvas
    ) {
        // No longer used - decorations are decoded and rendered inline
    }
    
    /**
     * Draw grid lines for the tile
     */
    private fun drawGridLines(tileCanvas: Canvas, width: Float, height: Float) {
        val gridPaint = Paint().apply {
            color = gridColor
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            alpha = 50 // Make grid lines subtle
        }
        
        // Draw vertical lines
        for (x in 1 until Tile.TILE_WIDTH) {
            val lineX = x * cellWidth
            tileCanvas.drawLine(lineX, 0f, lineX, height, gridPaint)
        }
        
        // Draw horizontal lines
        for (y in 1 until Tile.TILE_HEIGHT) {
            val lineY = y * cellHeight
            tileCanvas.drawLine(0f, lineY, width, lineY, gridPaint)
        }
    }
    
    /**
     * Draw guest cursors
     */
    private fun drawGuestCursors(
        canvas: Canvas,
        cursors: Map<String, CursorPosition>,
        tileX: Int,
        tileY: Int,
        screenX: Float,
        screenY: Float
    ) {
        cursors.forEach { (_, cursor) ->
            if (cursor.tileX == tileX && cursor.tileY == tileY) {
                val cursorPaint = Paint().apply {
                    color = Color.GREEN // Different color for guest cursors
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                
                val x = screenX + (cursor.charX * cellWidth)
                val y = screenY + (cursor.charY * cellHeight)
                
                // Draw cursor rectangle
                canvas.drawRect(
                    x, y,
                    x + cellWidth, y + cellHeight,
                    cursorPaint
                )
            }
        }
    }
    
    /**
     * Draw local cursor
     */
    private fun drawLocalCursor(
        canvas: Canvas,
        cursor: CursorPosition,
        screenX: Float,
        screenY: Float
    ) {
        val x = screenX + (cursor.charX * cellWidth)
        val y = screenY + (cursor.charY * cellHeight)
        
        // Draw blinking cursor
        val cursorPaint = Paint().apply {
            color = cursorColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        
        canvas.drawRect(
            x, y,
            x + cellWidth, y + cellHeight,
            cursorPaint
        )
    }
    
    /**
     * Get or create cached tile bitmap
     */
    private fun getOrCreateTileBitmap(tileKey: String): Bitmap {
        return tileCache[tileKey] ?: run {
            val bitmap = Bitmap.createBitmap(
                tileWidth.toInt(),
                tileHeight.toInt(),
                Bitmap.Config.ARGB_8888
            )
            tileCache[tileKey] = bitmap
            bitmap
        }
    }
    
    /**
     * Mark tile as dirty for re-rendering
     */
    fun markTileDirty(tileX: Int, tileY: Int) {
        val tileKey = "$tileX,$tileY"
        dirtyTiles[tileKey] = true
        
        // Remove from cache to force re-render
        tileCache.remove(tileKey)
        
        // Add to render queue
        if (!renderQueue.contains(tileKey)) {
            renderQueue.add(tileKey)
        }
    }
    
    /**
     * Mark all tiles as dirty
     */
    fun markAllTilesDirty() {
        dirtyTiles.clear()
        renderQueue.clear()
        tileCache.clear()
    }
    
    /**
     * Clear the tile cache
     */
    fun clearCache() {
        tileCache.clear()
        dirtyTiles.clear()
        renderQueue.clear()
    }
    
    /**
     * Request a render cycle
     */
    private fun requestRender() {
        // This would be called from the main thread
        // In a real implementation, this would trigger a redraw
    }
    
    /**
     * Screen to world coordinates
     */
    fun screenToWorld(screenX: Float, screenY: Float): WorldCoordinates {
        val worldX = (screenX - positionX) / tileWidth
        val worldY = (screenY - positionY) / tileHeight
        
        val tileX = kotlin.math.floor(worldX).toInt()
        val tileY = kotlin.math.floor(worldY).toInt()
        val charX = kotlin.math.floor((worldX - tileX) * Tile.TILE_WIDTH).toInt()
        val charY = kotlin.math.floor((worldY - tileY) * Tile.TILE_HEIGHT).toInt()
        
        return WorldCoordinates(tileX, tileY, charX, charY)
    }
    
    /**
     * Get current cell dimensions
     */
    fun getCellWidth(): Float = cellWidth
    fun getCellHeight(): Float = cellHeight
    
    /**
     * World to screen coordinates
     */
    fun worldToScreen(tileX: Int, tileY: Int, charX: Int, charY: Int): ScreenCoordinates {
        val screenX = positionX + (tileX * tileWidth) + (charX * cellWidth)
        val screenY = positionY + (tileY * tileHeight) + (charY * cellHeight)
        
        return ScreenCoordinates(screenX, screenY)
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        tileCache.values.forEach { it.recycle() }
        tileCache.clear()
        renderQueue.clear()
        dirtyTiles.clear()
    }
}

/**
 * Data classes for coordinates
 */
data class TileRange(
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int
)

data class WorldCoordinates(
    val tileX: Int,
    val tileY: Int,
    val charX: Int,
    val charY: Int
)

data class ScreenCoordinates(
    val x: Float,
    val y: Float
)