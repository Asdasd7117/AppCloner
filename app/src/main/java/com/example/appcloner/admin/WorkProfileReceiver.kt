package com.example.appcloner.admin

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.os.Build

class WorkProfileReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra("EXTRA_PACKAGE_NAME") ?: return
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, DeviceAdmin::class.java)

        // التحقق من أن النسخة الحالية تعمل داخل بيئة العمل كـ Profile Owner
        if (dpm.isProfileOwnerApp(context.packageName)) {
            try {
                if (intent.action == ACTION_INSTALL_APP) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        dpm.installExistingPackage(adminComponent, packageName)
                    } else {
                        dpm.enableSystemApp(adminComponent, packageName)
                    }
                    dpm.setApplicationHidden(adminComponent, packageName, false)
                } else if (intent.action == ACTION_UNINSTALL_APP) {
                    dpm.setApplicationHidden(adminComponent, packageName, true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        const val ACTION_INSTALL_APP = "com.example.appcloner.ACTION_INSTALL_APP"
        const val ACTION_UNINSTALL_APP = "com.example.appcloner.ACTION_UNINSTALL_APP"
    }
}
