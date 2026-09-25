package com.example.studywise.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studywise.databinding.ItemPlannedSessionBinding
import com.example.studywise.domain.PlannedStudySlot

class PlannedSessionAdapter(
    private val onItemClick: (PlannedStudySlot) -> Unit
) : ListAdapter<PlannedStudySlot, PlannedSessionAdapter.PlannedViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlannedViewHolder {
        val binding = ItemPlannedSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlannedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlannedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlannedViewHolder(
        private val binding: ItemPlannedSessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(pos))
                }
            }
        }

        fun bind(slot: PlannedStudySlot) {
            binding.tvPlanTime.text = slot.startTime
            binding.tvPlanSubject.text = slot.subjectName
            binding.tvPlanDuration.text = "${slot.durationMinutes} min"

            if (slot.isRescheduled && slot.reschedulingNote.isNotEmpty()) {
                binding.tvPlanRescheduleNote.visibility = View.VISIBLE
                binding.tvPlanRescheduleNote.text = slot.reschedulingNote
            } else {
                binding.tvPlanRescheduleNote.visibility = View.GONE
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<PlannedStudySlot>() {
        override fun areItemsTheSame(oldItem: PlannedStudySlot, newItem: PlannedStudySlot): Boolean {
            return oldItem.subjectId == newItem.subjectId && oldItem.startTime == newItem.startTime
        }

        override fun areContentsTheSame(oldItem: PlannedStudySlot, newItem: PlannedStudySlot): Boolean {
            return oldItem == newItem
        }
    }
}
