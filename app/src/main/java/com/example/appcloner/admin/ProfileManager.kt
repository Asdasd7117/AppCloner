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

    /**
     * التحقق من وجود Work Profile
     */
    fun hasWorkProfile(): Boolean {
        return getWorkProfileHandle() != null
    }

    /**
     * الحصول على UserHandle الخاص بـ Work Profile
     */
    fun getWorkProfileHandle(): UserHandle? {
        return try {
            val userManager = getUserManager()
            val myUser = Process.myUserHandle()

            userManager.userProfiles.firstOrNull { userHandle ->
                userHandle != myUser
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * إنشاء Intent لإنشاء Work Profile
     * من خلال واجهة Android الرسمية.
     */
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

    /**
     * التحقق هل التطبيق Profile Owner
     */
    fun isProfileOwner(): Boolean {
        return try {
            getDpm().isProfileOwnerApp(context.packageName)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * تثبيت / إتاحة تطبيق موجود داخل Work Profile
     */
    suspend fun installAppInWorkProfile(
        packageName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {

        runCatching {

            val workProfile = getWorkProfileHandle()
                ?: throw IllegalStateException(
                    "لا يوجد Work Profile."
                )

            val dpm = getDpm()

            if (!dpm.isProfileOwnerApp(context.packageName)) {
                throw SecurityException(
                    "التطبيق ليس Profile Owner للـ Work Profile."
                )
            }

            /*
             * إظهار التطبيق إذا كان مخفياً
             */
            dpm.setApplicationHidden(
                adminComponent,
                packageName,
                false
            )

            /*
             * Android 9+
             */
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

                /*
                 * للإصدارات القديمة
                 */
                dpm.enableSystemApp(
                    adminComponent,
                    packageName
                )
            }

            /*
             * مجرد استخدام المتغير للتأكد من وجود Profile فعلي
             */
            if (workProfile == Process.myUserHandle()) {
                throw IllegalStateException(
                    "تم اكتشاف User Profile غير صحيح."
                )
            }
        }
    }

    /**
     * تشغيل التطبيق داخل Work Profile
     *
     * ترجع رسالة واضحة للمستخدم عند حدوث فشل.
     */
    fun launchAppInWorkProfile(
        packageName: String
    ): String {

        try {

            /*
             * 1. الحصول على Work Profile
             */
            val workHandle = getWorkProfileHandle()

            if (workHandle == null) {
                return "❌ فشل الفتح: لا يوجد Work Profile."
            }

            /*
             * 2. الحصول على LauncherApps
             */
            val launcherApps = getLauncherApps()

            /*
             * 3. البحث عن Activity الخاصة بالتطبيق
             * داخل Work Profile
             */
            var activityList =
                launcherApps.getActivityList(
                    packageName,
                    workHandle
                )

            /*
             * 4. إذا لم نجد التطبيق،
             * نحاول إتاحته داخل Work Profile
             */
            if (activityList.isEmpty()) {

                val dpm = getDpm()

                /*
                 * يجب أن يكون التطبيق Profile Owner
                 */
                if (!dpm.isProfileOwnerApp(context.packageName)) {

                    return "❌ فشل الفتح: " +
                            "التطبيق ليس Profile Owner للـ Work Profile."
                }

                try {

                    /*
                     * إظهار التطبيق إذا كان مخفياً
                     */
                    dpm.setApplicationHidden(
                        adminComponent,
                        packageName,
                        false
                    )

                } catch (e: SecurityException) {

                    return "❌ خطأ صلاحيات أثناء إظهار التطبيق:\n" +
                            (e.message ?: "تم رفض العملية.")
                }

                /*
                 * تثبيت التطبيق الموجود مسبقاً
                 */
                try {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                        val installed =
                            dpm.installExistingPackage(
                                adminComponent,
                                packageName
                            )

                        if (!installed) {

                            return "❌ فشل الفتح:\n" +
                                    "لم يتم تثبيت التطبيق داخل Work Profile."
                        }

                    } else {

                        dpm.enableSystemApp(
                            adminComponent,
                            packageName
                        )
                    }

                } catch (e: SecurityException) {

                    return "❌ خطأ صلاحيات أثناء تثبيت التطبيق:\n" +
                            (e.message ?: "تم رفض العملية.")

                } catch (e: Exception) {

                    return "❌ خطأ أثناء تثبيت التطبيق:\n" +
                            (e.message ?: e.javaClass.simpleName)
                }

                /*
                 * إعادة البحث عن Activity
                 */
                activityList =
                    launcherApps.getActivityList(
                        packageName,
                        workHandle
                    )
            }

            /*
             * 5. لم نجد Activity
             */
            if (activityList.isEmpty()) {

                return "❌ فشل الفتح:\n" +
                        "التطبيق غير موجود داخل Work Profile " +
                        "أو لا يحتوي على واجهة تشغيل."
            }

            /*
             * 6. أخذ أول Activity قابلة للتشغيل
             */
            val activityInfo =
                activityList.first()

            /*
             * 7. تشغيل التطبيق
             */
            return try {

                launcherApps.startMainActivity(
                    activityInfo.componentName,
                    workHandle,
                    null,
                    null
                )

                "✅ تم تشغيل التطبيق بنجاح."

            } catch (e: SecurityException) {

                "❌ خطأ صلاحيات عند تشغيل التطبيق:\n" +
                        (e.message ?: "تم رفض تشغيل التطبيق.")

            } catch (e: IllegalArgumentException) {

                "❌ خطأ في بيانات التطبيق:\n" +
                        (e.message ?: "Component غير صالح.")

            } catch (e: Exception) {

                "❌ فشل تشغيل التطبيق:\n" +
                        (e.message ?: e.javaClass.simpleName)
            }

        } catch (e: SecurityException) {

            return "❌ خطأ صلاحيات:\n" +
                    (e.message ?: "تم رفض العملية.")

        } catch (e: Exception) {

            return "❌ خطأ غير متوقع:\n" +
                    (e.message ?: e.javaClass.simpleName)
        }
    }

    /**
     * إيقاف تطبيق في Work Profile
     */
    fun stopAppInWorkProfile(
        packageName: String
    ): String {

        return try {

            val workHandle =
                getWorkProfileHandle()
                    ?: return "❌ لا يوجد Work Profile."

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

    /**
     * إخفاء التطبيق من Work Profile
     */
    suspend fun uninstallAppFromWorkProfile(
        packageName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {

        runCatching {

            getWorkProfileHandle()
                ?: throw IllegalStateException(
                    "لا يوجد Work Profile."
                )

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
        }
    }

    /**
     * إزالة إدارة Work Profile
     */
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
            }
        }

    /**
     * الحصول على التطبيقات الموجودة
     * داخل Work Profile
     */
    fun getWorkProfileApps(): List<String> {

        return try {

            val workHandle =
                getWorkProfileHandle()
                    ?: return emptyList()

            val launcherApps =
                getLauncherApps()

            launcherApps
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

    /**
     * الحصول على التطبيقات القابلة للتشغيل
     * في الملف الشخصي الشخصي
     */
    fun getPersonalApps(): List<String> {

        return try {

            val packageManager =
                context.packageManager

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

مهم: هذه النسخة تجعل "launchAppInWorkProfile()" ترجع "String" بدل "Boolean". لذلك المكان الذي يستدعيها في الواجهة يجب أن يتعامل مع النص ويعرضه للمستخدم. بهذه الطريقة، إذا ضغطت على التطبيق ولم يفتح، لن يكون الفشل صامتًا وستعرف الرسالة التي رجعها النظام/الكود.
