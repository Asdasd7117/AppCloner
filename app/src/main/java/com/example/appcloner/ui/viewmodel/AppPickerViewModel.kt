package com.example.appcloner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcloner.data.repository.AppRepository
import com.example.appcloner.domain.model.AppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppPickerViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _availableApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val availableApps: StateFlow<List<AppInfo>> = _availableApps.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _availableApps.value = repository.getPersonalApps()
        }
    }

    fun addApp(appInfo: AppInfo, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addAppToVirtualEnv(appInfo.packageName, appInfo.label)
            onComplete()
        }
    }
}
