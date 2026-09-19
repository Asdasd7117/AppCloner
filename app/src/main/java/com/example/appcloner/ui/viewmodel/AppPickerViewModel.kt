package com.example.appcloner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcloner.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppPickerViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _availableApps = MutableStateFlow<List<String>>(emptyList())
    val availableApps: StateFlow<List<String>> = _availableApps.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _availableApps.value = repository.getPersonalApps()
        }
    }

    fun addApp(packageName: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.addAppToVirtualEnv(packageName)
            onComplete()
        }
    }
}
