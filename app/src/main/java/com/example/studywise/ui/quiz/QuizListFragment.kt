package com.example.studywise.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.studywise.R
import com.example.studywise.StudyWiseApplication
import com.example.studywise.adapter.QuizAdapter
import com.example.studywise.databinding.FragmentQuizListBinding
import com.example.studywise.ui.common.ViewModelFactory
import kotlinx.coroutines.launch

class QuizListFragment : Fragment() {

    private var _binding: FragmentQuizListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuizViewModel by activityViewModels {
        val app = requireActivity().application as StudyWiseApplication
        ViewModelFactory(app.repository)
    }

    private lateinit var quizAdapter: QuizAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        quizAdapter = QuizAdapter { quiz ->
            val bundle = Bundle().apply {
                putLong("quizId", quiz.id)
            }
            findNavController().navigate(R.id.action_quiz_list_to_attempt, bundle)
        }
        binding.rvQuizzes.adapter = quizAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.quizList.collect { list ->
                    quizAdapter.submitList(list)
                    if (list.isEmpty()) {
                        binding.layoutQuizEmpty.visibility = View.VISIBLE
                        binding.rvQuizzes.visibility = View.GONE
                        binding.tvQuizSubhead.text = "0 quizzes available"
                    } else {
                        binding.layoutQuizEmpty.visibility = View.GONE
                        binding.rvQuizzes.visibility = View.VISIBLE
                        binding.tvQuizSubhead.text = "${list.size} practice quizzes available"
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
