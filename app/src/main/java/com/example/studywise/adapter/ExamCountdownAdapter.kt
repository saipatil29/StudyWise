package com.example.studywise.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studywise.R
import com.example.studywise.data.entity.Subject
import com.example.studywise.databinding.ItemExamCountdownBinding
import com.example.studywise.utils.DateTimeUtils

class ExamCountdownAdapter(
    private val onItemClick: (Subject) -> Unit
) : ListAdapter<Subject, ExamCountdownAdapter.ExamViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExamViewHolder {
        val binding = ItemExamCountdownBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExamViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExamViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExamViewHolder(
        private val binding: ItemExamCountdownBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(pos))
                }
            }
        }

        fun bind(subject: Subject) {
            val context = binding.root.context
            binding.tvExamSubject.text = subject.name
            binding.tvExamCountdown.text = DateTimeUtils.getExamCountdownText(subject.examDate)
            binding.tvExamExactDate.text = DateTimeUtils.formatDate(subject.examDate)

            val prep = subject.preparationPercentage.coerceIn(0, 100)
            binding.progressExamPrep.progress = prep
            binding.tvExamPrepText.text = "$prep% Prepared"
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Subject>() {
        override fun areItemsTheSame(oldItem: Subject, newItem: Subject): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Subject, newItem: Subject): Boolean {
            return oldItem == newItem
        }
    }
}
