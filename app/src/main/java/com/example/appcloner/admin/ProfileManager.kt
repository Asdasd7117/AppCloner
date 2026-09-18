package com.example.appcloner.admin

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val adminComponent: ComponentName =
        ComponentName(context, DeviceAdmin::class.java)

    private var lastError: String = ""

    fun getLastError(): String = lastError

    private fun setError(message: String) {
        lastError = message
    }

    private fun clearError() {
        lastError = ""
    }

    private fun getDpm(): DevicePolicyManager {
        return context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    private fun getUserManager(): UserManager {
        return context.getSystemService(Context.USER_SERVICE) as UserManager
    }

    private fun getLauncherApps(): LauncherApps {
        return context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    }

    fun hasWorkProfile(): Boolean {
        return getWorkProfileHandle() != null
    }

    fun getWorkProfileHandle(): UserHandle? {
        return try {
            val userManager = getUserManager()
            val currentUser = Process.myUserHandle()
            userManager.userProfiles.firstOrNull { it != currentUser }
        } catch (e: Exception) {
            null
        }
    }

    fun isProfileOwner(): Boolean {
        return try {
            getDpm().isProfileOwnerApp(context.packageName)
        } catch (e: Exception) {
            false
        }
    }

    fun createWorkProfileIntent(): Intent {
        return Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE).apply {
            putExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                adminComponent
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(
                    DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION,
                    true
                )
            }
        }
    }

    suspend fun installAppInWorkProfile(
        packageName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val workHandle = getWorkProfileHandle()
                ?: throw IllegalStateException("لا يوجد Work Profile مفعل.")

            if (isProfileOwner()) {
                val dpm = getDpm()
                dpm.setApplicationHidden(adminComponent, packageName, false)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    dpm.installExistingPackage(adminComponent, packageName)
                } else {
                    dpm.enableSystemApp(adminComponent, packageName)
                }
            }
            Unit
        }
    }

    fun launchAppInWorkProfile(packageName: String): Boolean {
        clearError()

        return try {
            val workHandle = getWorkProfileHandle()
            if (workHandle == null) {
                setError("❌ فشل الفتح: لا يوجد Work Profile مفعل.")
                return false
            }

            val launcherApps = getLauncherApps()
            val activities = launcherApps.getActivityList(packageName, workHandle)

            if (activities.isNotEmpty()) {
                val mainActivity = activities.first()
                launcherApps.startMainActivity(
                    mainActivity.componentName,
                    workHandle,
                    null,
                    null
                )
                clearError()
                true
            } else {
                setError("❌ التطبيق غير متوفر داخل Work Profile.")
                false
            }
        } catch (e: Exception) {
            setError("❌ خطأ أثناء تشغيل التطبيق: ${e.message ?: e.javaClass.simpleName}")
            false
        }
    }

    fun stopAppInWorkProfile(packageName: String): String {
        return try {
            if (getWorkProfileHandle() == null) return "❌ لا يوجد Work Profile."

            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.killBackgroundProcesses(packageName)
            "✅ تم إرسال طلب إيقاف التطبيق."
        } catch (e: Exception) {
            "❌ فشل إيقاف التطبيق: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    suspend fun uninstallAppFromWorkProfile(packageName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (isProfileOwner()) {
                    val dpm = getDpm()
                    dpm.setApplicationHidden(adminComponent, packageName, true)
                }
                Unit
            }
        }

    fun getWorkProfileApps(): List<String> {
        return try {
            val workHandle = getWorkProfileHandle() ?: return emptyList()
            getLauncherApps()
                .getActivityList(null, workHandle)
                .map { it.applicationInfo.packageName }
                .distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getPersonalApps(): List<String> {
        return try {
            val packageManager = context.packageManager
            packageManager
                .getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { app ->
                    packageManager.getLaunchIntentForPackage(app.packageName) != null &&
                            app.packageName != context.packageName &&
                            !app.packageName.startsWith("android.") &&
                            !app.packageName.startsWith("com.android.")
                }
                .map { it.packageName }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
