package com.example.appcloner.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.example.appcloner.admin.ProfileManager
import com.example.appcloner.data.local.ClonedAppDao
import com.example.appcloner.data.local.ClonedAppEntity
import com.example.appcloner.domain.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ClonedAppDao,
    private val profileManager: ProfileManager
) {
    private val pm: PackageManager = context.packageManager

    fun getClonedApps(): Flow<List<ClonedAppEntity>> = dao.getAllClonedApps()

    suspend fun getPersonalApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { app ->
                pm.getLaunchIntentForPackage(app.packageName) != null &&
                app.packageName != context.packageName
            }
            .map { it.toAppInfo() }
            .sortedBy { it.label.lowercase() }
    }

    suspend fun addApp(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val app = pm.getApplicationInfo(packageName, 0)
            dao.insert(
                ClonedAppEntity(
                    packageName = packageName,
                    label = pm.getApplicationLabel(app).toString()
                )
            )
            // محاولة تثبيت التطبيق في Work Profile
            profileManager.installAppInWorkProfile(packageName)
        }
    }

    suspend fun removeApp(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            profileManager.uninstallAppFromWorkProfile(packageName)
            dao.deleteByPackage(packageName)
        }
    }

    fun launchApp(packageName: String): Boolean {
        return profileManager.launchAppInWorkProfile(packageName)
    }

    fun stopApp(packageName: String) {
        profileManager.stopAppInWorkProfile(packageName)
    }

    private fun ApplicationInfo.toAppInfo(): AppInfo {
        val label = pm.getApplicationLabel(this).toString()
        val icon: Drawable? = try {
            pm.getApplicationIcon(this)
        } catch (e: Exception) { null }
        val version = try {
            pm.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (e: Exception) { "" }
        return AppInfo(
            packageName = packageName,
            label = label,
            icon = icon,
            versionName = version,
            isSystemApp = (flags and ApplicationInfo.FLAG_SYSTEM) != 0
        )
    }
}