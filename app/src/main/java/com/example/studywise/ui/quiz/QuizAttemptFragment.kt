package com.example.studywise.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.studywise.R
import com.example.studywise.StudyWiseApplication
import com.example.studywise.databinding.FragmentQuizAttemptBinding
import com.example.studywise.ui.common.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class QuizAttemptFragment : Fragment() {

    private var _binding: FragmentQuizAttemptBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuizViewModel by activityViewModels {
        val app = requireActivity().application as StudyWiseApplication
        ViewModelFactory(app.repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizAttemptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val quizId = arguments?.getLong("quizId", -1L) ?: -1L
        if (quizId != -1L) {
            viewModel.startQuizSession(quizId)
        }

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnQuizAction.setOnClickListener {
            val selectedRadioId = binding.rgQuizOptions.checkedRadioButtonId
            if (selectedRadioId == -1) {
                Toast.makeText(requireContext(), "Please select an answer", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedIndex = when (selectedRadioId) {
                R.id.rbOptionA -> 0
                R.id.rbOptionB -> 1
                R.id.rbOptionC -> 2
                R.id.rbOptionD -> 3
                else -> 0
            }

            viewModel.submitAnswerAndProceed(selectedIndex)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sessionState.collect { state ->
                    renderSession(state)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.finishedEvent.collect { event ->
                    showResultsDialog(event)
                }
            }
        }
    }

    private fun renderSession(state: QuizSessionState) {
        val questions = state.questions
        if (questions.isEmpty() || state.currentIndex !in questions.indices) return

        val q = questions[state.currentIndex]
        binding.tvAttemptQuizTitle.text = state.quiz?.title ?: "Subject Quiz"
        binding.tvQuestionCounter.text = getString(R.string.question_counter_format, state.currentIndex + 1, questions.size)
        binding.tvQuizScoreTracker.text = "Score: ${state.currentScore}"

        val progress = (((state.currentIndex + 1).toDouble() / questions.size) * 100).toInt()
        binding.progressQuizQuestions.progress = progress

        binding.tvQuestionText.text = q.questionText
        binding.rbOptionA.text = q.optionA
        binding.rbOptionB.text = q.optionB
        binding.rbOptionC.text = q.optionC
        binding.rbOptionD.text = q.optionD

        // Clear selection for new question
        binding.rgQuizOptions.clearCheck()

        binding.btnQuizAction.text = if (state.isLastQuestion) getString(R.string.btn_submit) else getString(R.string.btn_next)
    }

    private fun showResultsDialog(event: QuizFinishedEvent) {
        val xpAwarded = if (event.percentage >= 80) 35 else 20
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.quiz_completed_title)
            .setMessage(getString(R.string.quiz_score_format, event.score, event.total, event.percentage) + "\n\n+$xpAwarded XP awarded!")
            .setPositiveButton("Done") { _, _ ->
                findNavController().popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
