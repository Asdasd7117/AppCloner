package com.example.appcloner.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.example.appcloner.admin.ProfileManager
import com.example.appcloner.domain.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileManager: ProfileManager
) {

    fun hasWorkProfile(): Boolean {
        return profileManager.hasWorkProfile()
    }

    /**
     * جلب التطبيقات المنسوخة كـ List<AppInfo>
     */
    fun getClonedApps(): List<AppInfo> {
        val packageNames = profileManager.getWorkProfileApps()
        return packageNames.map { pkg -> createAppInfo(pkg) }
    }

    /**
     * جلب التطبيقات الشخصية كـ List<AppInfo>
     */
    fun getPersonalApps(): List<AppInfo> {
        val packageNames = profileManager.getPersonalApps()
        return packageNames.map { pkg -> createAppInfo(pkg) }
    }

    suspend fun addApp(packageName: String): Result<Unit> {
        return profileManager.installAppInWorkProfile(packageName)
    }

    fun launchApp(packageName: String): Boolean {
        return profileManager.launchAppInWorkProfile(packageName).isSuccess
    }

    fun stopApp(packageName: String) {
        profileManager.stopAppInWorkProfile(packageName)
    }

    suspend fun removeApp(packageName: String): Result<Unit> {
        return profileManager.uninstallAppFromWorkProfile(packageName)
    }

    suspend fun installAppInWorkProfile(packageName: String): Result<Unit> {
        return profileManager.installAppInWorkProfile(packageName)
    }

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

    private fun createAppInfo(packageName: String): AppInfo {
        val pm = context.packageManager
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val label = pm.getApplicationLabel(appInfo).toString()
            val icon = pm.getApplicationIcon(appInfo)
            AppInfo(
                packageName = packageName,
                label = label,
                icon = icon
            )
        } catch (e: Exception) {
            AppInfo(
                packageName = packageName,
                label = packageName,
                icon = null
            )
        }
    }
}
