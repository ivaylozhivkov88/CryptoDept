package com.cryptodept.di

import com.cryptodept.data.datastore.PreferencesService
import com.cryptodept.data.datastore.SubscriptionAccessManager
import com.cryptodept.data.datastore.SystemSettingsManager
import com.cryptodept.data.datastore.UserSessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Exposes only the needed interface contract to each injection site.
 * PreferencesService itself remains a @Singleton and is constructed by Hilt
 * via its own @Inject constructor — no manual provide() needed for it.
 */
@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    fun provideSystemSettings(service: PreferencesService): SystemSettingsManager = service

    @Provides
    fun provideUserSession(service: PreferencesService): UserSessionManager = service

    @Provides
    fun provideSubscriptionAccess(service: PreferencesService): SubscriptionAccessManager = service
}
