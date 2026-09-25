package com.example.studywise.ui.subjects

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import android.widget.Toast
import com.example.studywise.R
import com.example.studywise.StudyWiseApplication
import com.example.studywise.adapter.SubjectAdapter
import com.example.studywise.data.entity.Subject
import com.example.studywise.databinding.FragmentSubjectsBinding
import com.example.studywise.ui.common.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SubjectsFragment : Fragment() {

    private var _binding: FragmentSubjectsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SubjectsViewModel by viewModels {
        val app = requireActivity().application as StudyWiseApplication
        ViewModelFactory(app.repository)
    }

    private lateinit var subjectAdapter: SubjectAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubjectsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        subjectAdapter = SubjectAdapter(
            onItemClick = { subject ->
                val bundle = Bundle().apply {
                    putLong("subjectId", subject.id)
                }
                findNavController().navigate(R.id.action_subjects_to_details, bundle)
            },
            onEditClick = { subject ->
                val bundle = Bundle().apply {
                    putLong("subjectId", subject.id)
                }
                findNavController().navigate(R.id.action_subjects_to_add_edit, bundle)
            },
            onDeleteClick = { subject ->
                showDeleteConfirmationDialog(subject)
            }
        )
        binding.rvSubjects.adapter = subjectAdapter
    }

    private fun showDeleteConfirmationDialog(subject: Subject) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Subject?")
            .setMessage("Are you sure you want to delete this subject?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteSubject(subject)
                Toast.makeText(requireContext(), "Subject deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupListeners() {
        binding.fabAddSubject.setOnClickListener {
            findNavController().navigate(R.id.action_subjects_to_add_edit)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.subjects.collect { list ->
                    subjectAdapter.submitList(list)
                    if (list.isEmpty()) {
                        binding.layoutSubjectsEmpty.visibility = View.VISIBLE
                        binding.rvSubjects.visibility = View.GONE
                        binding.tvSubjectsSubhead.text = "0 enrolled subjects"
                    } else {
                        binding.layoutSubjectsEmpty.visibility = View.GONE
                        binding.rvSubjects.visibility = View.VISIBLE
                        binding.tvSubjectsSubhead.text = "${list.size} enrolled subjects • Ranked by smart priority"
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
