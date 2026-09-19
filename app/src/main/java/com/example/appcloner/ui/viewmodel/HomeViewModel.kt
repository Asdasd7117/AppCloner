package com.example.appcloner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcloner.data.repository.AppRepository
import com.example.appcloner.domain.model.AppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    // التحديث الفوري للقائمة عبر Flow دون الحاجة لاستدعاء يدوي
    val clonedApps: StateFlow<List<AppInfo>> = repository.getClonedAppsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun launchApp(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.launchApp(packageName)
        }
    }

    fun removeApp(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.uninstallApp(packageName)
        }
    }
}
