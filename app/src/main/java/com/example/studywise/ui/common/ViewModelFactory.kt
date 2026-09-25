package com.example.studywise.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.studywise.repository.StudyWiseRepository
import com.example.studywise.ui.focus.FocusViewModel
import com.example.studywise.ui.home.HomeViewModel
import com.example.studywise.ui.profile.ProfileViewModel
import com.example.studywise.ui.progress.ProgressViewModel
import com.example.studywise.ui.quiz.QuizViewModel
import com.example.studywise.ui.subjects.SubjectDetailsViewModel
import com.example.studywise.ui.subjects.SubjectsViewModel

class ViewModelFactory(
    private val repository: StudyWiseRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(repository) as T
            }
            modelClass.isAssignableFrom(SubjectsViewModel::class.java) -> {
                SubjectsViewModel(repository) as T
            }
            modelClass.isAssignableFrom(SubjectDetailsViewModel::class.java) -> {
                SubjectDetailsViewModel(repository) as T
            }
            modelClass.isAssignableFrom(FocusViewModel::class.java) -> {
                FocusViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ProgressViewModel::class.java) -> {
                ProgressViewModel(repository) as T
            }
            modelClass.isAssignableFrom(QuizViewModel::class.java) -> {
                QuizViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
