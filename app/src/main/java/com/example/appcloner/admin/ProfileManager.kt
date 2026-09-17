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
    val adminComponent: ComponentName =
        ComponentName(context, DeviceAdmin::class.java)

    private fun getDpm(): DevicePolicyManager {
        return context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    private fun getUserManager(): UserManager {
        return context.getSystemService(Context.USER_SERVICE) as UserManager
    }

    /**
     * التحقق مما إذا كان التطبيق محدد كـ Device Owner على الجهاز
     */
    fun isDeviceOwner(): Boolean {
        return getDpm().isDeviceOwnerApp(context.packageName)
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
            val dpm = getDpm()
            val isOwner = dpm.isDeviceOwnerApp(context.packageName) || dpm.isProfileOwnerApp(context.packageName)
            if (!isOwner && getWorkProfileHandle() == null) {
                throw IllegalStateException("التطبيق لا يملك صلاحية Device Owner أو Profile Owner")
            }

            try {
                dpm.setApplicationHidden(adminComponent, packageName, false)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dpm.installExistingPackage(adminComponent, packageName)
            } else {
                dpm.enableSystemApp(adminComponent, packageName)
            }
            Unit
        }
    }

    /**
     * تشغيل تطبيق داخل Work Profile أو إرجاع نتيجة الخطأ للواجهة
     */
    fun launchAppInWorkProfile(packageName: String): Result<Unit> {
        val workHandle = getWorkProfileHandle()
        val targetHandle = workHandle ?: Process.myUserHandle()

        val dpm = getDpm()
        var lastException: Exception? = null

        // محاولة إظهار وتثبيت التطبيق داخل البيئة المعزولة
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
        var activityList = launcherApps.getActivityList(packageName, targetHandle)

        // محاولة إعادة الفحص في حال استغرق النظام بعض الوقت لإتاحة التطبيق
        var attempts = 0
        while (activityList.isEmpty() && attempts < 3) {
            try { Thread.sleep(200) } catch (_: Exception) {}
            activityList = launcherApps.getActivityList(packageName, targetHandle)
            attempts++
        }

        val activityInfo = activityList.firstOrNull()
            ?: return Result.failure(
                Exception(
                    "فشل الفتح: التطبيق غير متاح داخل البيئة المعزولة.\nالسبب التقني: ${lastException?.localizedMessage ?: "عدم وجود صلاحيات Device/Profile Owner مقترنة بالبروفايل."}"
                )
            )

        return try {
            launcherApps.startMainActivity(
                activityInfo.componentName,
                targetHandle,
                null,
                null
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("خطأ أثناء تشغيل الواجهة: ${e.localizedMessage}"))
        }
    }

    /**
     * إيقاف تطبيق في البيئة المعزولة
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
     * إخفاء/إزالة تطبيق من البيئة المعزولة
     */
    suspend fun uninstallAppFromWorkProfile(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val dpm = getDpm()
            dpm.setApplicationHidden(adminComponent, packageName, true)
            Unit
        }
    }

    /**
     * إزالة صلاحية الأدمن أو إلغاء البروفايل
     */
    suspend fun removeWorkProfile(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val dpm = getDpm()
            if (dpm.isProfileOwnerApp(context.packageName)) {
                dpm.clearProfileOwner(adminComponent)
            } else if (dpm.isDeviceOwnerApp(context.packageName)) {
                dpm.clearDeviceOwnerApp(context.packageName)
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
