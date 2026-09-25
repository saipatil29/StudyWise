package com.example.studywise.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studywise.R
import com.example.studywise.data.entity.Topic
import com.example.studywise.databinding.ItemTopicBinding
import com.example.studywise.domain.SpacedRepetitionEngine

class TopicAdapter(
    private val onToggleCompleted: (Topic, Boolean) -> Unit,
    private val onReviewClick: (Topic) -> Unit,
    private val onItemClick: (Topic) -> Unit
) : ListAdapter<Topic, TopicAdapter.TopicViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val binding = ItemTopicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TopicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TopicViewHolder(
        private val binding: ItemTopicBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.layoutTopicDetails.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(pos))
                }
            }

            binding.btnReviewTopic.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onReviewClick(getItem(pos))
                }
            }
        }

        fun bind(topic: Topic) {
            val context = binding.root.context
            binding.tvTopicName.text = topic.name

            // Checkbox state (avoid triggering listener during bind)
            binding.cbTopicCompleted.setOnCheckedChangeListener(null)
            binding.cbTopicCompleted.isChecked = topic.completionPercentage >= 100
            binding.cbTopicCompleted.setOnCheckedChangeListener { _, isChecked ->
                onToggleCompleted(topic, isChecked)
            }

            // Mastery level & Color
            binding.tvMasteryLevel.text = topic.masteryLevel
            val masteryColor = when (topic.masteryLevel.uppercase()) {
                "MASTERED" -> ContextCompat.getColor(context, R.color.success)
                "FAMILIAR" -> ContextCompat.getColor(context, R.color.secondary)
                else -> ContextCompat.getColor(context, R.color.text_tertiary)
            }
            binding.tvMasteryLevel.setTextColor(masteryColor)

            binding.tvTopicCompletion.text = "${topic.completionPercentage}% done"

            // Check if revision is due
            val isDue = SpacedRepetitionEngine.isRevisionDue(topic.nextReviewDate)
            binding.tvRevisionNotice.visibility = if (isDue) View.VISIBLE else View.GONE
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Topic>() {
        override fun areItemsTheSame(oldItem: Topic, newItem: Topic): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Topic, newItem: Topic): Boolean {
            return oldItem == newItem
        }
    }
}
