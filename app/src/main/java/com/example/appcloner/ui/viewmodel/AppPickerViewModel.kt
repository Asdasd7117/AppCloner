package com.example.appcloner.ui.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcloner.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null
)

@HiltViewModel
class AppPickerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AppRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _availableApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val availableApps: StateFlow<List<AppInfo>> = _availableApps.asStateFlow()

    init {
        loadApps()
    }

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val packageNames = repository.getInstalledApps()

            val appsList = packageNames.mapNotNull { pkgName ->
                try {
                    val appInfo = pm.getApplicationInfo(pkgName, 0)
                    AppInfo(
                        packageName = pkgName,
                        label = pm.getApplicationLabel(appInfo).toString(),
                        icon = pm.getApplicationIcon(appInfo)
                    )
                } catch (e: Exception) {
                    null
                }
            }

            withContext(Dispatchers.Main) {
                _availableApps.value = appsList
            }
        }
    }

    fun addApp(packageName: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.installApp(packageName)
            onComplete()
        }
    }
}
