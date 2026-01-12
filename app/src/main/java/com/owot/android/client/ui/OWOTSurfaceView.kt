package com.owot.android.client.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.core.view.GestureDetectorCompat
import com.owot.android.client.data.models.*
import com.owot.android.client.rendering.OWOTRenderer
import com.owot.android.client.viewmodel.WorldViewModel
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * Custom SurfaceView for rendering the OWOT infinite text canvas
 */
class OWOTSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {
    
    companion object {
        private const val TAG = "OWOTSurfaceView"
        private const val RENDER_INTERVAL = 16L // ~60 FPS
        private const val ZOOM_STEP = 1.2f
        private const val MIN_ZOOM = 0.1f
        private const val MAX_ZOOM = 5.0f
    }
    
    private var viewModel: WorldViewModel? = null
    private var renderer: OWOTRenderer? = null
    private var renderJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Camera and interaction state
    private var positionX: Float = 0f
    private var positionY: Float = 0f
    private var zoom: Float = 1.0f
    private var isInitialized = false
    
    // Touch handling
    private var gestureDetector: GestureDetectorCompat
    private var lastTouchPoint: PointF? = null
    private var isDragging = false
    private var lastDragTime: Long = 0
    private var uiHideTimer: Timer? = null
    
    init {
        holder.addCallback(this)
        holder.setFormat(PixelFormat.RGBA_8888) // Enable transparency
        
        // Setup gesture detection
        gestureDetector = GestureDetectorCompat(context, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (e1 != null) {
                    onDrag(e2.x, e2.y, e1.x - e2.x, e1.y - e2.y)
                }
                return true
            }
            
            override fun onDoubleTap(e: MotionEvent): Boolean {
                onDoubleTapAt(e.x, e.y)
                return true
            }
            
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onTapAt(e.x, e.y)
                return true
            }
            
            override fun onLongPress(e: MotionEvent) {
                onLongPressAt(e.x, e.y)
            }
        })
        
        // Enable touch events
        isFocusable = true
        isFocusableInTouchMode = true
        keepScreenOn = true
    }
    
    fun setViewModel(viewModel: WorldViewModel) {
        this.viewModel = viewModel
        if (isInitialized) {
            setupRenderer()
        }
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "Surface created")
        setupRenderer()
        startRenderLoop()
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d(TAG, "Surface changed: ${width}x${height}")
        renderer?.let {
            // Update renderer dimensions if needed
        }
    }
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "Surface destroyed")
        stopRenderLoop()
    }
    
    /**
     * Setup the OWOT renderer
     */
    private fun setupRenderer() {
        val canvas = holder.lockCanvas()
        if (canvas != null) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.LEFT
            }
            val backgroundPaint = Paint()
            
            renderer = OWOTRenderer(canvas, paint, textPaint, backgroundPaint)
            isInitialized = true
            
            holder.unlockCanvasAndPost(canvas)
        }
    }
    
    /**
     * Start the render loop
     */
    private fun startRenderLoop() {
        renderJob?.cancel()
        renderJob = scope.launch {
            while (isActive) {
                renderFrame()
                delay(RENDER_INTERVAL)
            }
        }
    }
    
    /**
     * Stop the render loop
     */
    private fun stopRenderLoop() {
        renderJob?.cancel()
        renderJob = null
    }
    
    /**
     * Render a single frame
     */
    private suspend fun renderFrame() {
        val canvas = holder.lockCanvas() ?: return
        
        try {
            // Clear canvas
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            
            // Get current tiles from view model
            val tiles = viewModel?.getVisibleTiles(width, height) ?: emptyMap()
            val guestCursors = viewModel?.getGuestCursors()
            val localCursor = viewModel?.getLocalCursor()
            
            // Update renderer with current state
            renderer?.updatePosition(positionX, positionY)
            renderer?.updateZoom(zoom)
            
            // Render visible tiles
            renderer?.renderVisibleTiles(tiles, width, height, guestCursors, localCursor)
            
        } catch (e: Exception) {
            Log.e(TAG, "Render error", e)
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }
    
    /**
     * Handle touch events
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handled = gestureDetector.onTouchEvent(event)
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchPoint = PointF(event.x, event.y)
                isDragging = false
                resetUiHideTimer()
            }
            
            MotionEvent.ACTION_MOVE -> {
                // Gesture detector handles scrolling, but we can add additional logic here
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                lastTouchPoint = null
                isDragging = false
                resetUiHideTimer()
            }
        }
        
        return handled || super.onTouchEvent(event)
    }
    
    /**
     * Handle drag operations
     */
    private fun onDrag(currentX: Float, currentY: Float, deltaX: Float, deltaY: Float) {
        if (!isDragging) {
            isDragging = true
            hideUi()
        }
        
        positionX += deltaX
        positionY += deltaY
        
        // Clamp position to reasonable bounds
        positionX = positionX.coerceIn(-10000f, 10000f)
        positionY = positionY.coerceIn(-10000f, 10000f)
        
        // Update view model with boundary information
        viewModel?.updateCameraPosition(positionX, positionY, zoom)
        
        lastDragTime = System.currentTimeMillis()
    }
    
    /**
     * Handle tap events
     */
    private fun onTapAt(x: Float, y: Float) {
        viewModel?.let { vm ->
            renderer?.screenToWorld(x, y)?.let { coords ->
                vm.onTapAt(coords.tileX, coords.tileY, coords.charX, coords.charY)
            }
        }
    }
    
    /**
     * Handle double tap events
     */
    private fun onDoubleTapAt(x: Float, y: Float) {
        // Center view on tapped position
        renderer?.screenToWorld(x, y)?.let { coords ->
            centerOn(coords.tileX, coords.tileY, coords.charX, coords.charY)
        }
    }
    
    /**
     * Handle long press events
     */
    private fun onLongPressAt(x: Float, y: Float) {
        viewModel?.let { vm ->
            renderer?.screenToWorld(x, y)?.let { coords ->
                vm.onLongPressAt(coords.tileX, coords.tileY, coords.charX, coords.charY)
            }
        }
    }
    
    /**
     * Zoom in
     */
    fun zoomIn() {
        zoom = (zoom * ZOOM_STEP).coerceAtMost(MAX_ZOOM)
        viewModel?.updateCameraPosition(positionX, positionY, zoom)
        hideUi()
    }
    
    /**
     * Zoom out
     */
    fun zoomOut() {
        zoom = (zoom / ZOOM_STEP).coerceAtLeast(MIN_ZOOM)
        viewModel?.updateCameraPosition(positionX, positionY, zoom)
        hideUi()
    }
    
    /**
     * Center the view
     */
    fun centerView() {
        positionX = 0f
        positionY = 0f
        zoom = 1.0f
        viewModel?.updateCameraPosition(positionX, positionY, zoom)
        hideUi()
    }
    
    /**
     * Center view on specific coordinates
     */
    fun centerOn(tileX: Int, tileY: Int, charX: Int = 0, charY: Int = 0) {
        val screenX = (width / 2).toFloat()
        val screenY = (height / 2).toFloat()
        
        renderer?.let { renderer ->
            val cellW = renderer.getCellWidth()
            val cellH = renderer.getCellHeight()
            
            positionX = screenX - ((tileX * 16 + charX) * cellW)
            positionY = screenY - ((tileY * 8 + charY) * cellH)
            
            viewModel?.updateCameraPosition(positionX, positionY, zoom)
            hideUi()
        }
    }
    
    /**
     * Hide UI elements
     */
    private fun hideUi() {
        // This would hide floating action buttons and other UI elements
        // Implementation depends on parent activity's UI structure
        (context as? WorldActivity)?.let { activity ->
            // Hide UI elements
        }
    }
    
    /**
     * Reset UI hide timer
     */
    private fun resetUiHideTimer() {
        uiHideTimer?.cancel()
        uiHideTimer = Timer()
        uiHideTimer?.schedule(object : TimerTask() {
            override fun run() {
                // Show UI after delay
                scope.launch(Dispatchers.Main) {
                    showUi()
                }
            }
        }, 3000) // Hide UI after 3 seconds of inactivity
    }
    
    /**
     * Show UI elements
     */
    private fun showUi() {
        // This would show floating action buttons and other UI elements
        (context as? WorldActivity)?.let { activity ->
            // Show UI elements
        }
    }
    
    /**
     * Hide UI after delay
     */
    fun hideUiAfterDelay() {
        resetUiHideTimer()
    }
    
    /**
     * Handle external drag events (from activity)
     */
    fun onDragStart(x: Float, y: Float) {
        lastTouchPoint = PointF(x, y)
        isDragging = false
    }
    
    fun onDrag(x: Float, y: Float) {
        lastTouchPoint?.let { last ->
            val deltaX = last.x - x
            val deltaY = last.y - y
            onDrag(x, y, deltaX, deltaY)
            lastTouchPoint = PointF(x, y)
        }
    }
    
    fun onDragEnd(x: Float, y: Float) {
        lastTouchPoint = null
        isDragging = false
        resetUiHideTimer()
    }
    
    fun onTap(x: Float, y: Float) {
        onTapAt(x, y)
    }
    
    /**
     * Get current camera position
     */
    fun getCameraPosition(): PointF {
        return PointF(positionX, positionY)
    }
    
    /**
     * Get current zoom level
     */
    fun getZoom(): Float {
        return zoom
    }
    
    /**
     * Set camera position programmatically
     */
    fun setCameraPosition(x: Float, y: Float, newZoom: Float) {
        positionX = x
        positionY = y
        zoom = newZoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        viewModel?.updateCameraPosition(positionX, positionY, zoom)
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopRenderLoop()
        scope.cancel()
        renderer?.cleanup()
        uiHideTimer?.cancel()
    }
}