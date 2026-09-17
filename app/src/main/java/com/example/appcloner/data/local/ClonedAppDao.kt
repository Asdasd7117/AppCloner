package com.example.appcloner.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClonedAppDao {
    @Query("SELECT * FROM cloned_apps ORDER BY addedAt DESC")
    fun getAllClonedApps(): Flow<List<ClonedAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: ClonedAppEntity)

    @Update
    suspend fun update(app: ClonedAppEntity)

    @Delete
    suspend fun delete(app: ClonedAppEntity)

    @Query("DELETE FROM cloned_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("SELECT * FROM cloned_apps WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): ClonedAppEntity?
}