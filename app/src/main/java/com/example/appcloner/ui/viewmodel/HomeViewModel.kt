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
class HomeViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _clonedApps = MutableStateFlow<List<String>>(emptyList())
    val clonedApps: StateFlow<List<String>> = _clonedApps.asStateFlow()

    init {
        loadClonedApps()
    }

    fun loadClonedApps() {
        viewModelScope.launch {
            _clonedApps.value = repository.getClonedApps()
        }
    }

    fun launchApp(packageName: String) {
        viewModelScope.launch {
            repository.launchApp(packageName)
        }
    }

    fun stopApp(packageName: String) {
        viewModelScope.launch {
            // إيقاف التطبيق عبر الـ Repository
        }
    }

    fun removeApp(packageName: String) {
        viewModelScope.launch {
            repository.uninstallApp(packageName)
            loadClonedApps()
        }
    }
}
