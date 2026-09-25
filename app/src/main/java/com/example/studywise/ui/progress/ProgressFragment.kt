package com.example.studywise.ui.progress

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.studywise.R
import com.example.studywise.StudyWiseApplication
import com.example.studywise.databinding.FragmentProgressBinding
import com.example.studywise.ui.common.ViewModelFactory
import com.example.studywise.utils.DateTimeUtils
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch

class ProgressFragment : Fragment() {

    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProgressViewModel by viewModels {
        val app = requireActivity().application as StudyWiseApplication
        ViewModelFactory(app.repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderStats(state)
                    renderWeeklyChart(state.weekDaysActivity)
                    renderSubjectBreakdown(state.subjectPreparations)
                }
            }
        }
    }

    private fun renderStats(state: ProgressUiState) {
        binding.tvTotalStudyTime.text = DateTimeUtils.formatDuration(state.totalStudyMinutes)
        binding.tvTodayStudyTime.text = DateTimeUtils.formatDuration(state.todayStudyMinutes)
        binding.tvCurrentStreak.text = "${state.currentStreak} Days"
        binding.tvAvgQuizScore.text = "${state.avgQuizScorePercentage}%"

        binding.tvTotalSessions.text = state.totalSessionsCount.toString()
        binding.tvAvgSessionDuration.text = "${state.avgSessionDurationMinutes}m"
        binding.tvLongestStreak.text = "${state.longestStreak} Days"

        binding.tvWeeklyTotalMinutes.text = "${DateTimeUtils.formatDuration(state.weeklyStudyMinutes)} this week"
        binding.tvOverallPrepScore.text = "${state.overallPreparationPercentage}% Avg"
    }

    private fun renderWeeklyChart(days: List<DayActivity>) {
        val chartContainer = binding.layoutWeeklyChart
        chartContainer.removeAllViews()

        if (days.isEmpty()) return

        val maxMinutes = (days.maxOfOrNull { it.minutesStudied } ?: 60).coerceAtLeast(45)
        val context = requireContext()

        for (day in days) {
            val colLayout = LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                setPadding(4, 0, 4, 0)
            }

            // Minutes text on top of bar
            val tvMinutes = TextView(context).apply {
                text = if (day.minutesStudied > 0) "${day.minutesStudied}m" else "-"
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(
                    if (day.isToday) ContextCompat.getColor(context, R.color.primary)
                    else ContextCompat.getColor(context, R.color.text_secondary)
                )
                if (day.isToday) typeface = Typeface.DEFAULT_BOLD
            }
            colLayout.addView(tvMinutes)

            // Bar view
            val barHeightFraction = (day.minutesStudied.toFloat() / maxMinutes).coerceIn(0.08f, 1.0f)
            val maxBarHeightPx = (80 * resources.displayMetrics.density).toInt()
            val actualHeight = if (day.minutesStudied > 0) (maxBarHeightPx * barHeightFraction).toInt() else (8 * resources.displayMetrics.density).toInt()

            val barView = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (14 * resources.displayMetrics.density).toInt(),
                    actualHeight
                ).apply {
                    topMargin = (4 * resources.displayMetrics.density).toInt()
                    bottomMargin = (6 * resources.displayMetrics.density).toInt()
                }
                background = ContextCompat.getDrawable(context, R.drawable.bg_chart_bar)
                backgroundTintList = ContextCompat.getColorStateList(
                    context,
                    if (day.isToday) R.color.primary
                    else if (day.minutesStudied > 0) R.color.primary_container
                    else R.color.surface_variant
                )
            }
            colLayout.addView(barView)

            // Day label (Mon, Tue, etc.)
            val tvDay = TextView(context).apply {
                text = day.dayName
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(
                    if (day.isToday) ContextCompat.getColor(context, R.color.primary)
                    else ContextCompat.getColor(context, R.color.text_secondary)
                )
                if (day.isToday) typeface = Typeface.DEFAULT_BOLD
            }
            colLayout.addView(tvDay)

            chartContainer.addView(colLayout)
        }
    }

    private fun renderSubjectBreakdown(subjects: List<SubjectPrepItem>) {
        val container = binding.layoutSubjectPrepBreakdown
        container.removeAllViews()

        if (subjects.isEmpty()) {
            val emptyTv = TextView(requireContext()).apply {
                text = "No subjects enrolled yet."
                textSize = 13f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
            container.addView(emptyTv)
            return
        }

        val context = requireContext()
        val density = resources.displayMetrics.density

        for (item in subjects) {
            val row = LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (12 * density).toInt()
                }
                orientation = LinearLayout.VERTICAL
            }

            // Top row with subject name and percentage
            val headerRow = LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val tvName = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = item.subjectName
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
            headerRow.addView(tvName)

            val tvPercent = TextView(context).apply {
                text = "${item.preparationPercentage}%"
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(context, R.color.primary))
            }
            headerRow.addView(tvPercent)

            row.addView(headerRow)

            // Progress indicator
            val progressIndicator = LinearProgressIndicator(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (6 * density).toInt()
                }
                progress = item.preparationPercentage
                trackThickness = (6 * density).toInt()
                trackCornerRadius = (3 * density).toInt()
                setIndicatorColor(ContextCompat.getColor(context, R.color.primary))
                trackColor = ContextCompat.getColor(context, R.color.primary_container)
            }
            row.addView(progressIndicator)

            container.addView(row)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
