package com.example.studywise.ui.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studywise.adapter.SubjectCardItem
import com.example.studywise.data.entity.Subject
import com.example.studywise.domain.PriorityCalculator
import com.example.studywise.repository.StudyWiseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SubjectsViewModel(
    private val repository: StudyWiseRepository
) : ViewModel() {

    val subjects: StateFlow<List<SubjectCardItem>> = combine(
        repository.allSubjects,
        repository.allTopics,
        repository.allSessions
    ) { subjectList, topicList, sessionList ->
        val now = System.currentTimeMillis()
        val topicsBySubject = topicList.groupBy { it.subjectId }
        val sessionsBySubject = sessionList.groupBy { it.subjectId }

        subjectList.map { subject ->
            val subTopics = topicsBySubject[subject.id] ?: emptyList()
            val subSessions = sessionsBySubject[subject.id] ?: emptyList()
            val score = PriorityCalculator.calculatePriorityScore(subject, subTopics, subSessions, now)
            val level = PriorityCalculator.getPriorityLevel(score)

            SubjectCardItem(
                subject = subject,
                priorityScore = score,
                priorityLevel = level
            )
        }.sortedByDescending { it.priorityScore }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
        }
    }
}
