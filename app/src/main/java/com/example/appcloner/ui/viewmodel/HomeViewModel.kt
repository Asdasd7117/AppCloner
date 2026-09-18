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

    /**
     * إعادة تحميل التطبيقات المثبتة داخل Work Profile مع التنفيذ في IO Dispatcher
     */
    fun loadClonedApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = repository.getClonedApps()
            withContext(Dispatchers.Main) {
                _clonedApps.value = apps
            }
        }
    }

    /**
     * تشغيل التطبيق المنسوخ مع معالجة الأخطاء
     */
    fun launchApp(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.launchApp(packageName)
            if (!success) {
                val error = repository.getLastError()
                withContext(Dispatchers.Main) {
                    _errorMessage.value = error.ifEmpty { "فشل تشغيل التطبيق." }
                }
            }
        }
    }

    /**
     * إيقاف العمليات في الخلفية للتطبيق
     */
    fun stopApp(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val resultMessage = repository.stopApp(packageName)
            // يمكن الاستفادة من الرسالة إذا كنت تعرض Toast أو SnackBar
        }
    }

    /**
     * إزالة/إخفاء التطبيق المنسوخ وإعادة جلب القائمة
     */
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
