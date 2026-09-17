package com.example.appcloner.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.example.appcloner.admin.ProfileManager
import com.example.appcloner.domain.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
     * إرجاع التطبيقات المنسوخة كـ Flow<List<AppInfo>> لتتوافق مع HomeViewModel و stateIn
     */
    fun getClonedApps(): Flow<List<AppInfo>> = flow {
        val packageNames = profileManager.getWorkProfileApps()
        val apps = packageNames.map { pkg -> createAppInfo(pkg) }
        emit(apps)
    }

    /**
     * إرجاع التطبيقات الشخصية كـ List<AppInfo>
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
            val pkgInfo = pm.getPackageInfo(packageName, 0)
            val label = pm.getApplicationLabel(appInfo).toString()
            val icon = pm.getApplicationIcon(appInfo)
            val versionName = pkgInfo.versionName ?: "1.0.0"
            
            AppInfo(
                packageName = packageName,
                label = label,
                icon = icon,
                versionName = versionName
            )
        } catch (e: Exception) {
            AppInfo(
                packageName = packageName,
                label = packageName,
                icon = null,
                versionName = "1.0.0"
            )
        }
    }
}
