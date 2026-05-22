package com.cryptodept.di

import com.cryptodept.data.api.CoinCapPriceProvider
import com.cryptodept.data.api.CoinPaprikaPriceProvider
import com.cryptodept.data.api.CoinbasePriceProvider
import com.cryptodept.data.api.FirebasePriceProvider
import com.cryptodept.data.api.KrakenPriceProvider
import com.cryptodept.domain.repository.PriceProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Registers all PriceProvider implementations into a Set<PriceProvider>
 * via Hilt Multibindings. To add a new exchange (e.g. Binance), simply
 * create a new XxxPriceProvider class and add a @Binds @IntoSet here.
 * MultiSourcePriceAggregator does NOT need to change.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PriceProvidersModule {

    @Binds
    @IntoSet
    abstract fun bindKrakenProvider(impl: KrakenPriceProvider): PriceProvider

    @Binds
    @IntoSet
    abstract fun bindCoinbaseProvider(impl: CoinbasePriceProvider): PriceProvider

    @Binds
    @IntoSet
    abstract fun bindCoinCapProvider(impl: CoinCapPriceProvider): PriceProvider

    @Binds
    @IntoSet
    abstract fun bindCoinPaprikaProvider(impl: CoinPaprikaPriceProvider): PriceProvider

    @Binds
    @IntoSet
    abstract fun bindFirebaseProvider(impl: FirebasePriceProvider): PriceProvider
}
