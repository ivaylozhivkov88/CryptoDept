package com.cryptodept.di

import com.cryptodept.domain.model.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AgentModule {

    @Provides
    @Singleton
    fun provideTechnicalSentinel(): TechnicalSentinel = TechnicalSentinel()

    @Provides
    @Singleton
    fun provideWhaleScout(): WhaleScout = WhaleScout()

    @Provides
    @Singleton
    fun provideSentimentPulse(): SentimentPulse = SentimentPulse()

    @Provides
    @Singleton
    fun provideMarketingStrategist(): MarketingStrategist = MarketingStrategist()

    @Provides
    @Singleton
    fun provideNarrativeOrchestrator(): NarrativeOrchestrator = NarrativeOrchestrator()

    @Provides
    @Singleton
    fun provideOversightSentinel(): OversightSentinel = OversightSentinel()

    @Provides
    @Singleton
    fun provideMarketNarrator(): MarketNarrator = MarketNarrator()

    @Provides
    @Singleton
    fun provideSystemAuditor(): SystemAuditor = SystemAuditor()

    @Provides
    @Singleton
    fun provideFiscalTreasury(): FiscalTreasury = FiscalTreasury()

    @Provides
    @Singleton
    fun providePriceOracle(): PriceOracle = PriceOracle()

    @Provides
    @Singleton
    fun provideDataIntegrityAgent(
        api: com.cryptodept.data.api.CoinGeckoApi,
        repository: com.cryptodept.domain.repository.CryptoRepository
    ): com.cryptodept.domain.agent.DataIntegrityAgent = com.cryptodept.domain.agent.DataIntegrityAgent(api, repository)
}
