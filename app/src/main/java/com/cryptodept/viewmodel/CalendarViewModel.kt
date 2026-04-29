package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.CalendarEvent
import com.cryptodept.domain.repository.MacroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val macroRepository: MacroRepository
) : ViewModel() {

    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hotOnly = MutableStateFlow(false)
    
    val filteredEvents: StateFlow<List<CalendarEvent>> = combine(_events, _hotOnly) { list, hotOnly ->
        if (hotOnly) list.filter { it.isHot } else list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { loadEvents() }

    fun loadEvents() {
        viewModelScope.launch {
            _isLoading.value = true
            macroRepository.getCalendarEvents()
                .onSuccess { _events.value = it }
            _isLoading.value = false
        }
    }

    fun toggleHotOnly() { _hotOnly.value = !_hotOnly.value }
}
