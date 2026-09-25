package com.example.studywise.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studywise.R
import com.example.studywise.data.entity.Badge
import com.example.studywise.databinding.ItemBadgeBinding

class BadgeAdapter : ListAdapter<Badge, BadgeAdapter.BadgeViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val binding = ItemBadgeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BadgeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BadgeViewHolder(
        private val binding: ItemBadgeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(badge: Badge) {
            val context = binding.root.context
            binding.tvBadgeName.text = badge.name
            binding.tvBadgeDescription.text = badge.description

            if (badge.isUnlocked) {
                binding.tvBadgeStatus.text = "UNLOCKED"
                binding.tvBadgeStatus.setTextColor(ContextCompat.getColor(context, R.color.success))
                binding.ivBadgeIcon.setColorFilter(ContextCompat.getColor(context, R.color.xp_gold))
                binding.cardBadge.alpha = 1.0f
            } else {
                binding.tvBadgeStatus.text = "LOCKED"
                binding.tvBadgeStatus.setTextColor(ContextCompat.getColor(context, R.color.text_tertiary))
                binding.ivBadgeIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_tertiary))
                binding.cardBadge.alpha = 0.6f
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Badge>() {
        override fun areItemsTheSame(oldItem: Badge, newItem: Badge): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Badge, newItem: Badge): Boolean {
            return oldItem == newItem
        }
    }
}
