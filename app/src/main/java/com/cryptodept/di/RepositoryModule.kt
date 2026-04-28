package com.cryptodept.di

import com.cryptodept.data.repository.AlertsRepositoryImpl
import com.cryptodept.data.repository.AnalysisRepositoryImpl
import com.cryptodept.data.repository.ChartRepositoryImpl
import com.cryptodept.data.repository.CryptoRepositoryImpl
import com.cryptodept.domain.repository.AlertsRepository
import com.cryptodept.domain.repository.AnalysisRepository
import com.cryptodept.domain.repository.ChartRepository
import com.cryptodept.domain.repository.CryptoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCryptoRepository(
        cryptoRepositoryImpl: CryptoRepositoryImpl
    ): CryptoRepository

    @Binds
    @Singleton
    abstract fun bindChartRepository(
        chartRepositoryImpl: ChartRepositoryImpl
    ): ChartRepository

    @Binds
    @Singleton
    abstract fun bindAnalysisRepository(
        analysisRepositoryImpl: AnalysisRepositoryImpl
    ): AnalysisRepository

    @Binds
    @Singleton
    abstract fun bindAlertsRepository(
        alertsRepositoryImpl: AlertsRepositoryImpl
    ): AlertsRepository
}