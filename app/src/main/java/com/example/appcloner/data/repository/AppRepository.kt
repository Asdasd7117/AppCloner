package com.example.appcloner.data.repository

import com.example.appcloner.admin.ProfileManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val profileManager: ProfileManager
) {

    /**
     * تم إضافة suspend لجميع الدوال التي تستدعي ProfileManager لضمان عدم حدوث خطأ الكومبايلر
     */
    suspend fun launchApp(packageName: String): Boolean {
        return profileManager.launchAppInWorkProfile(packageName)
    }

    suspend fun installApp(packageName: String): Result<Unit> {
        return profileManager.installAppInWorkProfile(packageName)
    }

    suspend fun uninstallApp(packageName: String): Result<Unit> {
        return profileManager.uninstallAppFromWorkProfile(packageName)
    }

    fun getInstalledApps(): List<String> {
        return profileManager.getPersonalApps()
    }

    fun getClonedApps(): List<String> {
        return profileManager.getWorkProfileApps()
    }
}
