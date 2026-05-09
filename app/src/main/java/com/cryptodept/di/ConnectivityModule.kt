package com.cryptodept.di

import android.content.Context
import com.cryptodept.util.ConnectivityObserver
import com.cryptodept.util.NetworkConnectivityObserver
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectivityModule {

    @Binds
    @Singleton
    abstract fun bindConnectivityObserver(
        networkConnectivityObserver: NetworkConnectivityObserver
    ): ConnectivityObserver
    
    companion object {
        @Provides
        @Singleton
        fun provideNetworkConnectivityObserver(
            @ApplicationContext context: Context
        ): NetworkConnectivityObserver {
            return NetworkConnectivityObserver(context)
        }
    }
}
