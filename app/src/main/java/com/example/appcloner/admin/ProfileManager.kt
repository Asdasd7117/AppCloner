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
            val workHandle = getWorkProfileHandle()
                ?: throw IllegalStateException("No work profile found")

            val dpm = getDpm()

            // 1. إلغاء أي إخفاء محتمل للتطبيق
            dpm.setApplicationHidden(adminComponent, packageName, false)

            // 2. تمكين التطبيق إذا كان موجوداً كـ System App داخل البروفايل
            try {
                dpm.enableSystemApp(adminComponent, packageName)
            } catch (e: Exception) {
                // ليس تطبيق نظام، ننتقل للطريقة المباشرة عبر LauncherApps / PackageInstaller
            }

            // 3. تثبيت/تفعيل الحزمة للمستخدم في Work Profile عبر Command أو PackageInstaller
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    dpm.installExistingPackage(adminComponent, packageName)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            Unit
        }
    }

    /**
     * تشغيل تطبيق داخل Work Profile، أو فتح متجر Play Store داخل البروفايل لتثبيته فوراً إذا لم يكن متاحاً
     */
    fun launchAppInWorkProfile(packageName: String): Boolean {
        val workHandle = getWorkProfileHandle() ?: return false
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        val activityList = launcherApps.getActivityList(packageName, workHandle)
        val activityInfo = activityList.firstOrNull()

        return if (activityInfo != null) {
            launcherApps.startMainActivity(
                activityInfo.componentName,
                workHandle,
                null,
                null
            )
            true
        } else {
            // فتح صفحة التطبيق داخل متجر Google Play الخاص بالـ Work Profile لنسخه بنقرة واحدة
            try {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                e.printStackTrace Box@{
                    return false
                }
            }
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
