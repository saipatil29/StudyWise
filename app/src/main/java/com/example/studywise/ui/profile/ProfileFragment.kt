package com.example.studywise.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.studywise.R
import com.example.studywise.StudyWiseApplication
import com.example.studywise.adapter.BadgeAdapter
import com.example.studywise.databinding.FragmentProfileBinding
import com.example.studywise.ui.common.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        val app = requireActivity().application as StudyWiseApplication
        ViewModelFactory(app.repository)
    }

    private lateinit var badgeAdapter: BadgeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBadges()
        setupListeners()
        observeViewModel()
    }

    private fun setupBadges() {
        badgeAdapter = BadgeAdapter()
        binding.rvBadges.adapter = badgeAdapter
    }

    private fun setupListeners() {
        // Theme chip group
        binding.chipGroupTheme.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            when (checkedIds.first()) {
                R.id.chipThemeLight -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    viewModel.setThemeMode("LIGHT")
                }
                R.id.chipThemeDark -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    viewModel.setThemeMode("DARK")
                }
                R.id.chipThemeSystem -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    viewModel.setThemeMode("SYSTEM")
                }
            }
        }

        // Daily Target Slider
        binding.sliderDailyTarget.addOnChangeListener { _, value, fromUser ->
            binding.tvDailyTargetValue.text = "${value.toInt()}m"
            if (fromUser) {
                viewModel.setDailyGoal(value.toInt())
            }
        }

        // Notifications Switch
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setNotificationsEnabled(isChecked)
        }

        // Load Demo Data Button
        binding.btnLoadDemoData.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Load Realistic Coursework?")
                .setMessage(getString(R.string.load_demo_confirm))
                .setPositiveButton("Load Demo") { _, _ ->
                    val app = requireActivity().application as StudyWiseApplication
                    viewModel.loadDemoData(app.database)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        // Reset Data Button
        binding.btnResetAllData.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Reset All Data?")
                .setMessage(getString(R.string.reset_data_confirm))
                .setPositiveButton("Reset") { _, _ ->
                    viewModel.resetAllData()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderProfile(state)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.toastEvent.collect { msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun renderProfile(state: ProfileUiState) {
        val prefs = state.preferences
        val level = state.levelInfo

        binding.tvProfileName.text = prefs.studentName
        binding.tvProfileEmail.text = prefs.studentEmail

        binding.tvUserLevel.text = getString(R.string.level_format, level.level)
        binding.tvUserXpProgress.text = getString(R.string.xp_format, level.currentLevelXp, level.xpRequiredForNextLevel)
        binding.progressLevelXp.progress = level.progressPercentage

        // Set theme chip without triggering re-entrant event
        when (prefs.themeMode) {
            "LIGHT" -> binding.chipThemeLight.isChecked = true
            "DARK" -> binding.chipThemeDark.isChecked = true
            else -> binding.chipThemeSystem.isChecked = true
        }

        binding.sliderDailyTarget.value = prefs.dailyGoalMinutes.toFloat().coerceIn(30f, 360f)
        binding.tvDailyTargetValue.text = "${prefs.dailyGoalMinutes}m"
        binding.switchNotifications.isChecked = prefs.notificationsEnabled

        badgeAdapter.submitList(state.badges)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
