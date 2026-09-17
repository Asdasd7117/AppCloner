package com.example.appcloner.admin

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

    private fun getDpm(): DevicePolicyManager {
        return context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    private fun getUserManager(): UserManager {
        return context.getSystemService(Context.USER_SERVICE) as UserManager
    }

    fun hasWorkProfile(): Boolean {
        return getWorkProfileHandle() != null
    }

    fun getWorkProfileHandle(): UserHandle? {
        val um = getUserManager()
        val myUser = Process.myUserHandle()
        return um.userProfiles.firstOrNull { it != myUser }
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

    suspend fun installAppInWorkProfile(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            getWorkProfileHandle() ?: throw IllegalStateException("لم يتم العثور على Work Profile")
            val dpm = getDpm()
            dpm.setApplicationHidden(adminComponent, packageName, false)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dpm.installExistingPackage(adminComponent, packageName)
            } else {
                dpm.enableSystemApp(adminComponent, packageName)
            }
            Unit
        }
    }

    /**
     * تشغيل التطبيق وإرجاع نتيجة تفصيلية أو سبب الفشل الدقيق
     */
    fun launchAppInWorkProfile(packageName: String): Result<Unit> {
        val workHandle = getWorkProfileHandle()
            ?: return Result.failure(Exception("خطأ: لم يتم إنشاء البيئة المعزولة (Work Profile) بعد."))

        val dpm = getDpm()
        var lastException: Exception? = null

        // محاولة تثبيت/تمكين الحزمة
        try {
            dpm.setApplicationHidden(adminComponent, packageName, false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dpm.installExistingPackage(adminComponent, packageName)
            } else {
                dpm.enableSystemApp(adminComponent, packageName)
            }
        } catch (e: Exception) {
            lastException = e
        }

        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        var activityList = launcherApps.getActivityList(packageName, workHandle)

        // محاولة إعادة الفحص في حال استغرق النظام وقتاً
        var attempts = 0
        while (activityList.isEmpty() && attempts < 3) {
            try { Thread.sleep(200) } catch (_: Exception) {}
            activityList = launcherApps.getActivityList(packageName, workHandle)
            attempts++
        }

        val activityInfo = activityList.firstOrNull()
            ?: return Result.failure(
                Exception(
                    "فشل الفتح: التطبيق غير مثبت داخل الـ Work Profile، والتطبيق الحالي لا يملك صلاحية Profile Owner داخل العزل لتثبيته تلقائياً.\nالسبب: ${lastException?.localizedMessage ?: "عدم وجود صلاحيات كافية"}"
                )
            )

        return try {
            launcherApps.startMainActivity(
                activityInfo.componentName,
                workHandle,
                null,
                null
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("خطأ أثناء تشغيل الواجهة: ${e.localizedMessage}"))
        }
    }

    fun stopAppInWorkProfile(packageName: String) {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            am.killBackgroundProcesses(packageName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun uninstallAppFromWorkProfile(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            getWorkProfileHandle() ?: throw IllegalStateException("لم يتم العثور على Work Profile")
            getDpm().setApplicationHidden(adminComponent, packageName, true)
            Unit
        }
    }

    suspend fun removeWorkProfile(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val dpm = getDpm()
            if (dpm.isProfileOwnerApp(context.packageName)) {
                dpm.clearProfileOwner(adminComponent)
            }
            Unit
        }
    }

    fun getWorkProfileApps(): List<String> {
        val workHandle = getWorkProfileHandle() ?: return emptyList()
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        return launcherApps.getActivityList(null, workHandle)
            .map { it.applicationInfo.packageName }
            .distinct()
    }

    fun getPersonalApps(): List<String> {
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
