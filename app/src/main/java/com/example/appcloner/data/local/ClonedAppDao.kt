package com.example.appcloner.data.repository

import com.example.appcloner.data.local.ClonedAppDao
import com.example.appcloner.data.local.ClonedAppEntity
import com.example.appcloner.domain.model.AppInfo
import com.example.appcloner.virtual.VirtualAppManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val virtualAppManager: VirtualAppManager,
    private val clonedAppDao: ClonedAppDao
) {

    // جلب القائمة مباشرة من قاعدة البيانات وتحديث UI تلقائياً عبر Flow
    fun getClonedAppsFlow(): Flow<List<AppInfo>> {
        return clonedAppDao.getAllClonedApps().map { entities ->
            entities.map { entity ->
                virtualAppManager.getAppInfoForPackage(entity.packageName)
                    ?: AppInfo(
                        packageName = entity.packageName,
                        label = entity.appName,
                        icon = null
                    )
            }
        }
    }

    suspend fun addAppToVirtualEnv(packageName: String, appName: String) {
        clonedAppDao.insert(ClonedAppEntity(packageName = packageName, appName = appName))
        virtualAppManager.addAppToVirtualEnv(packageName)
    }

    suspend fun uninstallApp(packageName: String) {
        clonedAppDao.deleteByPackage(packageName)
        virtualAppManager.removeAppFromVirtualEnv(packageName)
    }

    fun launchApp(packageName: String): Boolean = virtualAppManager.launchVirtualApp(packageName)

    fun getPersonalApps(): List<AppInfo> = virtualAppManager.getInstalledPersonalAppsInfo()
}
