package com.example.appcloner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcloner.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _clonedApps = MutableStateFlow<List<String>>(emptyList())
    val clonedApps: StateFlow<List<String>> = _clonedApps.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadClonedApps()
    }

    fun loadClonedApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = repository.getClonedApps()
            withContext(Dispatchers.Main) {
                _clonedApps.value = apps
            }
        }
    }

    fun launchApp(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.launchApp(packageName)
            if (!success) {
                val err = repository.getLastError()
                withContext(Dispatchers.Main) {
                    _errorMessage.value = if (err.isEmpty()) "فشل تشغيل التطبيق." else err
                }
            }
        }
    }

    fun stopApp(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.stopApp(packageName)
        }
    }

    fun removeApp(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.uninstallApp(packageName)
            loadClonedApps()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
