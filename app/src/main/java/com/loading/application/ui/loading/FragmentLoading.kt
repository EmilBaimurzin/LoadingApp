package com.loading.application.ui.loading

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.loading.application.R
import com.loading.application.databinding.FragmentLoadingBinding
import com.loading.application.domain.LoadingStatus
import com.loading.application.ui.other.ViewBindingFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class FragmentLoading :
    ViewBindingFragment<FragmentLoadingBinding>(FragmentLoadingBinding::inflate) {
    private val dialog by lazy {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.config)
            .setPositiveButton(R.string._continue) { _, _ ->
                viewModel.loadConfig()
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                viewModel.setErrorState(true)
                Toast.makeText(
                    requireContext(),
                    "Config loading failed, try again",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private val errorToast by lazy {
        Toast.makeText(
            requireContext(),
            "Loading took too long, try again",
            Toast.LENGTH_SHORT
        )
    }

    private val launcher by lazy {
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                if (viewModel.status.value == LoadingStatus.PRE_LOAD) {
                    viewModel.start()
                    viewModel.setErrorState(false)
                    viewModel.downloadConfig {
                        lifecycleScope.launch(Dispatchers.Main) {
                            dialog.show()
                        }
                    }
                }
            } else {
                viewModel.setErrorState(true)
                Toast.makeText(
                    requireContext(),
                    "Allow notifications to continue",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private val viewModel: LoadingViewModel by viewModels()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        start(false)

        viewModel.errorCallback = {
            lifecycleScope.launch(Dispatchers.Main) {
                errorToast.show()
            }
        }

        if (viewModel.status.value == LoadingStatus.LOAD_CONFIG) {
            dialog.show()
        }
        binding.retryButton.setOnClickListener {
            start(true)
        }

        binding.finishButton.setOnClickListener {
            findNavController().navigate(R.id.action_fragmentLoading_to_fragmentMain)
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.progress.collect {
                    binding.progressBar.progress = it
                    binding.percentage.text = "${it}%"
                    if (it == 100) {
                        viewModel.finish()
                    }
                    if (viewModel.status.value == LoadingStatus.LAUNCHING_TASKS) {
                        val doneText = getString(R.string.task_done)
                        val loadingText = getString(R.string.task_loading)
                        binding.status.text =
                            "Task 1: ${if (viewModel.task1Status) doneText else loadingText}\nTask 2: ${if (viewModel.task2Status) doneText else loadingText}\nTask 3: ${if (viewModel.task3Status) doneText else loadingText}"
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorState.collect {
                    binding.apply {
                        progressBar.isVisible = !it
                        percentage.isVisible = !it
                        status.isVisible = !it
                        retryButton.isVisible = it
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.status.collect {
                    val textId = when (it) {
                        LoadingStatus.PRE_LOAD -> null
                        LoadingStatus.DOWNLOAD_CONFIG -> R.string.downloading_config
                        LoadingStatus.LOAD_CONFIG -> null
                        LoadingStatus.LOADING_CONFIG -> R.string.loading_config
                        LoadingStatus.LAUNCHING_TASKS -> R.string.launchingTasks
                        LoadingStatus.FINISH -> R.string.done
                    }
                    val text =
                        when (it) {
                            LoadingStatus.PRE_LOAD, LoadingStatus.LOAD_CONFIG -> ""
                            LoadingStatus.LAUNCHING_TASKS -> ""
                            else -> getString(textId ?: R.string.loading_config)
                        }
                    binding.status.text = text
                    binding.finishButton.isVisible = it == LoadingStatus.FINISH
                }
            }
        }
    }

    private fun start(retry: Boolean) {
        if (checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (viewModel.status.value == LoadingStatus.PRE_LOAD || retry) {
                viewModel.start()
                viewModel.setErrorState(false)
                viewModel.downloadConfig {
                    lifecycleScope.launch(Dispatchers.Main) {
                        dialog.show()
                    }
                }
            }
        } else {
            viewModel.setErrorState(true)
            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                Toast.makeText(
                    requireContext(),
                    "Allow notifications to continue",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}