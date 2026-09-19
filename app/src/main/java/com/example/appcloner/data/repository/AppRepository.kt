package com.example.appcloner.data.repository

import com.example.appcloner.virtual.VirtualAppManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val virtualAppManager: VirtualAppManager
) {

    fun getClonedApps(): List<String> = virtualAppManager.getClonedApps()

    fun launchApp(packageName: String): Boolean = virtualAppManager.launchVirtualApp(packageName)

    fun uninstallApp(packageName: String): Boolean = virtualAppManager.removeAppFromVirtualEnv(packageName)

    fun getPersonalApps(): List<String> = virtualAppManager.getInstalledPersonalApps()

    fun addAppToVirtualEnv(packageName: String): Boolean = virtualAppManager.addAppToVirtualEnv(packageName)
}
