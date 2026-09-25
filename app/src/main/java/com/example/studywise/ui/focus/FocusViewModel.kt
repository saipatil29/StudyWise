package com.example.studywise.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studywise.adapter.StudySessionDisplayItem
import com.example.studywise.data.entity.Subject
import com.example.studywise.repository.StudyWiseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FocusTimerState {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

data class SessionCompletedEvent(
    val subjectName: String,
    val minutesStudied: Int,
    val xpAwarded: Int
)

data class FocusUiState(
    val selectedSubject: Subject? = null,
    val durationMinutes: Int = 25,
    val remainingSeconds: Long = 25 * 60L,
    val totalSeconds: Long = 25 * 60L,
    val progressPercentage: Int = 100,
    val timerState: FocusTimerState = FocusTimerState.IDLE,
    val formattedTime: String = "25:00"
)

class FocusViewModel(
    private val repository: StudyWiseRepository
) : ViewModel() {

    val allSubjects: StateFlow<List<Subject>> = repository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyHistory: StateFlow<List<StudySessionDisplayItem>> = combine(
        repository.allSessions,
        repository.allSubjects
    ) { sessions, subjects ->
        val subjectMap = subjects.associateBy { it.id }
        sessions.map { session ->
            val subName = subjectMap[session.subjectId]?.name ?: "General Study"
            StudySessionDisplayItem(session = session, subjectName = subName)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private val _completedEvent = MutableSharedFlow<SessionCompletedEvent>()
    val completedEvent: SharedFlow<SessionCompletedEvent> = _completedEvent.asSharedFlow()

    private var timerJob: Job? = null
    private var isSessionSaved = false

    fun selectSubjectById(id: Long) {
        viewModelScope.launch {
            val subject = repository.getSubjectById(id)
            if (subject != null) {
                _uiState.value = _uiState.value.copy(selectedSubject = subject)
            }
        }
    }

    fun selectSubject(subject: Subject) {
        _uiState.value = _uiState.value.copy(selectedSubject = subject)
    }

    fun setDuration(minutes: Int) {
        if (_uiState.value.timerState == FocusTimerState.RUNNING) return
        val clamped = minutes.coerceIn(1, 180)
        val totalSecs = clamped * 60L
        _uiState.value = _uiState.value.copy(
            durationMinutes = clamped,
            totalSeconds = totalSecs,
            remainingSeconds = totalSecs,
            progressPercentage = 100,
            formattedTime = formatTime(totalSecs),
            timerState = FocusTimerState.IDLE
        )
    }

    fun startTimer() {
        if (_uiState.value.timerState == FocusTimerState.RUNNING) return
        isSessionSaved = false
        _uiState.value = _uiState.value.copy(timerState = FocusTimerState.RUNNING)

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0 && _uiState.value.timerState == FocusTimerState.RUNNING) {
                delay(1000)
                val newRemaining = _uiState.value.remainingSeconds - 1
                val total = _uiState.value.totalSeconds
                val progress = if (total > 0) ((newRemaining.toDouble() / total) * 100).toInt() else 0

                _uiState.value = _uiState.value.copy(
                    remainingSeconds = newRemaining,
                    progressPercentage = progress,
                    formattedTime = formatTime(newRemaining)
                )

                if (newRemaining <= 0) {
                    onTimerFinished()
                    break
                }
            }
        }
    }

    fun pauseTimer() {
        if (_uiState.value.timerState == FocusTimerState.RUNNING) {
            timerJob?.cancel()
            _uiState.value = _uiState.value.copy(timerState = FocusTimerState.PAUSED)
        }
    }

    fun resumeTimer() {
        if (_uiState.value.timerState == FocusTimerState.PAUSED) {
            startTimer()
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        val total = _uiState.value.totalSeconds
        _uiState.value = _uiState.value.copy(
            remainingSeconds = total,
            progressPercentage = 100,
            formattedTime = formatTime(total),
            timerState = FocusTimerState.IDLE
        )
    }

    fun finishEarly() {
        if (_uiState.value.timerState == FocusTimerState.RUNNING || _uiState.value.timerState == FocusTimerState.PAUSED) {
            timerJob?.cancel()
            onTimerFinished()
        }
    }

    private fun onTimerFinished() {
        if (isSessionSaved) return
        isSessionSaved = true

        val current = _uiState.value
        val elapsedSeconds = current.totalSeconds - current.remainingSeconds
        val elapsedMinutes = (elapsedSeconds / 60).toInt().coerceAtLeast(1)
        val subject = current.selectedSubject

        _uiState.value = current.copy(
            timerState = FocusTimerState.COMPLETED,
            remainingSeconds = 0,
            progressPercentage = 0,
            formattedTime = "00:00"
        )

        viewModelScope.launch {
            val subjectId = subject?.id ?: (repository.getAllSubjectsSync().firstOrNull()?.id ?: 1L)
            val subName = subject?.name ?: "Focus Study"
            repository.recordStudySession(subjectId = subjectId, durationMinutes = elapsedMinutes)

            val xp = (elapsedMinutes * 1).coerceAtLeast(10)
            _completedEvent.emit(
                SessionCompletedEvent(
                    subjectName = subName,
                    minutesStudied = elapsedMinutes,
                    xpAwarded = xp
                )
            )
        }
    }

    private fun formatTime(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
