package com.cryptodept.data.repository

import com.cryptodept.domain.repository.JournalRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalRepositoryImpl @Inject constructor() : JournalRepository {

    /**
     * JournalRepository управлява дневника на трейдъра.
     * Тук ще се добавят методи за запис на сделки, бележки и стратегии.
     */

    // Ако в интерфейса си дефинирал методи, добави ги тук с override.
    // Пример (ако имаш такива в JournalRepository):
    /*
    override suspend fun getAllEntries(): List<JournalEntry> {
        return emptyList()
    }
    */
}