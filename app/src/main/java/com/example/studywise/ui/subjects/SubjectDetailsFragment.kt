package com.example.studywise.ui.subjects

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.studywise.R
import com.example.studywise.StudyWiseApplication
import com.example.studywise.adapter.TopicAdapter
import com.example.studywise.data.entity.Subject
import com.example.studywise.data.entity.Topic
import com.example.studywise.databinding.DialogAddTopicBinding
import com.example.studywise.databinding.DialogSpacedRevisionBinding
import com.example.studywise.databinding.FragmentSubjectDetailsBinding
import com.example.studywise.domain.ReadinessStatus
import com.example.studywise.domain.RecallQuality
import com.example.studywise.ui.common.ViewModelFactory
import com.example.studywise.utils.DateTimeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SubjectDetailsFragment : Fragment() {

    private var _binding: FragmentSubjectDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SubjectDetailsViewModel by viewModels {
        val app = requireActivity().application as StudyWiseApplication
        ViewModelFactory(app.repository)
    }

    private lateinit var topicAdapter: TopicAdapter
    private var currentSubject: Subject? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubjectDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val subjectId = arguments?.getLong("subjectId", -1L) ?: -1L
        if (subjectId != -1L) {
            viewModel.setSubjectId(subjectId)
        }

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        topicAdapter = TopicAdapter(
            onToggleCompleted = { topic, isCompleted ->
                viewModel.toggleTopicCompleted(topic, isCompleted)
            },
            onReviewClick = { topic ->
                showSpacedRevisionDialog(topic)
            },
            onItemClick = { topic ->
                showEditTopicDialog(topic)
            }
        )
        binding.rvTopics.adapter = topicAdapter
    }

    private fun setupListeners() {
        binding.btnEditSubject.setOnClickListener {
            currentSubject?.let { subject ->
                val bundle = Bundle().apply {
                    putLong("subjectId", subject.id)
                }
                findNavController().navigate(R.id.action_subject_details_to_add_edit, bundle)
            }
        }

        binding.btnDeleteSubject.setOnClickListener {
            val subject = currentSubject ?: return@setOnClickListener
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Subject?")
                .setMessage("Are you sure you want to delete this subject?")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteSubject(subject) {
                        Toast.makeText(requireContext(), "Subject deleted", Toast.LENGTH_SHORT).show()
                        val popped = findNavController().popBackStack(R.id.navigation_subjects, false)
                        if (!popped) {
                            findNavController().popBackStack()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnAddTopic.setOnClickListener {
            showAddTopicDialog()
        }

        binding.btnStartDetailsFocus.setOnClickListener {
            currentSubject?.let { subject ->
                val bundle = Bundle().apply {
                    putLong("selectedSubjectId", subject.id)
                    putInt("presetDurationMinutes", subject.dailyGoalMinutes)
                }
                findNavController().navigate(R.id.action_subject_details_to_focus, bundle)
            }
        }

        binding.btnDetailsTakeQuiz.setOnClickListener {
            findNavController().navigate(R.id.action_subject_details_to_quiz_list)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val subject = state.subject ?: return@collect
                    currentSubject = subject
                    renderSubjectHeader(subject)
                    renderReadiness(state)
                    renderTopics(state)
                }
            }
        }
    }

    private fun renderSubjectHeader(subject: Subject) {
        val context = requireContext()
        binding.tvDetailsSubjectName.text = subject.name
        binding.tvDetailsExamCountdown.text = DateTimeUtils.getExamCountdownText(subject.examDate)
        binding.tvDetailsDifficulty.text = "${subject.difficulty.replaceFirstChar { it.uppercase() }} Difficulty"

        val diffColor = when (subject.difficulty.uppercase()) {
            "HARD" -> ContextCompat.getColor(context, R.color.difficulty_hard)
            "MEDIUM" -> ContextCompat.getColor(context, R.color.difficulty_medium)
            else -> ContextCompat.getColor(context, R.color.difficulty_easy)
        }
        binding.tvDetailsDifficulty.setTextColor(diffColor)

        if (subject.description.isNotBlank()) {
            binding.tvDetailsDescription.text = subject.description
            binding.tvDetailsDescription.visibility = View.VISIBLE
        } else {
            binding.tvDetailsDescription.visibility = View.GONE
        }
    }

    private fun renderReadiness(state: SubjectDetailsUiState) {
        val readiness = state.readiness ?: return
        val context = requireContext()

        binding.tvReadinessPercentage.text = "${readiness.overallPercentage}%"
        binding.tvReadinessStatus.text = readiness.status.label.uppercase()

        val (statusBg, statusTextColor) = when (readiness.status) {
            ReadinessStatus.READY -> Pair(
                ContextCompat.getColor(context, R.color.priority_low_container),
                ContextCompat.getColor(context, R.color.priority_low_text)
            )
            ReadinessStatus.ON_TRACK -> Pair(
                ContextCompat.getColor(context, R.color.secondary_container),
                ContextCompat.getColor(context, R.color.on_secondary_container)
            )
            ReadinessStatus.NEEDS_WORK -> Pair(
                ContextCompat.getColor(context, R.color.priority_medium_container),
                ContextCompat.getColor(context, R.color.priority_medium_text)
            )
            ReadinessStatus.AT_RISK -> Pair(
                ContextCompat.getColor(context, R.color.priority_critical_container),
                ContextCompat.getColor(context, R.color.priority_critical_text)
            )
        }
        binding.tvReadinessStatus.setBackgroundColor(statusBg)
        binding.tvReadinessStatus.setTextColor(statusTextColor)

        // Factors
        binding.progressPrepFactor.progress = readiness.preparationScore
        binding.tvPrepFactorValue.text = "${readiness.preparationScore}%"

        binding.progressTopicFactor.progress = readiness.topicScore
        binding.tvTopicFactorValue.text = "${readiness.topicScore}%"

        binding.progressQuizFactor.progress = readiness.quizScore
        binding.tvQuizFactorValue.text = "${readiness.quizScore}%"

        binding.progressConsistencyFactor.progress = readiness.consistencyScore
        binding.tvConsistencyFactorValue.text = "${readiness.consistencyScore}%"

        // Revision due card
        if (state.revisionDueCount > 0) {
            binding.cardRevisionDueAlert.visibility = View.VISIBLE
            binding.tvRevisionDueCount.text = getString(R.string.revision_due_format, state.revisionDueCount)
        } else {
            binding.cardRevisionDueAlert.visibility = View.GONE
        }
    }

    private fun renderTopics(state: SubjectDetailsUiState) {
        binding.tvTopicsSectionHeader.text = "TOPICS (${state.topics.size}) • ${state.averageTopicCompletion}% DONE"
        topicAdapter.submitList(state.topics)

        if (state.topics.isEmpty()) {
            binding.tvTopicsEmpty.visibility = View.VISIBLE
            binding.rvTopics.visibility = View.GONE
        } else {
            binding.tvTopicsEmpty.visibility = View.GONE
            binding.rvTopics.visibility = View.VISIBLE
        }
    }

    private fun showAddTopicDialog() {
        val dialogBinding = DialogAddTopicBinding.inflate(layoutInflater)

        dialogBinding.sliderTopicCompletion.addOnChangeListener { _, value, _ ->
            dialogBinding.tvTopicCompletionValue.text = "${value.toInt()}%"
        }
        dialogBinding.tvTopicCompletionValue.text = "${dialogBinding.sliderTopicCompletion.value.toInt()}%"

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val name = dialogBinding.etTopicName.text?.toString()?.trim() ?: ""
                if (name.isNotEmpty()) {
                    val comp = dialogBinding.sliderTopicCompletion.value.toInt()
                    val mastery = when {
                        dialogBinding.chipMastered.isChecked -> "MASTERED"
                        dialogBinding.chipFamiliar.isChecked -> "FAMILIAR"
                        else -> "UNSTUDIED"
                    }
                    viewModel.addTopic(name, comp, mastery)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditTopicDialog(topic: Topic) {
        val dialogBinding = DialogAddTopicBinding.inflate(layoutInflater)
        dialogBinding.tvTopicDialogTitle.text = getString(R.string.edit_topic)
        dialogBinding.etTopicName.setText(topic.name)
        dialogBinding.sliderTopicCompletion.value = topic.completionPercentage.toFloat()
        dialogBinding.tvTopicCompletionValue.text = "${topic.completionPercentage}%"

        when (topic.masteryLevel.uppercase()) {
            "MASTERED" -> dialogBinding.chipMastered.isChecked = true
            "FAMILIAR" -> dialogBinding.chipFamiliar.isChecked = true
            else -> dialogBinding.chipUnstudied.isChecked = true
        }

        dialogBinding.sliderTopicCompletion.addOnChangeListener { _, value, _ ->
            dialogBinding.tvTopicCompletionValue.text = "${value.toInt()}%"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val name = dialogBinding.etTopicName.text?.toString()?.trim() ?: ""
                if (name.isNotEmpty()) {
                    val comp = dialogBinding.sliderTopicCompletion.value.toInt()
                    val mastery = when {
                        dialogBinding.chipMastered.isChecked -> "MASTERED"
                        dialogBinding.chipFamiliar.isChecked -> "FAMILIAR"
                        else -> "UNSTUDIED"
                    }
                    val updated = topic.copy(name = name, completionPercentage = comp, masteryLevel = mastery)
                    viewModel.updateTopic(updated)
                }
            }
            .setNeutralButton("Delete") { _, _ ->
                viewModel.deleteTopic(topic)
                Toast.makeText(requireContext(), "Topic removed", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showSpacedRevisionDialog(topic: Topic) {
        val dialogBinding = DialogSpacedRevisionBinding.inflate(layoutInflater)
        dialogBinding.tvRevisionTopicTitle.text = topic.name

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnRecallHard.setOnClickListener {
            viewModel.recordSpacedRevision(topic, RecallQuality.HARD)
            dialog.dismiss()
            Toast.makeText(requireContext(), "Scheduled for revision tomorrow (1d)", Toast.LENGTH_SHORT).show()
        }
        dialogBinding.btnRecallOkay.setOnClickListener {
            viewModel.recordSpacedRevision(topic, RecallQuality.OKAY)
            dialog.dismiss()
            Toast.makeText(requireContext(), "Scheduled for revision in 3 days", Toast.LENGTH_SHORT).show()
        }
        dialogBinding.btnRecallGood.setOnClickListener {
            viewModel.recordSpacedRevision(topic, RecallQuality.GOOD)
            dialog.dismiss()
            Toast.makeText(requireContext(), "Scheduled for revision in 7 days", Toast.LENGTH_SHORT).show()
        }
        dialogBinding.btnRecallEasy.setOnClickListener {
            viewModel.recordSpacedRevision(topic, RecallQuality.EASY)
            dialog.dismiss()
            Toast.makeText(requireContext(), "Mastered! Scheduled in 14 days", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
