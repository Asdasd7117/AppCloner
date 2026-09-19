package com.example.appcloner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClonedAppDao {

    @Query("SELECT * FROM cloned_apps")
    fun getAllApps(): Flow<List<ClonedAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: ClonedAppEntity)

    @Query("DELETE FROM cloned_apps WHERE packageName = :packageName")
    suspend fun deleteAppByPackage(packageName: String)
}
