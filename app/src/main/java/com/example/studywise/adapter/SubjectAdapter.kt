package com.example.studywise.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studywise.R
import com.example.studywise.data.entity.Subject
import com.example.studywise.databinding.ItemSubjectBinding
import com.example.studywise.domain.PriorityCalculator
import com.example.studywise.domain.PriorityLevel
import com.example.studywise.utils.DateTimeUtils

data class SubjectCardItem(
    val subject: Subject,
    val priorityScore: Int,
    val priorityLevel: PriorityLevel
)

class SubjectAdapter(
    private val onItemClick: (Subject) -> Unit,
    private val onEditClick: ((Subject) -> Unit)? = null,
    private val onDeleteClick: ((Subject) -> Unit)? = null
) : ListAdapter<SubjectCardItem, SubjectAdapter.SubjectViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val binding = ItemSubjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SubjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SubjectViewHolder(
        private val binding: ItemSubjectBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position).subject)
                }
            }

            binding.btnEditSubject.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEditClick?.invoke(getItem(position).subject)
                }
            }

            binding.btnDeleteSubject.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick?.invoke(getItem(position).subject)
                }
            }
        }

        fun bind(item: SubjectCardItem) {
            val context = binding.root.context
            val subject = item.subject

            binding.tvSubjectName.text = subject.name
            binding.tvExamCountdown.text = DateTimeUtils.getExamCountdownText(subject.examDate)
            binding.tvDifficulty.text = subject.difficulty.replaceFirstChar { it.uppercase() }

            // Difficulty color
            val diffColor = when (subject.difficulty.uppercase()) {
                "HARD" -> ContextCompat.getColor(context, R.color.difficulty_hard)
                "MEDIUM" -> ContextCompat.getColor(context, R.color.difficulty_medium)
                else -> ContextCompat.getColor(context, R.color.difficulty_easy)
            }
            binding.tvDifficulty.setTextColor(diffColor)

            // Priority score & level
            binding.tvPriorityScore.text = context.getString(R.string.priority_score_format, item.priorityScore)
            binding.tvPriorityLevel.text = item.priorityLevel.name

            val (bgColor, textColor) = when (item.priorityLevel) {
                PriorityLevel.CRITICAL -> Pair(
                    ContextCompat.getColor(context, R.color.priority_critical_container),
                    ContextCompat.getColor(context, R.color.priority_critical_text)
                )
                PriorityLevel.HIGH -> Pair(
                    ContextCompat.getColor(context, R.color.priority_high_container),
                    ContextCompat.getColor(context, R.color.priority_high_text)
                )
                PriorityLevel.MEDIUM -> Pair(
                    ContextCompat.getColor(context, R.color.priority_medium_container),
                    ContextCompat.getColor(context, R.color.priority_medium_text)
                )
                PriorityLevel.LOW -> Pair(
                    ContextCompat.getColor(context, R.color.priority_low_container),
                    ContextCompat.getColor(context, R.color.priority_low_text)
                )
            }
            binding.tvPriorityLevel.setBackgroundColor(bgColor)
            binding.tvPriorityLevel.setTextColor(textColor)

            // Preparation progress
            val prep = subject.preparationPercentage.coerceIn(0, 100)
            binding.progressPreparation.progress = prep
            binding.tvPreparationPercentage.text = context.getString(R.string.preparation_percentage, prep)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<SubjectCardItem>() {
        override fun areItemsTheSame(oldItem: SubjectCardItem, newItem: SubjectCardItem): Boolean {
            return oldItem.subject.id == newItem.subject.id
        }

        override fun areContentsTheSame(oldItem: SubjectCardItem, newItem: SubjectCardItem): Boolean {
            return oldItem == newItem
        }
    }
}
