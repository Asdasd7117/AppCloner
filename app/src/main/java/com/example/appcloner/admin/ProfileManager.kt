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

    fun getLastError(): String {
        return lastError
    }

    private fun setError(message: String) {
        lastError = message
    }

    private fun clearError() {
        lastError = ""
    }

    private fun getDpm(): DevicePolicyManager {
        return context.getSystemService(
            Context.DEVICE_POLICY_SERVICE
        ) as DevicePolicyManager
    }

    private fun getUserManager(): UserManager {
        return context.getSystemService(
            Context.USER_SERVICE
        ) as UserManager
    }

    private fun getLauncherApps(): LauncherApps {
        return context.getSystemService(
            Context.LAUNCHER_APPS_SERVICE
        ) as LauncherApps
    }

    fun hasWorkProfile(): Boolean {
        return getWorkProfileHandle() != null
    }

    fun getWorkProfileHandle(): UserHandle? {
        return try {
            val userManager = getUserManager()
            val currentUser = Process.myUserHandle()

            userManager.userProfiles.firstOrNull {
                it != currentUser
            }
        } catch (e: Exception) {
            null
        }
    }

    fun createWorkProfileIntent(): Intent {
        return Intent(
            DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE
        ).apply {

            putExtra(
                DevicePolicyManager
                    .EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                adminComponent
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(
                    DevicePolicyManager
                        .EXTRA_PROVISIONING_SKIP_ENCRYPTION,
                    true
                )
            }
        }
    }

    fun isProfileOwner(): Boolean {
        return try {
            getDpm().isProfileOwnerApp(context.packageName)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun installAppInWorkProfile(
        packageName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {

        runCatching {

            if (getWorkProfileHandle() == null) {
                throw IllegalStateException(
                    "لا يوجد Work Profile."
                )
            }

            val dpm = getDpm()

            if (!dpm.isProfileOwnerApp(context.packageName)) {
                throw SecurityException(
                    "التطبيق ليس Profile Owner للـ Work Profile."
                )
            }

            dpm.setApplicationHidden(
                adminComponent,
                packageName,
                false
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                val installed = dpm.installExistingPackage(
                    adminComponent,
                    packageName
                )

                if (!installed) {
                    throw IllegalStateException(
                        "لم يتم تثبيت التطبيق داخل Work Profile."
                    )
                }

            } else {

                dpm.enableSystemApp(
                    adminComponent,
                    packageName
                )
            }

            // مهم: إجبار runCatching على Result<Unit>
            Unit
        }
    }

    /**
     * تشغيل التطبيق داخل Work Profile.
     *
     * ترجع Boolean حتى تبقى متوافقة مع AppRepository.
     *
     * عند الفشل يتم حفظ السبب في getLastError().
     */
    fun launchAppInWorkProfile(
        packageName: String
    ): Boolean {

        clearError()

        return try {

            val workHandle = getWorkProfileHandle()

            if (workHandle == null) {
                setError(
                    "❌ فشل الفتح: لا يوجد Work Profile."
                )
                return false
            }

            val launcherApps = getLauncherApps()

            var activities = launcherApps.getActivityList(
                packageName,
                workHandle
            )

            /*
             * إذا لم يكن التطبيق ظاهرًا داخل Work Profile،
             * نحاول إتاحته وتثبيته.
             */
            if (activities.isEmpty()) {

                val dpm = getDpm()

                if (!dpm.isProfileOwnerApp(context.packageName)) {
                    setError(
                        "❌ فشل الفتح: التطبيق ليس Profile Owner للـ Work Profile."
                    )
                    return false
                }

                try {

                    dpm.setApplicationHidden(
                        adminComponent,
                        packageName,
                        false
                    )

                } catch (e: SecurityException) {

                    setError(
                        "❌ خطأ صلاحيات أثناء إظهار التطبيق:\n" +
                                (e.message ?: "تم رفض العملية.")
                    )
                    return false
                }

                try {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                        val installed =
                            dpm.installExistingPackage(
                                adminComponent,
                                packageName
                            )

                        if (!installed) {
                            setError(
                                "❌ فشل الفتح: لم يتم تثبيت التطبيق داخل Work Profile."
                            )
                            return false
                        }

                    } else {

                        dpm.enableSystemApp(
                            adminComponent,
                            packageName
                        )
                    }

                } catch (e: SecurityException) {

                    setError(
                        "❌ خطأ صلاحيات أثناء تثبيت التطبيق:\n" +
                                (e.message ?: "تم رفض العملية.")
                    )
                    return false

                } catch (e: Exception) {

                    setError(
                        "❌ خطأ أثناء تثبيت التطبيق:\n" +
                                (e.message ?: e.javaClass.simpleName)
                    )
                    return false
                }

                /*
                 * إعادة البحث عن Activity بعد التثبيت.
                 */
                activities = launcherApps.getActivityList(
                    packageName,
                    workHandle
                )
            }

            if (activities.isEmpty()) {

                setError(
                    "❌ فشل الفتح: التطبيق غير موجود داخل Work Profile أو لا يحتوي على واجهة تشغيل."
                )
                return false
            }

            val activityInfo = activities.first()

            try {

                launcherApps.startMainActivity(
                    activityInfo.componentName,
                    workHandle,
                    null,
                    null
                )

                clearError()
                true

            } catch (e: SecurityException) {

                setError(
                    "❌ خطأ صلاحيات عند تشغيل التطبيق:\n" +
                            (e.message ?: "تم رفض تشغيل التطبيق.")
                )
                false

            } catch (e: IllegalArgumentException) {

                setError(
                    "❌ خطأ في Activity الخاصة بالتطبيق:\n" +
                            (e.message ?: "Component غير صالح.")
                )
                false

            } catch (e: Exception) {

                setError(
                    "❌ فشل تشغيل التطبيق:\n" +
                            (e.message ?: e.javaClass.simpleName)
                )
                false
            }

        } catch (e: SecurityException) {

            setError(
                "❌ خطأ صلاحيات:\n" +
                        (e.message ?: "تم رفض العملية.")
            )
            false

        } catch (e: Exception) {

            setError(
                "❌ خطأ غير متوقع:\n" +
                        (e.message ?: e.javaClass.simpleName)
            )
            false
        }
    }

    fun stopAppInWorkProfile(
        packageName: String
    ): String {

        return try {

            if (getWorkProfileHandle() == null) {
                return "❌ لا يوجد Work Profile."
            }

            val activityManager =
                context.getSystemService(
                    Context.ACTIVITY_SERVICE
                ) as ActivityManager

            activityManager.killBackgroundProcesses(
                packageName
            )

            "✅ تم إرسال طلب إيقاف التطبيق."

        } catch (e: Exception) {

            "❌ فشل إيقاف التطبيق:\n" +
                    (e.message ?: e.javaClass.simpleName)
        }
    }

    suspend fun uninstallAppFromWorkProfile(
        packageName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {

        runCatching {

            if (getWorkProfileHandle() == null) {
                throw IllegalStateException(
                    "لا يوجد Work Profile."
                )
            }

            val dpm = getDpm()

            if (!dpm.isProfileOwnerApp(context.packageName)) {
                throw SecurityException(
                    "التطبيق ليس Profile Owner."
                )
            }

            dpm.setApplicationHidden(
                adminComponent,
                packageName,
                true
            )

            Unit
        }
    }

    suspend fun removeWorkProfile(): Result<Unit> =
        withContext(Dispatchers.IO) {

            runCatching {

                val dpm = getDpm()

                if (dpm.isProfileOwnerApp(context.packageName)) {

                    dpm.clearProfileOwner(
                        adminComponent
                    )

                } else {

                    throw SecurityException(
                        "التطبيق ليس Profile Owner لهذا Work Profile."
                    )
                }

                Unit
            }
        }

    fun getWorkProfileApps(): List<String> {

        return try {

            val workHandle =
                getWorkProfileHandle()
                    ?: return emptyList()

            getLauncherApps()
                .getActivityList(
                    null,
                    workHandle
                )
                .map {
                    it.applicationInfo.packageName
                }
                .distinct()

        } catch (e: Exception) {

            emptyList()
        }
    }

    fun getPersonalApps(): List<String> {

        return try {

            val packageManager = context.packageManager

            packageManager
                .getInstalledApplications(
                    PackageManager.GET_META_DATA
                )
                .filter { app ->

                    packageManager.getLaunchIntentForPackage(
                        app.packageName
                    ) != null &&

                    app.packageName != context.packageName &&

                    !app.packageName.startsWith("android.") &&

                    !app.packageName.startsWith("com.android.")
                }
                .map {
                    it.packageName
                }

        } catch (e: Exception) {

            emptyList()
        }
    }
}
