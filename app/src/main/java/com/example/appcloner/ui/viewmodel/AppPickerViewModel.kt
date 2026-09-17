package com.example.appcloner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcloner.data.repository.AppRepository
import com.example.appcloner.domain.model.AppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppPickerViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _availableApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val availableApps: StateFlow<List<AppInfo>> = _availableApps

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init { loadApps() }

    private fun loadApps() {
        viewModelScope.launch {
            _availableApps.value = repository.getPersonalApps()
        }
    }

    fun onQueryChange(q: String) {
        _searchQuery.value = q
        viewModelScope.launch {
            val all = repository.getPersonalApps()
            _availableApps.value = if (q.isBlank()) all
            else all.filter {
                it.label.contains(q, ignoreCase = true) ||
                it.packageName.contains(q, ignoreCase = true)
            }
        }
    }

    fun addApp(packageName: String) {
        viewModelScope.launch { repository.addApp(packageName) }
    }
}