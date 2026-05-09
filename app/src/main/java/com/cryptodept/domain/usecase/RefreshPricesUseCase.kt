package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.CryptoResult
import com.cryptodept.domain.repository.CryptoRepository
import javax.inject.Inject

class RefreshPricesUseCase
    @Inject
    constructor(
        private val repository: CryptoRepository,
    ) {
        suspend operator fun invoke(): CryptoResult<Unit> = repository.refreshPrices()
    }
