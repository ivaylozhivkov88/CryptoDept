package com.cryptodept.ui.signals.composer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.SignalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SignalComposerViewModel
    @Inject
    constructor(
        private val repository: SignalRepository,
    ) : ViewModel() {
        private val _rules = MutableStateFlow<List<CustomSignalRule>>(emptyList())
        val rules: StateFlow<List<CustomSignalRule>> = _rules.asStateFlow()

        init {
            loadRules()
        }

        private fun loadRules() {
            viewModelScope.launch {
                repository.getAllCustomRules().collect {
                    _rules.value = it
                }
            }
        }

        fun saveRule(
            name: String,
            conditions: List<CustomSignalCondition>,
            operator: LogicalOperator,
            action: SignalAction,
        ) {
            viewModelScope.launch {
                val rule =
                    CustomSignalRule(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        conditions = conditions,
                        operator = operator,
                        action = action,
                    )
                repository.saveRule(rule)
            }
        }

        fun deleteRule(id: String) {
            viewModelScope.launch {
                repository.deleteRule(id)
            }
        }

        fun toggleRule(
            id: String,
            isActive: Boolean,
        ) {
            viewModelScope.launch {
                repository.toggleRule(id, isActive)
            }
        }
    }
