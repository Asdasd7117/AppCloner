package com.example.appcloner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ClonedAppEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clonedAppDao(): ClonedAppDao
}