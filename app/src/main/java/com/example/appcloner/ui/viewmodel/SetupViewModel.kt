package com.example.appcloner.ui.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.example.appcloner.admin.ProfileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val profileManager: ProfileManager
) : ViewModel() {

    private val _hasWorkProfile = MutableStateFlow(false)
    val hasWorkProfile: StateFlow<Boolean> = _hasWorkProfile.asStateFlow()

    init {
        checkWorkProfile()
    }

    fun checkWorkProfile() {
        _hasWorkProfile.value = profileManager.hasWorkProfile()
    }

    fun createWorkProfileIntent(): Intent {
        return profileManager.createWorkProfileIntent()
    }
}
