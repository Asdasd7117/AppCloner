package com.example.appcloner.admin

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

class WorkProfileReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val packageName = intent.getStringExtra("EXTRA_PACKAGE_NAME") ?: return

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, DeviceAdmin::class.java)

        // التأكد من أن النسخة الحالية تعمل كـ Profile Owner داخل بيئة العمل
        if (!dpm.isProfileOwnerApp(context.packageName)) return

        when (action) {
            ACTION_INSTALL_APP -> {
                try {
                    dpm.setApplicationHidden(adminComponent, packageName, false)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        dpm.installExistingPackage(adminComponent, packageName)
                    } else {
                        dpm.enableSystemApp(adminComponent, packageName)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            ACTION_UNINSTALL_APP -> {
                try {
                    dpm.setApplicationHidden(adminComponent, packageName, true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        const val ACTION_INSTALL_APP = "com.example.appcloner.ACTION_INSTALL_APP"
        const val ACTION_UNINSTALL_APP = "com.example.appcloner.ACTION_UNINSTALL_APP"
    }
}
