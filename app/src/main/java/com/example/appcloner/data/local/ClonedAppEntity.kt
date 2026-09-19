package com.example.appcloner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cloned_apps")
data class ClonedAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isCloned: Boolean = true
)
