package com.example.appcloner.data.repository

import com.example.appcloner.admin.ProfileManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val profileManager: ProfileManager
) {

    fun hasWorkProfile(): Boolean {
        return profileManager.hasWorkProfile()
    }

    suspend fun installAppInWorkProfile(packageName: String): Result<Unit> {
        return profileManager.installAppInWorkProfile(packageName)
    }

    /**
     * ترجع Boolean لترتبط بشكل صحيح مع السطر 63 بدون Return type mismatch
     */
    fun launchAppInWorkProfile(packageName: String): Boolean {
        return profileManager.launchAppInWorkProfile(packageName).isSuccess
    }

    fun stopAppInWorkProfile(packageName: String) {
        profileManager.stopAppInWorkProfile(packageName)
    }

    suspend fun uninstallAppFromWorkProfile(packageName: String): Result<Unit> {
        return profileManager.uninstallAppFromWorkProfile(packageName)
    }

    suspend fun removeWorkProfile(): Result<Unit> {
        return profileManager.removeWorkProfile()
    }

    fun getWorkProfileApps(): List<String> {
        return profileManager.getWorkProfileApps()
    }

    fun getPersonalApps(): List<String> {
        return profileManager.getPersonalApps()
    }
}
