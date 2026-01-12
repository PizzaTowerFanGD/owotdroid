package com.owot.android.client.ui

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.owot.android.client.R
import com.owot.android.client.data.models.*
import com.owot.android.client.ui.adapter.ChatAdapter
import com.owot.android.client.viewmodel.WorldViewModel
import com.owot.android.client.viewmodel.WorldViewModelFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Main world activity with canvas rendering and interaction
 */
@AndroidEntryPoint
class WorldActivity : AppCompatActivity() {
    
    private lateinit var viewModel: WorldViewModel
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var chatInput: TextInputEditText
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var worldSurface: OWOTSurfaceView
    private lateinit var infoText: MaterialTextView
    private lateinit var connectionStatusFab: FloatingActionButton
    
    // Touch handling
    private var lastTouchPoint: Point? = null
    private var lastTouchTime: Long = 0
    private var isDragging = false
    private var dragDistanceThreshold = 10
    
    companion object {
        private const val EXTRA_WORLD_NAME = "world_name"
        
        fun start(context: Context, worldName: String) {
            val intent = Intent(context, WorldActivity::class.java).apply {
                putExtra(EXTRA_WORLD_NAME, worldName)
            }
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_world)
        
        val worldName = intent.getStringExtra(EXTRA_WORLD_NAME) ?: run {
            Toast.makeText(this, "No world name provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupViews()
        setupViewModel(worldName)
        setupChat()
        setupSurface()
        setupListeners()
        observeData()
    }
    
    private fun setupViews() {
        worldSurface = findViewById(R.id.surface_owot)
        chatInput = findViewById(R.id.input_chat)
        chatRecyclerView = findViewById(R.id.recycler_view_chat)
        infoText = findViewById(R.id.text_info)
        connectionStatusFab = findViewById(R.id.fab_connection_status)
    }
    
    private fun setupViewModel(worldName: String) {
        val factory = WorldViewModelFactory(worldName, application)
        viewModel = ViewModelProvider(this, factory)[WorldViewModel::class.java]
    }
    
    private fun setupChat() {
        chatAdapter = ChatAdapter()
        chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@WorldActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }
    
    private fun setupSurface() {
        worldSurface.setViewModel(viewModel)
        worldSurface.setOnTouchListener { _, event ->
            handleTouchEvent(event)
        }
    }
    
    private fun setupListeners() {
        // Chat input
        chatInput.setOnEditorActionListener { _, _, _ ->
            sendChatMessage()
            true
        }
        
        // Connection status FAB
        connectionStatusFab.setOnClickListener {
            if (viewModel.isConnected()) {
                viewModel.disconnect()
            } else {
                viewModel.connect()
            }
        }
        
        // FAB for additional actions (zoom, etc.)
        findViewById<FloatingActionButton>(R.id.fab_zoom_in).setOnClickListener {
            worldSurface.zoomIn()
        }
        
        findViewById<FloatingActionButton>(R.id.fab_zoom_out).setOnClickListener {
            worldSurface.zoomOut()
        }
        
        findViewById<FloatingActionButton>(R.id.fab_center).setOnClickListener {
            worldSurface.centerView()
        }
        
        // Handle system UI visibility
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            val fullscreen = (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) != 0
            if (!fullscreen) {
                // Auto-hide UI after delay when not in fullscreen
                worldSurface.hideUiAfterDelay()
            }
        }
    }
    
    private fun observeData() {
        viewModel.clientState.observe(this) { state ->
            updateConnectionStatus(state)
        }
        
        viewModel.worldInfo.observe(this) { worldInfo ->
            updateWorldInfo(worldInfo)
        }
        
        viewModel.chatMessages.observe(this) { messages ->
            chatAdapter.submitList(messages)
            chatRecyclerView.scrollToPosition(messages.size - 1)
        }
        
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
        
        viewModel.announcements.observe(this) { announcement ->
            announcement?.let {
                showAnnouncement(it)
                viewModel.clearAnnouncement()
            }
        }
    }
    
    private fun handleTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchPoint = Point(event.x.toInt(), event.y.toInt())
                lastTouchTime = System.currentTimeMillis()
                isDragging = false
            }
            
            MotionEvent.ACTION_MOVE -> {
                val currentPoint = Point(event.x.toInt(), event.y.toInt())
                val distance = lastTouchPoint?.let { 
                    kotlin.math.sqrt(
                        kotlin.math.pow((currentPoint.x - it.x).toDouble(), 2.0) +
                        kotlin.math.pow((currentPoint.y - it.y).toDouble(), 2.0)
                    )
                }?.toInt() ?: 0
                
                if (distance > dragDistanceThreshold) {
                    if (!isDragging) {
                        isDragging = true
                        worldSurface.onDragStart(currentPoint.x.toFloat(), currentPoint.y.toFloat())
                    } else {
                        worldSurface.onDrag(currentPoint.x.toFloat(), currentPoint.y.toFloat())
                    }
                }
            }
            
            MotionEvent.ACTION_UP -> {
                val currentTime = System.currentTimeMillis()
                val timeDiff = currentTime - lastTouchTime
                
                if (!isDragging && timeDiff < 200) {
                    // This was a tap
                    worldSurface.onTap(event.x, event.y)
                } else if (isDragging) {
                    worldSurface.onDragEnd(event.x, event.y)
                }
                
                lastTouchPoint = null
                isDragging = false
            }
        }
        
        return true
    }
    
    private fun sendChatMessage() {
        val message = chatInput.text?.toString()?.trim()
        if (message != null && message.isNotEmpty()) {
            viewModel.sendChatMessage(message)
            chatInput.text?.clear()
        }
    }
    
    private fun updateConnectionStatus(state: ClientState) {
        val statusText = when {
            state.isConnecting -> "Connecting..."
            state.isConnected -> "Connected (${state.userCount} users)"
            else -> "Disconnected"
        }
        
        infoText.text = "World: ${viewModel.worldName} | $statusText | Ping: ${state.ping}ms"
        
        // Update FAB icon and color
        connectionStatusFab.setImageResource(
            when {
                state.isConnected -> R.drawable.ic_connected
                state.isConnecting -> R.drawable.ic_connecting
                else -> R.drawable.ic_disconnected
            }
        )
        
        connectionStatusFab.setBackgroundTintList(
            when {
                state.isConnected -> android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.GREEN
                )
                state.isConnecting -> android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.YELLOW
                )
                else -> android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.RED
                )
            }
        )
    }
    
    private fun updateWorldInfo(worldInfo: WorldInfo?) {
        worldInfo?.let {
            // Update world-specific information
            supportActionBar?.title = "${viewModel.worldName} (${it.userCount} online)"
        }
    }
    
    private fun showAnnouncement(message: String) {
        Toast.makeText(this, "Announcement: $message", Toast.LENGTH_LONG).show()
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        worldSurface.cleanup()
    }
    
    override fun onBackPressed() {
        // Exit fullscreen mode first
        if (window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
            // Not in fullscreen, exit activity
            super.onBackPressed()
        } else {
            // Toggle fullscreen
            toggleFullscreen()
        }
    }
    
    private fun toggleFullscreen() {
        val window = window
        val decorView = window.decorView
        
        val uiFlags = if (decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
            // Enter fullscreen
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        } else {
            // Exit fullscreen
            View.SYSTEM_UI_FLAG_VISIBLE
        }
        
        decorView.systemUiVisibility = uiFlags
        
        // Also hide/show action bar
        if (uiFlags and View.SYSTEM_UI_FLAG_FULLSCREEN != 0) {
            supportActionBar?.hide()
        } else {
            supportActionBar?.show()
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        return handleTouchEvent(event)
    }
}