package com.cryptodept.di

import android.content.Context
import com.cryptodept.data.datastore.PreferencesService
import com.cryptodept.data.update.AppUpdateRepository
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
    fun provideAppUpdateRepository(
        @ApplicationContext context: Context,
        preferences: PreferencesService,
    ): AppUpdateRepository = AppUpdateRepository(context, preferences)
}
