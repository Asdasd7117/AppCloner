package com.example.appcloner.virtual

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class StubActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetPackage = intent.getStringExtra("EXTRA_TARGET_PACKAGE")
        if (targetPackage.isNullOrEmpty()) {
            finish()
            return
        }

        val container = VirtualContainer(this)
        val classLoader = container.createClassLoader(targetPackage)

        if (classLoader == null) {
            finish()
            return
        }

        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
            val targetActivityName = launchIntent?.component?.className

            if (targetActivityName == null) {
                finish()
                return
            }

            val virtualContext = VirtualContext(this, targetPackage, container)
            val targetClass = classLoader.loadClass(targetActivityName)
            val activityObject = targetClass.getDeclaredConstructor().newInstance()

            if (activityObject is Activity) {
                val intentToLaunch = Intent(launchIntent).apply {
                    component = android.content.ComponentName(targetPackage, targetActivityName)
                }
                setIntent(intentToLaunch)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }
}
