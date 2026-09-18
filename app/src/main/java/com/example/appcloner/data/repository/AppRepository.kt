package com.example.appcloner.data.repository

import com.example.appcloner.admin.ProfileManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val profileManager: ProfileManager
) {

    fun getInstalledApps(): List<String> {
        return profileManager.getPersonalApps()
    }

    fun getClonedApps(): List<String> {
        return profileManager.getWorkProfileApps()
    }

    suspend fun installApp(packageName: String): Result<Unit> {
        return profileManager.installAppInWorkProfile(packageName)
    }

    suspend fun launchApp(packageName: String): Boolean {
        return profileManager.launchAppInWorkProfile(packageName)
    }

    fun stopApp(packageName: String): String {
        return profileManager.stopAppInWorkProfile(packageName)
    }

    suspend fun uninstallApp(packageName: String): Result<Unit> {
        return profileManager.uninstallAppFromWorkProfile(packageName)
    }

    fun getLastError(): String {
        return profileManager.getLastError()
    }
}
