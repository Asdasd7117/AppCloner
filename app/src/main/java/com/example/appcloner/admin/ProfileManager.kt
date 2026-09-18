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
import kotlinx.coroutines.delay
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

    private fun getDpm(): DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private fun getUserManager(): UserManager =
        context.getSystemService(Context.USER_SERVICE) as UserManager

    private fun getLauncherApps(): LauncherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

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

    /**
     * تثبيت التطبيق بإرسال بث لنسخة الـ Work Profile إذا لم نكن نحن الـ Owner
     */
    suspend fun installAppInWorkProfile(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val workHandle = getWorkProfileHandle()
                ?: throw IllegalStateException("لا يوجد Work Profile مفعل.")

            if (isProfileOwner()) {
                // إذا كنا نعمل من داخل الـ Work Profile مباشرة
                val dpm = getDpm()
                dpm.setApplicationHidden(adminComponent, packageName, false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    dpm.installExistingPackage(adminComponent, packageName)
                } else {
                    dpm.enableSystemApp(adminComponent, packageName)
                }
            } else {
                // إذا كنا في البروفايل الشخصي، نرسل إشارة للنسخة الموجودة في الـ Work Profile
                val intent = Intent(WorkProfileReceiver.ACTION_INSTALL_APP).apply {
                    setPackage(context.packageName)
                    putExtra("EXTRA_PACKAGE_NAME", packageName)
                }
                context.sendBroadcastAsUser(intent, workHandle)
                
                // انتظار بسيط للتأكد من إتمام التثبيت قبل العودة
                delay(1000)
            }
            Unit
        }
    }

    suspend fun launchAppInWorkProfile(packageName: String): Boolean = withContext(Dispatchers.IO) {
        clearError()
        try {
            val workHandle = getWorkProfileHandle()
            if (workHandle == null) {
                setError("❌ لا يوجد Work Profile مفعل.")
                return@withContext false
            }

            val launcherApps = getLauncherApps()
            var activities = launcherApps.getActivityList(packageName, workHandle)

            if (activities.isEmpty()) {
                // محاولة تثبيت إضافية فورية
                installAppInWorkProfile(packageName)
                delay(800)
                activities = launcherApps.getActivityList(packageName, workHandle)
            }

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
                setError("❌ فشل إظهار التطبيق داخل Work Profile.")
                false
            }
        } catch (e: Exception) {
            setError("❌ خطأ أثناء التشغيل: ${e.message}")
            false
        }
    }

    fun stopAppInWorkProfile(packageName: String): String {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.killBackgroundProcesses(packageName)
            "✅ تم الإيقاف"
        } catch (e: Exception) {
            "❌ فشل الإيقاف: ${e.message}"
        }
    }

    suspend fun uninstallAppFromWorkProfile(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val workHandle = getWorkProfileHandle() ?: return@runCatching
            if (isProfileOwner()) {
                getDpm().setApplicationHidden(adminComponent, packageName, true)
            } else {
                val intent = Intent(WorkProfileReceiver.ACTION_UNINSTALL_APP).apply {
                    setPackage(context.packageName)
                    putExtra("EXTRA_PACKAGE_NAME", packageName)
                }
                context.sendBroadcastAsUser(intent, workHandle)
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
                .filter { it != context.packageName }
                .distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getPersonalApps(): List<String> {
        return try {
            val pm = context.packageManager
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { app ->
                    pm.getLaunchIntentForPackage(app.packageName) != null &&
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
