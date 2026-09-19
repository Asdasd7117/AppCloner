package com.example.appcloner.di

import android.content.Context
import androidx.room.Room
import com.example.appcloner.data.local.AppDatabase
import com.example.appcloner.data.local.ClonedAppDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_cloner_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideClonedAppDao(database: AppDatabase): ClonedAppDao {
        return database.clonedAppDao()
    }
}
