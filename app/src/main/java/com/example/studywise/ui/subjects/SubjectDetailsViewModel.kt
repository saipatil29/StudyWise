package com.example.studywise.ui.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studywise.data.entity.Subject
import com.example.studywise.data.entity.Topic
import com.example.studywise.domain.ExamReadinessCalculator
import com.example.studywise.domain.ExamReadinessResult
import com.example.studywise.domain.ReadinessStatus
import com.example.studywise.domain.RecallQuality
import com.example.studywise.domain.SpacedRepetitionEngine
import com.example.studywise.repository.StudyWiseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SubjectDetailsUiState(
    val subject: Subject? = null,
    val topics: List<Topic> = emptyList(),
    val readiness: ExamReadinessResult? = null,
    val revisionDueCount: Int = 0,
    val averageTopicCompletion: Int = 0
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SubjectDetailsViewModel(
    private val repository: StudyWiseRepository
) : ViewModel() {

    private val currentSubjectId = MutableStateFlow<Long?>(null)

    fun setSubjectId(id: Long) {
        currentSubjectId.value = id
    }

    val uiState: StateFlow<SubjectDetailsUiState> = currentSubjectId
        .filterNotNull()
        .flatMapLatest { subjectId ->
            combine(
                repository.getSubjectByIdFlow(subjectId),
                repository.getTopicsForSubject(subjectId),
                repository.getSessionsForSubject(subjectId),
                repository.allAttempts
            ) { subject, topics, sessions, allAttempts ->
                if (subject == null) {
                    SubjectDetailsUiState()
                } else {
                    val subjectAttempts = allAttempts.filter { it.subjectId == subject.id }
                    val readiness = ExamReadinessCalculator.calculateReadiness(
                        subject = subject,
                        topics = topics,
                        quizAttempts = subjectAttempts,
                        studySessions = sessions
                    )

                    val now = System.currentTimeMillis()
                    val revisionDue = topics.count { SpacedRepetitionEngine.isRevisionDue(it.nextReviewDate, now) }
                    val avgCompletion = if (topics.isNotEmpty()) {
                        (topics.map { it.completionPercentage }.average()).toInt()
                    } else 0

                    SubjectDetailsUiState(
                        subject = subject,
                        topics = topics,
                        readiness = readiness,
                        revisionDueCount = revisionDue,
                        averageTopicCompletion = avgCompletion
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SubjectDetailsUiState()
        )

    fun addTopic(name: String, completionPercentage: Int, masteryLevel: String) {
        val subjectId = currentSubjectId.value ?: return
        viewModelScope.launch {
            val topic = Topic(
                subjectId = subjectId,
                name = name,
                completionPercentage = completionPercentage,
                masteryLevel = masteryLevel
            )
            repository.insertTopic(topic)
            updateSubjectPreparationFromTopics(subjectId)
        }
    }

    fun updateTopic(topic: Topic) {
        viewModelScope.launch {
            repository.updateTopic(topic)
            currentSubjectId.value?.let { updateSubjectPreparationFromTopics(it) }
        }
    }

    fun deleteTopic(topic: Topic) {
        viewModelScope.launch {
            repository.deleteTopic(topic)
            currentSubjectId.value?.let { updateSubjectPreparationFromTopics(it) }
        }
    }

    fun toggleTopicCompleted(topic: Topic, completed: Boolean) {
        val updated = topic.copy(
            completionPercentage = if (completed) 100 else 0,
            masteryLevel = if (completed) "MASTERED" else "UNSTUDIED"
        )
        updateTopic(updated)
    }

    fun recordSpacedRevision(topic: Topic, quality: RecallQuality) {
        val nextReviewDate = SpacedRepetitionEngine.calculateNextReviewDate(quality)
        val newMastery = SpacedRepetitionEngine.getUpdatedMasteryLevel(quality, topic.masteryLevel)
        val updated = topic.copy(
            masteryLevel = newMastery,
            lastReviewed = System.currentTimeMillis(),
            nextReviewDate = nextReviewDate
        )
        updateTopic(updated)
    }

    fun deleteSubject(subject: Subject, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
            onComplete()
        }
    }

    private suspend fun updateSubjectPreparationFromTopics(subjectId: Long) {
        val topics = repository.getTopicsForSubjectSync(subjectId)
        val subject = repository.getSubjectById(subjectId) ?: return
        if (topics.isNotEmpty()) {
            val avgTopicCompletion = (topics.map { it.completionPercentage }.average()).toInt()
            // Weighted update: 70% topic progress, 30% existing prep
            val newPrep = ((avgTopicCompletion * 0.7) + (subject.preparationPercentage * 0.3)).toInt().coerceIn(0, 100)
            repository.updateSubject(subject.copy(preparationPercentage = newPrep, updatedAt = System.currentTimeMillis()))
        }
    }
}
