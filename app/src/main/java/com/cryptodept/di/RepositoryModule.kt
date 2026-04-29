package com.cryptodept.di

import com.cryptodept.data.repository.*
import com.cryptodept.domain.repository.* // Глобален импорт на интерфейсите
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
    abstract fun bindCryptoRepository(impl: CryptoRepositoryImpl): CryptoRepository

    @Binds
    @Singleton
    abstract fun bindChartRepository(impl: ChartRepositoryImpl): ChartRepository

    @Binds
    @Singleton
    abstract fun bindAnalysisRepository(impl: AnalysisRepositoryImpl): AnalysisRepository

    @Binds
    @Singleton
    abstract fun bindAlertsRepository(impl: AlertsRepositoryImpl): AlertsRepository

    @Binds
    @Singleton
    abstract fun bindDerivativesRepository(impl: DerivativesRepositoryImpl): DerivativesRepository

    @Binds
    @Singleton
    abstract fun bindMacroRepository(impl: MacroRepositoryImpl): MacroRepository

    @Binds
    @Singleton
    abstract fun bindNewsRepository(impl: NewsRepositoryImpl): NewsRepository

    @Binds
    @Singleton
    abstract fun bindJournalRepository(impl: JournalRepositoryImpl): JournalRepository
}