package com.cryptodept.di

import android.content.Context
import com.cryptodept.service.AlertNotificationService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    @Provides
    @Singleton
    fun provideAlertNotificationService(
        @ApplicationContext ctx: Context,
    ) = AlertNotificationService(ctx)
}
