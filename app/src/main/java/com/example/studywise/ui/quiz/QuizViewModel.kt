package com.example.studywise.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studywise.adapter.QuizItemDisplay
import com.example.studywise.data.entity.Quiz
import com.example.studywise.data.entity.QuizQuestion
import com.example.studywise.repository.StudyWiseRepository
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

data class QuizSessionState(
    val quiz: Quiz? = null,
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val currentScore: Int = 0,
    val isLastQuestion: Boolean = false,
    val isFinished: Boolean = false
)

data class QuizFinishedEvent(
    val score: Int,
    val total: Int,
    val percentage: Int
)

class QuizViewModel(
    private val repository: StudyWiseRepository
) : ViewModel() {

    val quizList: StateFlow<List<QuizItemDisplay>> = combine(
        repository.allQuizzes,
        repository.allSubjects
    ) { quizzes, subjects ->
        val subjectMap = subjects.associateBy { it.id }
        quizzes.map { quiz ->
            val subName = subjectMap[quiz.subjectId]?.name ?: "General"
            QuizItemDisplay(quiz = quiz, subjectName = subName)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _sessionState = MutableStateFlow(QuizSessionState())
    val sessionState: StateFlow<QuizSessionState> = _sessionState.asStateFlow()

    private val _finishedEvent = MutableSharedFlow<QuizFinishedEvent>()
    val finishedEvent: SharedFlow<QuizFinishedEvent> = _finishedEvent.asSharedFlow()

    fun startQuizSession(quizId: Long) {
        viewModelScope.launch {
            val quiz = repository.getQuizById(quizId)
            val questions = repository.getQuestionsForQuizSync(quizId)
            _sessionState.value = QuizSessionState(
                quiz = quiz,
                questions = questions,
                currentIndex = 0,
                currentScore = 0,
                isLastQuestion = questions.size <= 1,
                isFinished = false
            )
        }
    }

    fun submitAnswerAndProceed(selectedOption: Int) {
        val state = _sessionState.value
        val questions = state.questions
        if (state.currentIndex !in questions.indices) return

        val currentQ = questions[state.currentIndex]
        val isCorrect = (selectedOption == currentQ.correctOptionIndex)
        val newScore = if (isCorrect) state.currentScore + 1 else state.currentScore

        val nextIndex = state.currentIndex + 1
        if (nextIndex < questions.size) {
            _sessionState.value = state.copy(
                currentIndex = nextIndex,
                currentScore = newScore,
                isLastQuestion = (nextIndex == questions.lastIndex)
            )
        } else {
            // Finished
            val total = questions.size
            val percentage = if (total > 0) (newScore * 100) / total else 0
            _sessionState.value = state.copy(
                currentScore = newScore,
                isFinished = true
            )

            viewModelScope.launch {
                state.quiz?.let { q ->
                    repository.recordQuizAttempt(
                        quizId = q.id,
                        subjectId = q.subjectId,
                        score = newScore,
                        total = total
                    )
                }
                _finishedEvent.emit(QuizFinishedEvent(newScore, total, percentage))
            }
        }
    }
}
