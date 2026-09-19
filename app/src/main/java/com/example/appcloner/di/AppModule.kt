package com.example.appcloner.di

import android.content.Context
import com.example.appcloner.virtual.VirtualAppManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideVirtualAppManager(
        @ApplicationContext context: Context
    ): VirtualAppManager {
        return VirtualAppManager(context)
    }
}
