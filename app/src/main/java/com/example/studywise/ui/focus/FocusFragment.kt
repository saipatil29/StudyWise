package com.example.studywise.ui.focus

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.studywise.R
import com.example.studywise.StudyWiseApplication
import com.example.studywise.data.entity.Subject
import com.example.studywise.databinding.FragmentFocusBinding
import com.example.studywise.ui.common.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class FocusFragment : Fragment() {

    private var _binding: FragmentFocusBinding? = null
    private val binding get() = _binding!!

    // Use activityViewModels so the timer persists seamlessly across tab switches & screen rotations!
    private val viewModel: FocusViewModel by activityViewModels {
        val app = requireActivity().application as StudyWiseApplication
        ViewModelFactory(app.repository)
    }

    private var availableSubjects: List<Subject> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFocusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if navigated with preset subject or duration
        arguments?.getLong("selectedSubjectId", -1L)?.let { id ->
            if (id != -1L) viewModel.selectSubjectById(id)
        }
        arguments?.getInt("presetDurationMinutes", -1)?.let { mins ->
            if (mins > 0) viewModel.setDuration(mins)
        }

        setupDurationToggle()
        setupListeners()
        observeViewModel()
    }

    private fun setupDurationToggle() {
        binding.toggleDurationGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btnDuration25 -> {
                    binding.tilCustomDuration.visibility = View.GONE
                    viewModel.setDuration(25)
                }
                R.id.btnDuration50 -> {
                    binding.tilCustomDuration.visibility = View.GONE
                    viewModel.setDuration(50)
                }
                R.id.btnDurationCustom -> {
                    binding.tilCustomDuration.visibility = View.VISIBLE
                    val mins = binding.etCustomDuration.text?.toString()?.toIntOrNull() ?: 30
                    viewModel.setDuration(mins)
                }
            }
        }

        binding.etCustomDuration.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (binding.btnDurationCustom.isChecked) {
                    val mins = s?.toString()?.toIntOrNull() ?: 30
                    viewModel.setDuration(mins)
                }
            }
        })
    }

    private fun setupListeners() {
        binding.btnPrimaryAction.setOnClickListener {
            val state = viewModel.uiState.value.timerState
            when (state) {
                FocusTimerState.IDLE, FocusTimerState.COMPLETED -> viewModel.startTimer()
                FocusTimerState.RUNNING -> viewModel.pauseTimer()
                FocusTimerState.PAUSED -> viewModel.resumeTimer()
            }
        }

        binding.btnResetTimer.setOnClickListener {
            viewModel.resetTimer()
        }

        binding.btnFinishTimer.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Finish Study Session?")
                .setMessage("Your elapsed study time will be saved to your study history and XP will be awarded.")
                .setPositiveButton("Finish") { _, _ ->
                    viewModel.finishEarly()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        binding.btnViewHistory.setOnClickListener {
            findNavController().navigate(R.id.action_focus_to_history)
        }
    }

    private fun observeViewModel() {
        // Observe subject list for dropdown
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allSubjects.collect { subjects ->
                    availableSubjects = subjects
                    val names = subjects.map { it.name }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
                    binding.actvSubjectSelector.setAdapter(adapter)

                    binding.actvSubjectSelector.setOnItemClickListener { _, _, position, _ ->
                        if (position in subjects.indices) {
                            viewModel.selectSubject(subjects[position])
                        }
                    }

                    // Preselect if current subject set
                    val current = viewModel.uiState.value.selectedSubject
                    if (current != null) {
                        binding.actvSubjectSelector.setText(current.name, false)
                    } else if (subjects.isNotEmpty()) {
                        viewModel.selectSubject(subjects.first())
                        binding.actvSubjectSelector.setText(subjects.first().name, false)
                    }
                }
            }
        }

        // Observe UI State
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderTimerState(state)
                }
            }
        }

        // Observe Completion Event
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.completedEvent.collect { event ->
                    showCelebrationDialog(event)
                }
            }
        }
    }

    private fun renderTimerState(state: FocusUiState) {
        val context = requireContext()
        binding.tvTimerDisplay.text = state.formattedTime
        binding.progressTimer.progress = state.progressPercentage

        val subjectName = state.selectedSubject?.name ?: "Focus Study"
        binding.tvTimerSubject.text = subjectName

        when (state.timerState) {
            FocusTimerState.IDLE, FocusTimerState.COMPLETED -> {
                binding.btnPrimaryAction.text = getString(R.string.btn_start)
                binding.btnPrimaryAction.setIconResource(R.drawable.ic_play)
                binding.btnPrimaryAction.setBackgroundColor(ContextCompat.getColor(context, R.color.primary))
                binding.layoutSecondaryActions.visibility = View.GONE
                binding.tilSelectSubject.isEnabled = true
                binding.toggleDurationGroup.isEnabled = true
            }
            FocusTimerState.RUNNING -> {
                binding.btnPrimaryAction.text = getString(R.string.btn_pause)
                binding.btnPrimaryAction.setIconResource(R.drawable.ic_pause)
                binding.btnPrimaryAction.setBackgroundColor(ContextCompat.getColor(context, R.color.secondary))
                binding.layoutSecondaryActions.visibility = View.VISIBLE
                binding.tilSelectSubject.isEnabled = false
                binding.toggleDurationGroup.isEnabled = false
            }
            FocusTimerState.PAUSED -> {
                binding.btnPrimaryAction.text = getString(R.string.btn_resume)
                binding.btnPrimaryAction.setIconResource(R.drawable.ic_play)
                binding.btnPrimaryAction.setBackgroundColor(ContextCompat.getColor(context, R.color.tertiary))
                binding.layoutSecondaryActions.visibility = View.VISIBLE
                binding.tilSelectSubject.isEnabled = false
                binding.toggleDurationGroup.isEnabled = false
            }
        }
    }

    private fun showCelebrationDialog(event: SessionCompletedEvent) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.focus_completed_title)
            .setMessage(getString(R.string.focus_completed_message, event.subjectName, event.minutesStudied, event.xpAwarded))
            .setPositiveButton("Awesome!", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
