package com.example.studywise.ui.home

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.studywise.R
import com.example.studywise.StudyWiseApplication
import com.example.studywise.adapter.ExamCountdownAdapter
import com.example.studywise.adapter.PlannedSessionAdapter
import com.example.studywise.databinding.FragmentHomeBinding
import com.example.studywise.domain.PriorityLevel
import com.example.studywise.ui.common.ViewModelFactory
import com.example.studywise.utils.DateTimeUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        val app = requireActivity().application as StudyWiseApplication
        ViewModelFactory(app.repository)
    }

    private lateinit var examAdapter: ExamCountdownAdapter
    private lateinit var planAdapter: PlannedSessionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        examAdapter = ExamCountdownAdapter { subject ->
            val bundle = Bundle().apply {
                putLong("subjectId", subject.id)
            }
            findNavController().navigate(R.id.action_home_to_subject_details, bundle)
        }
        binding.rvUpcomingExams.adapter = examAdapter

        planAdapter = PlannedSessionAdapter { slot ->
            val bundle = Bundle().apply {
                putLong("selectedSubjectId", slot.subjectId)
                putInt("presetDurationMinutes", slot.durationMinutes)
            }
            findNavController().navigate(R.id.action_home_to_focus, bundle)
        }
        binding.rvTodaysPlan.adapter = planAdapter
    }

    private fun setupListeners() {
        binding.btnEmptyAddSubject.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_add_edit_subject)
        }

        binding.tvViewAllExams.setOnClickListener {
            findNavController().navigate(R.id.navigation_subjects)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderGreeting()
                    renderHeader(state)
                    renderRecommendation(state)
                    renderExamsAndPlan(state)
                }
            }
        }
    }

    private fun renderGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 4..11 -> getString(R.string.greeting_morning)
            in 12..16 -> getString(R.string.greeting_afternoon)
            else -> getString(R.string.greeting_evening)
        }
        binding.tvGreeting.text = greeting

        val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        binding.tvDate.text = dateFormat.format(Date())
    }

    private fun renderHeader(state: HomeUiState) {
        // Streak
        if (state.streak.currentStreak > 0) {
            binding.tvStreakCount.text = getString(R.string.streak_format, state.streak.currentStreak)
            binding.cardStreakBadge.visibility = View.VISIBLE
        } else {
            binding.tvStreakCount.text = "0 Days"
            binding.cardStreakBadge.visibility = View.VISIBLE
        }

        // Daily Goal
        binding.tvDailyGoalText.text = getString(R.string.daily_goal_format, state.completedTodayMinutes, state.dailyGoalMinutes)
        val goalProgress = if (state.dailyGoalMinutes > 0) {
            ((state.completedTodayMinutes.toDouble() / state.dailyGoalMinutes) * 100).toInt().coerceIn(0, 100)
        } else 0
        binding.progressDailyGoal.progress = goalProgress

        // Summary stats
        val plannedStr = DateTimeUtils.formatDuration(state.totalPlannedMinutes)
        val completedStr = DateTimeUtils.formatDuration(state.completedTodayMinutes)
        binding.tvSummaryStats.text = getString(R.string.summary_stats_format, state.subjectsCount, plannedStr, completedStr)

        // Toggle empty state vs content
        if (state.subjectsCount == 0) {
            binding.layoutHomeEmptyState.visibility = View.VISIBLE
            binding.layoutHomeMainContent.visibility = View.GONE
        } else {
            binding.layoutHomeEmptyState.visibility = View.GONE
            binding.layoutHomeMainContent.visibility = View.VISIBLE
        }
    }

    private fun renderRecommendation(state: HomeUiState) {
        val rec = state.topRecommendation ?: return
        val subject = rec.subject
        val context = requireContext()

        binding.tvRecSubjectName.text = subject.name
        binding.tvRecExamCountdown.text = DateTimeUtils.getExamCountdownText(subject.examDate)
        binding.tvRecPriorityScore.text = getString(R.string.priority_score_format, rec.priorityScore)
        binding.tvRecPriorityLevel.text = rec.priorityLevel.name
        binding.tvRecDuration.text = getString(R.string.recommended_duration_format, rec.recommendedDurationMinutes)
        binding.tvRecReason.text = rec.reason

        // Colors based on priority level
        val (bgColor, textColor) = when (rec.priorityLevel) {
            PriorityLevel.CRITICAL -> Pair(
                ContextCompat.getColor(context, R.color.priority_critical_container),
                ContextCompat.getColor(context, R.color.priority_critical_text)
            )
            PriorityLevel.HIGH -> Pair(
                ContextCompat.getColor(context, R.color.priority_high_container),
                ContextCompat.getColor(context, R.color.priority_high_text)
            )
            PriorityLevel.MEDIUM -> Pair(
                ContextCompat.getColor(context, R.color.priority_medium_container),
                ContextCompat.getColor(context, R.color.priority_medium_text)
            )
            PriorityLevel.LOW -> Pair(
                ContextCompat.getColor(context, R.color.priority_low_container),
                ContextCompat.getColor(context, R.color.priority_low_text)
            )
        }
        binding.tvRecPriorityLevel.setBackgroundColor(bgColor)
        binding.tvRecPriorityLevel.setTextColor(textColor)

        // Preparation
        val prep = subject.preparationPercentage.coerceIn(0, 100)
        binding.progressRecPrep.progress = prep
        binding.tvRecPrepPercentage.text = getString(R.string.preparation_percentage, prep)

        // Start Study Button
        binding.btnRecStartStudy.setOnClickListener {
            val bundle = Bundle().apply {
                putLong("selectedSubjectId", subject.id)
                putInt("presetDurationMinutes", rec.recommendedDurationMinutes)
            }
            findNavController().navigate(R.id.action_home_to_focus, bundle)
        }
    }

    private fun renderExamsAndPlan(state: HomeUiState) {
        examAdapter.submitList(state.upcomingExams)
        planAdapter.submitList(state.todaysPlan)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
