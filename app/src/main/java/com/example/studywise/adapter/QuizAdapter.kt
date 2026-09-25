package com.example.studywise.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studywise.data.entity.Quiz
import com.example.studywise.databinding.ItemQuizBinding

data class QuizItemDisplay(
    val quiz: Quiz,
    val subjectName: String
)

class QuizAdapter(
    private val onStartQuiz: (Quiz) -> Unit
) : ListAdapter<QuizItemDisplay, QuizAdapter.QuizViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizViewHolder {
        val binding = ItemQuizBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuizViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuizViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class QuizViewHolder(
        private val binding: ItemQuizBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnTakeQuiz.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onStartQuiz(getItem(pos).quiz)
                }
            }
        }

        fun bind(item: QuizItemDisplay) {
            binding.tvQuizTitle.text = item.quiz.title
            binding.tvQuizDescription.text = item.quiz.description
            binding.tvQuizSubjectTag.text = item.subjectName
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<QuizItemDisplay>() {
        override fun areItemsTheSame(oldItem: QuizItemDisplay, newItem: QuizItemDisplay): Boolean {
            return oldItem.quiz.id == newItem.quiz.id
        }

        override fun areContentsTheSame(oldItem: QuizItemDisplay, newItem: QuizItemDisplay): Boolean {
            return oldItem == newItem
        }
    }
}
