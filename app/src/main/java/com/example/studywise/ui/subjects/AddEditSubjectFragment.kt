package com.example.studywise.ui.subjects

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.studywise.R
import com.example.studywise.StudyWiseApplication
import com.example.studywise.data.entity.Subject
import com.example.studywise.databinding.FragmentAddEditSubjectBinding
import com.example.studywise.utils.DateTimeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Calendar

class AddEditSubjectFragment : Fragment() {

    private var _binding: FragmentAddEditSubjectBinding? = null
    private val binding get() = _binding!!

    private var subjectId: Long = -1L
    private var selectedExamDateMillis: Long = 0L
    private var existingSubject: Subject? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditSubjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subjectId = arguments?.getLong("subjectId", -1L) ?: -1L

        setupDatePicker()
        setupSlider()
        setupListeners()

        if (subjectId != -1L) {
            loadExistingSubject(subjectId)
        } else {
            // Default exam date to 7 days from now
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
            selectedExamDateMillis = cal.timeInMillis
            binding.etExamDate.setText(DateTimeUtils.formatDate(selectedExamDateMillis))
        }
    }

    private fun setupDatePicker() {
        binding.etExamDate.setOnClickListener {
            val cal = Calendar.getInstance()
            if (selectedExamDateMillis > 0) {
                cal.timeInMillis = selectedExamDateMillis
            }

            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                    }
                    selectedExamDateMillis = selectedCal.timeInMillis
                    binding.etExamDate.setText(DateTimeUtils.formatDate(selectedExamDateMillis))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupSlider() {
        binding.sliderPreparation.addOnChangeListener { _, value, _ ->
            binding.tvPrepSliderValue.text = "${value.toInt()}%"
        }
        binding.tvPrepSliderValue.text = "${binding.sliderPreparation.value.toInt()}%"
    }

    private fun setupListeners() {
        binding.btnSaveSubject.setOnClickListener {
            validateAndSave()
        }

        binding.btnDeleteSubject.setOnClickListener {
            confirmDelete()
        }
    }

    private fun loadExistingSubject(id: Long) {
        val app = requireActivity().application as StudyWiseApplication
        viewLifecycleOwner.lifecycleScope.launch {
            val subject = app.repository.getSubjectById(id) ?: return@launch
            existingSubject = subject

            binding.tvAddEditTitle.text = getString(R.string.edit_subject)
            binding.etSubjectName.setText(subject.name)
            selectedExamDateMillis = subject.examDate
            binding.etExamDate.setText(DateTimeUtils.formatDate(subject.examDate))
            binding.sliderPreparation.value = subject.preparationPercentage.toFloat()
            binding.tvPrepSliderValue.text = "${subject.preparationPercentage}%"
            binding.etDailyGoal.setText(subject.dailyGoalMinutes.toString())
            binding.etDescription.setText(subject.description)

            when (subject.difficulty.uppercase()) {
                "EASY" -> binding.chipEasy.isChecked = true
                "HARD" -> binding.chipHard.isChecked = true
                else -> binding.chipMedium.isChecked = true
            }

            binding.btnDeleteSubject.visibility = View.VISIBLE
        }
    }

    private fun validateAndSave() {
        val name = binding.etSubjectName.text?.toString()?.trim() ?: ""
        if (name.isEmpty()) {
            binding.tilSubjectName.error = getString(R.string.error_name_required)
            return
        } else {
            binding.tilSubjectName.error = null
        }

        if (selectedExamDateMillis == 0L) {
            binding.tilExamDate.error = getString(R.string.error_valid_date)
            return
        } else {
            binding.tilExamDate.error = null
        }

        val dailyGoalStr = binding.etDailyGoal.text?.toString()?.trim() ?: "45"
        val dailyGoal = dailyGoalStr.toIntOrNull() ?: 45
        if (dailyGoal <= 0) {
            binding.tilDailyGoal.error = getString(R.string.error_valid_goal)
            return
        } else {
            binding.tilDailyGoal.error = null
        }

        val prep = binding.sliderPreparation.value.toInt().coerceIn(0, 100)
        val description = binding.etDescription.text?.toString()?.trim() ?: ""

        val difficulty = when {
            binding.chipEasy.isChecked -> "EASY"
            binding.chipHard.isChecked -> "HARD"
            else -> "MEDIUM"
        }

        val app = requireActivity().application as StudyWiseApplication
        viewLifecycleOwner.lifecycleScope.launch {
            if (subjectId == -1L) {
                // Create new
                val newSubject = Subject(
                    name = name,
                    examDate = selectedExamDateMillis,
                    preparationPercentage = prep,
                    difficulty = difficulty,
                    dailyGoalMinutes = dailyGoal,
                    description = description
                )
                app.repository.insertSubject(newSubject)
                Toast.makeText(requireContext(), "Subject added successfully!", Toast.LENGTH_SHORT).show()
            } else {
                // Update
                val existing = existingSubject ?: app.repository.getSubjectById(subjectId)
                val updated = if (existing != null) {
                    existing.copy(
                        name = name,
                        examDate = selectedExamDateMillis,
                        preparationPercentage = prep,
                        difficulty = difficulty,
                        dailyGoalMinutes = dailyGoal,
                        description = description,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    Subject(
                        id = subjectId,
                        name = name,
                        examDate = selectedExamDateMillis,
                        preparationPercentage = prep,
                        difficulty = difficulty,
                        dailyGoalMinutes = dailyGoal,
                        description = description,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                app.repository.updateSubject(updated)
                Toast.makeText(requireContext(), "Subject updated successfully!", Toast.LENGTH_SHORT).show()
            }
            findNavController().popBackStack()
        }
    }

    private fun confirmDelete() {
        val app = requireActivity().application as StudyWiseApplication
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Subject?")
            .setMessage("Are you sure you want to delete this subject?")
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val subject = existingSubject ?: app.repository.getSubjectById(subjectId)
                    if (subject != null) {
                        app.repository.deleteSubject(subject)
                    } else if (subjectId != -1L) {
                        app.repository.deleteSubjectById(subjectId)
                    }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
