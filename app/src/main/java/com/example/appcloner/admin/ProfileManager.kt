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

    /**
     * هل يوجد Work Profile مُنشأ حالياً؟
     */
    fun hasWorkProfile(): Boolean {
        return getWorkProfileHandle() != null
    }

    /**
     * الحصول على UserHandle للـ Work Profile
     */
    fun getWorkProfileHandle(): UserHandle? {
        val um = getUserManager()
        val myUser = Process.myUserHandle()
        return um.userProfiles.firstOrNull { it != myUser }
    }

    /**
     * إنشاء Intent لبدء عملية إعداد Work Profile من واجهة النظام الرسمية
     */
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

    /**
     * تثبيت/تفعيل تطبيق داخل Work Profile
     */
    suspend fun installAppInWorkProfile(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            getWorkProfileHandle() ?: throw IllegalStateException("No work profile found")
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
     * تشغيل تطبيق داخل Work Profile
     * (إذا لم يكن منسوخاً بعد، سيتم نسخه فوراً في نفس اللحظة ثم فتحه)
     */
    fun launchAppInWorkProfile(packageName: String): Boolean {
        val workHandle = getWorkProfileHandle() ?: return false
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        var activityList = launcherApps.getActivityList(packageName, workHandle)
        var activityInfo = activityList.firstOrNull()

        // إذا لم يكن التطبيق موجوداً داخل الـ Work Profile، نقوم بنسخه بالقوة فوراً!
        if (activityInfo == null) {
            try {
                val dpm = getDpm()
                dpm.setApplicationHidden(adminComponent, packageName, false)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    dpm.installExistingPackage(adminComponent, packageName)
                } else {
                    dpm.enableSystemApp(adminComponent, packageName)
                }
                
                // جلب الواجهة مرة أخرى بعد عملية النسخ
                activityList = launcherApps.getActivityList(packageName, workHandle)
                activityInfo = activityList.firstOrNull()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // إذا استمر في كونه null (التطبيق غير مدعوم أو لا يمتلك واجهة)
        if (activityInfo == null) return false

        return try {
            launcherApps.startMainActivity(
                activityInfo.componentName,
                workHandle,
                null,
                null
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * إيقاف تطبيق في Work Profile
     */
    fun stopAppInWorkProfile(packageName: String) {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            am.killBackgroundProcesses(packageName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * إخفاء/إزالة تطبيق من Work Profile
     */
    suspend fun uninstallAppFromWorkProfile(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            getWorkProfileHandle() ?: throw IllegalStateException("No work profile found")
            getDpm().setApplicationHidden(adminComponent, packageName, true)
            Unit
        }
    }

    /**
     * حذف Work Profile بالكامل
     */
    suspend fun removeWorkProfile(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val dpm = getDpm()
            if (dpm.isProfileOwnerApp(context.packageName)) {
                dpm.clearProfileOwner(adminComponent)
            }
            Unit
        }
    }

    /**
     * الحصول على التطبيقات المثبتة في Work Profile
     */
    fun getWorkProfileApps(): List<String> {
        val workHandle = getWorkProfileHandle() ?: return emptyList()
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        return launcherApps.getActivityList(null, workHandle)
            .map { it.applicationInfo.packageName }
            .distinct()
    }

    /**
     * الحصول على جميع التطبيقات القابلة للنسخ (المثبتة في البروفايل الشخصي)
     */
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
