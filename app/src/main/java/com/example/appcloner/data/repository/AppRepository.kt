package com.example.appcloner.data.repository

import com.example.appcloner.admin.ProfileManager
import com.example.appcloner.data.local.ClonedAppDao
import com.example.appcloner.data.local.ClonedAppEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val profileManager: ProfileManager,
    private val clonedAppDao: ClonedAppDao
) {

    val allClonedApps: Flow<List<ClonedAppEntity>> = clonedAppDao.getAllApps()

    suspend fun launchApp(packageName: String): Boolean {
        return profileManager.launchAppInWorkProfile(packageName)
    }

    suspend fun installApp(packageName: String): Result<Unit> {
        val result = profileManager.installAppInWorkProfile(packageName)
        if (result.isSuccess) {
            clonedAppDao.insertApp(
                ClonedAppEntity(
                    packageName = packageName,
                    appName = packageName,
                    isCloned = true
                )
            )
        }
        return result
    }

    suspend fun uninstallApp(packageName: String): Result<Unit> {
        val result = profileManager.uninstallAppFromWorkProfile(packageName)
        if (result.isSuccess) {
            clonedAppDao.deleteAppByPackage(packageName)
        }
        return result
    }

    fun getInstalledApps(): List<String> {
        return profileManager.getPersonalApps()
    }

    fun getClonedApps(): List<String> {
        return profileManager.getWorkProfileApps()
    }

    fun getLastError(): String {
        return profileManager.getLastError()
    }
}
