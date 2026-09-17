package com.example.appcloner.ui.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcloner.data.local.ClonedAppEntity
import com.example.appcloner.data.repository.AppRepository
import com.example.appcloner.domain.model.AppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AppRepository
) : ViewModel() {

    private val pm: PackageManager = context.packageManager

    // تم التغيير إلى StateFlow لضمان عدم طلب initial في الشاشة
    val clonedApps: StateFlow<List<AppInfo>> = repository.getClonedApps()
        .map { entities -> entities.map { it.toAppInfo() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    fun launchApp(packageName: String) {
        repository.launchApp(packageName)
    }

    fun stopApp(packageName: String) {
        repository.stopApp(packageName)
    }

    fun removeApp(packageName: String) {
        viewModelScope.launch { repository.removeApp(packageName) }
    }

    private fun ClonedAppEntity.toAppInfo(): AppInfo {
        val appInfo = try {
            pm.getApplicationInfo(packageName, 0)
        } catch (e: Exception) { null }
        
        val icon: Drawable? = try {
            appInfo?.let { pm.getApplicationIcon(it) }
        } catch (e: Exception) { null }
        
        return AppInfo(
            packageName = packageName,
            label = label,
            icon = icon,
            versionName = ""
        )
    }
}
