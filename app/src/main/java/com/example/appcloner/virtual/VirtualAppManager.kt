package com.example.appcloner.virtual

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.appcloner.domain.model.AppInfo
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

    // الدالة التي كانت مفقودة وتسببت في خطأ KSP
    fun getAppInfoForPackage(packageName: String): AppInfo? {
        val pm = context.packageManager
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            AppInfo(
                packageName = packageName,
                label = pm.getApplicationLabel(appInfo).toString(),
                icon = pm.getApplicationIcon(appInfo)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getClonedAppsInfo(): List<AppInfo> {
        return clonedAppsList.mapNotNull { pkg ->
            getAppInfoForPackage(pkg)
        }
    }

    fun getInstalledPersonalAppsInfo(): List<AppInfo> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { app ->
                pm.getLaunchIntentForPackage(app.packageName) != null &&
                        app.packageName != context.packageName &&
                        !app.packageName.startsWith("android.") &&
                        !app.packageName.startsWith("com.android.")
            }
            .map { app ->
                AppInfo(
                    packageName = app.packageName,
                    label = pm.getApplicationLabel(app).toString(),
                    icon = pm.getApplicationIcon(app)
                )
            }
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
}
