package com.example.studywise.ui.focus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.studywise.StudyWiseApplication
import com.example.studywise.adapter.StudySessionAdapter
import com.example.studywise.databinding.FragmentStudyHistoryBinding
import com.example.studywise.ui.common.ViewModelFactory
import kotlinx.coroutines.launch

class StudyHistoryFragment : Fragment() {

    private var _binding: FragmentStudyHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FocusViewModel by activityViewModels {
        val app = requireActivity().application as StudyWiseApplication
        ViewModelFactory(app.repository)
    }

    private lateinit var adapter: StudySessionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudyHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = StudySessionAdapter()
        binding.rvStudyHistory.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.studyHistory.collect { list ->
                    adapter.submitList(list)
                    if (list.isEmpty()) {
                        binding.layoutHistoryEmpty.visibility = View.VISIBLE
                        binding.rvStudyHistory.visibility = View.GONE
                        binding.tvHistorySubhead.text = "0 completed sessions"
                    } else {
                        binding.layoutHistoryEmpty.visibility = View.GONE
                        binding.rvStudyHistory.visibility = View.VISIBLE
                        binding.tvHistorySubhead.text = "${list.size} completed study sessions"
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
