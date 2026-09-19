package com.example.appcloner.virtual

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VirtualAppManager @Inject constructor(
    private val context: Context
) {

    private val clonedAppsList = mutableSetOf<String>()

    fun addAppToVirtualEnv(packageName: String): Boolean {
        return clonedAppsList.add(packageName)
    }

    fun removeAppFromVirtualEnv(packageName: String): Boolean {
        return clonedAppsList.remove(packageName)
    }

    fun getClonedApps(): List<String> {
        return clonedAppsList.toList()
    }

    fun launchVirtualApp(packageName: String): Boolean {
        return try {
            val intent = Intent(context, StubActivity::class.java).apply {
                putExtra("EXTRA_TARGET_PACKAGE", packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getInstalledPersonalApps(): List<String> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { app ->
                pm.getLaunchIntentForPackage(app.packageName) != null &&
                        app.packageName != context.packageName &&
                        !app.packageName.startsWith("android.") &&
                        !app.packageName.startsWith("com.android.")
            }
            .map { it.packageName }
    }
}
