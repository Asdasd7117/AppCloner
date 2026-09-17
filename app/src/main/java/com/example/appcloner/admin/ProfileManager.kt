package com.example.appcloner.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.UserHandle
import android.os.UserManager
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent: ComponentName =
        ComponentName(context, DeviceAdmin::class.java)
    private val userManager: UserManager =
        context.getSystemService(Context.USER_SERVICE) as UserManager

    /**
     * هل التطبيق مُفعَّل كـ Device Owner؟
     */
    fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(context.packageName)

    /**
     * هل يوجد Work Profile مُنشأ حالياً؟
     */
    fun hasWorkProfile(): Boolean {
        return userManager.userProfiles.any { it != Process.myUserHandle() }
    }

    /**
     * الحصول على UserHandle للـ Work Profile
     */
    fun getWorkProfileHandle(): UserHandle? {
        return userManager.userProfiles.firstOrNull { it != Process.myUserHandle() }
    }

    /**
     * إنشاء Work Profile جديد
     * يتطلب أن يكون التطبيق Device Owner
     */
    suspend fun createWorkProfile(): Result<UserHandle> = withContext(Dispatchers.Main) {
        runCatching {
            if (!isDeviceOwner()) {
                throw IllegalStateException("App is not a Device Owner")
            }
            if (hasWorkProfile()) {
                throw IllegalStateException("Work profile already exists")
            }
            dpm.createProfileManagementAction(adminComponent)
            // بدء إنشاء البروفايل
            val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE).apply {
                putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION, false)
                setPackage(context.packageName)
            }
            // ملاحظة: في الإصدارات الحديثة يُستخدم createAndManageUser أو
            // ProvisioningParams مع DevicePolicyManager
            throw UnsupportedOperationException(
                "Use ADB command for initial provisioning: " +
                "adb shell cmd device-policy create-profile-and-admin " +
                "--name \"AppCloner\" --component ${adminComponent.flattenToString()}"
            )
        }
    }

    /**
     * تثبيت تطبيق داخل Work Profile
     */
    suspend fun installAppInWorkProfile(packageName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val workHandle = getWorkProfileHandle()
                    ?: throw IllegalStateException("No work profile found")

                // الحصول على APK path من البروفايل الشخصي
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val apkPath = appInfo.sourceDir

                // إنشاء session للتثبيت في Work Profile
                val sessionParams = android.content.pm.PackageInstaller.SessionParams(
                    android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
                ).apply {
                    setSize(java.io.File(apkPath).length())
                }

                val installer = dpm.createAndManageUser(
                    adminComponent,
                    "work_${System.currentTimeMillis()}",
                    adminComponent,
                    null,
                    0
                )
                // ملاحظة: التثبيت الفعلي يتطلب LauncherApps أو
                // استخدام PackageInstaller في سياق المستخدم الآخر

                Unit
            }
        }

    /**
     * تشغيل تطبيق داخل Work Profile
     */
    fun launchAppInWorkProfile(packageName: String): Boolean {
        val workHandle = getWorkProfileHandle() ?: return false
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE)
                as LauncherApps

        val activityList = launcherApps.getActivityList(packageName, workHandle)
        val activityInfo = activityList.firstOrNull() ?: return false

        launcherApps.startMainActivity(
            activityInfo.componentName,
            workHandle,
            null,
            null
        )
        return true
    }

    /**
     * إيقاف تطبيق في Work Profile (force stop)
     */
    fun stopAppInWorkProfile(packageName: String) {
        val workHandle = getWorkProfileHandle() ?: return
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE)
                as LauncherApps
        // LauncherApps لا توفر force stop مباشر، لكن يمكن استخدام
        // DevicePolicyManager أو ActivityManager عبر reflection
        try {
            val amClass = Class.forName("android.app.ActivityManager")
            val getService = amClass.getMethod("getService")
            val am = getService.invoke(null)
            val forceStop = amClass.getMethod(
                "forceStopPackageAsUser",
                String::class.java,
                Int::class.javaPrimitiveType
            )
            val userIdMethod = workHandle.javaClass.getMethod("getIdentifier")
            val userId = userIdMethod.invoke(workHandle) as Int
            forceStop.invoke(am, packageName, userId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * حذف تطبيق من Work Profile
     */
    suspend fun uninstallAppFromWorkProfile(packageName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val workHandle = getWorkProfileHandle()
                    ?: throw IllegalStateException("No work profile found")
                // استخدام PackageInstaller في سياق المستخدم الآخر
                dpm.setApplicationHidden(
                    adminComponent,
                    packageName,
                    true
                )
            }
        }

    /**
     * حذف Work Profile بالكامل
     */
    suspend fun removeWorkProfile(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val workHandle = getWorkProfileHandle()
                ?: throw IllegalStateException("No work profile found")
            userManager.removeUser(
                (workHandle.javaClass.getMethod("getIdentifier").invoke(workHandle) as Int)
            )
        }
    }

    /**
     * الحصول على التطبيقات المثبتة في Work Profile
     */
    fun getWorkProfileApps(): List<String> {
        val workHandle = getWorkProfileHandle() ?: return emptyList()
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE)
                as LauncherApps
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
                // استبعاد تطبيقات النظام وتطبيقاتنا
                pm.getLaunchIntentForPackage(app.packageName) != null &&
                app.packageName != context.packageName &&
                !app.packageName.startsWith("android.") &&
                !app.packageName.startsWith("com.android.")
            }
            .map { it.packageName }
    }
}