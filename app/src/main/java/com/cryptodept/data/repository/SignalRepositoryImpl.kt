package com.cryptodept.data.repository

import com.cryptodept.data.db.CustomSignalDao
import com.cryptodept.data.db.CustomSignalRuleEntity
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.SignalRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalRepositoryImpl
    @Inject
    constructor(
        private val dao: CustomSignalDao,
        private val gson: Gson,
    ) : SignalRepository {
        override fun getAllCustomRules(): Flow<List<CustomSignalRule>> =
            dao.getAllRules().map { entities ->
                entities.map { entity ->
                    val type = object : TypeToken<List<CustomSignalCondition>>() {}.type
                    val conditions: List<CustomSignalCondition> = gson.fromJson(entity.conditionsJson, type)

                    CustomSignalRule(
                        id = entity.id,
                        name = entity.name,
                        conditions = conditions,
                        operator = LogicalOperator.valueOf(entity.operator),
                        action = SignalAction.valueOf(entity.action),
                        isActive = entity.isActive,
                    )
                }
            }

        override suspend fun saveRule(rule: CustomSignalRule) {
            val entity =
                CustomSignalRuleEntity(
                    id = rule.id,
                    name = rule.name,
                    conditionsJson = gson.toJson(rule.conditions),
                    operator = rule.operator.name,
                    action = rule.action.name,
                    isActive = rule.isActive,
                )
            dao.insertRule(entity)
        }

        override suspend fun deleteRule(ruleId: String) {
            // Need to fetch entity first to delete or just use ID
            // Simplified: using a dummy entity with ID
            val entity = CustomSignalRuleEntity(ruleId, "", "", "", "", false)
            dao.deleteRule(entity)
        }

        override suspend fun toggleRule(
            ruleId: String,
            isActive: Boolean,
        ) {
            dao.toggleRule(ruleId, isActive)
        }
    }
