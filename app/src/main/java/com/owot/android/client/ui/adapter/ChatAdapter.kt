package com.owot.android.client.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.owot.android.client.data.models.ChatResponse
import com.owot.android.client.databinding.ItemChatMessageBinding

/**
 * RecyclerView adapter for displaying chat messages
 */
class ChatAdapter : ListAdapter<ChatResponse, ChatMessageViewHolder>(ChatDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatMessageViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatMessageViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ChatMessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ChatMessageViewHolder(
    private val binding: ItemChatMessageBinding
) : RecyclerView.ViewHolder(binding.root) {
    
    fun bind(message: ChatResponse) {
        // Set username with special styling for admins/staff
        val username = if (message.isAdmin) {
            "[Admin] ${message.nickname}"
        } else if (message.isOp) {
            "[Op] ${message.nickname}"
        } else if (message.isStaff) {
            "[Staff] ${message.nickname}"
        } else {
            message.nickname
        }
        
        binding.textUsername.text = username
        
        // Set message text
        binding.textMessage.text = message.message
        
        // Set color if specified
        message.color?.let { color ->
            binding.textMessage.setTextColor(color)
        }
        
        // Set timestamp
        binding.textTimestamp.text = formatTimestamp(message.date)
        
        // Show/hide admin indicators
        binding.imageAdminBadge.visibility = if (message.isAdmin) android.view.View.VISIBLE else android.view.View.GONE
        binding.imageOpBadge.visibility = if (message.isOp) android.view.View.VISIBLE else android.view.View.GONE
        binding.imageStaffBadge.visibility = if (message.isStaff) android.view.View.VISIBLE else android.view.View.GONE
        
        // Set message type styling
        when (message.location) {
            com.owot.android.client.data.models.ChatLocation.GLOBAL -> {
                binding.root.setBackgroundResource(android.R.color.transparent)
            }
            com.owot.android.client.data.models.ChatLocation.PAGE -> {
                binding.root.setBackgroundResource(android.R.color.transparent)
            }
        }
    }
    
    private fun formatTimestamp(dateString: String): String {
        return try {
            // Parse the date string (assuming ISO format)
            val date = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).parse(dateString)
            val now = java.util.Date()
            val diff = now.time - (date?.time ?: 0)
            
            when {
                diff < 60_000 -> "Now"
                diff < 3_600_000 -> "${diff / 60_000}m ago"
                diff < 86_400_000 -> "${diff / 3_600_000}h ago"
                else -> "${diff / 86_400_000}d ago"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
}

class ChatDiffCallback : DiffUtil.ItemCallback<ChatResponse>() {
    override fun areItemsTheSame(oldItem: ChatResponse, newItem: ChatResponse): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: ChatResponse, newItem: ChatResponse): Boolean {
        return oldItem == newItem
    }
}