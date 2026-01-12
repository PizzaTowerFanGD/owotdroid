package com.owot.android.client.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.owot.android.client.data.models.WorldListItem
import com.owot.android.client.databinding.ItemWorldBinding

/**
 * RecyclerView adapter for displaying world list items
 */
class WorldListAdapter(
    private val onWorldClick: (String) -> Unit
) : ListAdapter<WorldListItem, WorldViewHolder>(WorldDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorldViewHolder {
        val binding = ItemWorldBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WorldViewHolder(binding, onWorldClick)
    }
    
    override fun onBindViewHolder(holder: WorldViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class WorldViewHolder(
    private val binding: ItemWorldBinding,
    private val onWorldClick: (String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    
    fun bind(world: WorldListItem) {
        binding.textWorldTitle.text = world.title
        binding.textWorldDescription.text = world.description
        binding.textUserCount.text = "${world.userCount} users"
        binding.textLastVisited.text = formatLastVisited(world.lastVisited)
        
        binding.root.setOnClickListener {
            onWorldClick(world.name)
        }
    }
    
    private fun formatLastVisited(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            else -> "${diff / 86_400_000}d ago"
        }
    }
}

class WorldDiffCallback : DiffUtil.ItemCallback<WorldListItem>() {
    override fun areItemsTheSame(oldItem: WorldListItem, newItem: WorldListItem): Boolean {
        return oldItem.name == newItem.name
    }
    
    override fun areContentsTheSame(oldItem: WorldListItem, newItem: WorldListItem): Boolean {
        return oldItem == newItem
    }
}