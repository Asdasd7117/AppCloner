package com.example.appcloner.virtual

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dalvik.system.PathClassLoader
import java.io.File

class VirtualContainer(private val context: Context) {

    fun getVirtualDataDir(packageName: String): File {
        val dir = File(context.filesDir, "virtual_env/$packageName")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun createClassLoader(packageName: String): ClassLoader? {
        return try {
            val appInfo: ApplicationInfo = context.packageManager.getApplicationInfo(
                packageName,
                PackageManager.GET_META_DATA
            )
            val apkPath = appInfo.sourceDir
            val nativeLibDir = appInfo.nativeLibraryDir

            PathClassLoader(
                apkPath,
                nativeLibDir,
                context.classLoader
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
