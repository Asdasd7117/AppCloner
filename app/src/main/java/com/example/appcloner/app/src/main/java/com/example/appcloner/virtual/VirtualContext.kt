package com.example.appcloner.virtual

import android.content.Context
import android.content.ContextWrapper
import java.io.File

class VirtualContext(
    base: Context,
    private val packageName: String,
    private val container: VirtualContainer
) : ContextWrapper(base) {

    override fun getFilesDir(): File {
        val filesDir = File(container.getVirtualDataDir(packageName), "files")
        if (!filesDir.exists()) filesDir.mkdirs()
        return filesDir
    }

    override fun getCacheDir(): File {
        val cacheDir = File(container.getVirtualDataDir(packageName), "cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        return cacheDir
    }

    override fun getDatabasePath(name: String): File {
        val dbDir = File(container.getVirtualDataDir(packageName), "databases")
        if (!dbDir.exists()) dbDir.mkdirs()
        return File(dbDir, name)
    }

    override fun getSharedPreferencesPath(name: String): File {
        val prefsDir = File(container.getVirtualDataDir(packageName), "shared_prefs")
        if (!prefsDir.exists()) prefsDir.mkdirs()
        return File(prefsDir, "$name.xml")
    }

    override fun getPackageName(): String {
        return packageName
    }
}
