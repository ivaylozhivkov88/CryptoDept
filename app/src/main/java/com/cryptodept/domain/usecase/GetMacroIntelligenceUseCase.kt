package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.MacroIntelligence
import com.cryptodept.domain.repository.MacroRepository
import javax.inject.Inject

class GetMacroIntelligenceUseCase @Inject constructor(
    private val repository: MacroRepository
) {
    suspend operator fun invoke(): Result<MacroIntelligence> {
        return repository.getMacroIntelligence()
    }
}
