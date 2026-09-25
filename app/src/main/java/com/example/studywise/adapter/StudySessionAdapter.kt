package com.example.studywise.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studywise.R
import com.example.studywise.data.entity.StudySession
import com.example.studywise.databinding.ItemStudySessionBinding
import com.example.studywise.utils.DateTimeUtils

data class StudySessionDisplayItem(
    val session: StudySession,
    val subjectName: String
)

class StudySessionAdapter : ListAdapter<StudySessionDisplayItem, StudySessionAdapter.SessionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemStudySessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SessionViewHolder(
        private val binding: ItemStudySessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StudySessionDisplayItem) {
            val session = item.session
            binding.tvSessionSubject.text = item.subjectName
            val dateStr = DateTimeUtils.formatDate(session.date)
            val timeStr = DateTimeUtils.formatTime(session.date)
            binding.tvSessionTime.text = "$dateStr • $timeStr"
            binding.tvSessionDuration.text = "${session.durationMinutes} min"
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<StudySessionDisplayItem>() {
        override fun areItemsTheSame(oldItem: StudySessionDisplayItem, newItem: StudySessionDisplayItem): Boolean {
            return oldItem.session.id == newItem.session.id
        }

        override fun areContentsTheSame(oldItem: StudySessionDisplayItem, newItem: StudySessionDisplayItem): Boolean {
            return oldItem == newItem
        }
    }
}
