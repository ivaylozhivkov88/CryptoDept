package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.education.GlossaryDatabase
import com.cryptodept.domain.education.GlossaryDatabase.GlossaryCategory
import com.cryptodept.domain.education.GlossaryDatabase.GlossaryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class GlossaryViewModel @Inject constructor() : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<GlossaryCategory?>(null)
    val selectedCategory: StateFlow<GlossaryCategory?> = _selectedCategory.asStateFlow()
    
    val filteredEntries: StateFlow<List<GlossaryEntry>> = combine(
        _searchQuery,
        _selectedCategory,
    ) { query, category ->
        var result = GlossaryDatabase.entries
        if (category != null) result = result.filter { it.category == category }
        if (query.isNotBlank()) result = GlossaryDatabase.search(query)
            .let { searched ->
                if (category != null) searched.filter { it.category == category }
                else searched
            }
        result.sortedBy { it.term }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GlossaryDatabase.entries.sortedBy { it.term },
    )
    
    fun updateSearch(query: String) { _searchQuery.value = query }
    fun selectCategory(category: GlossaryCategory?) { _selectedCategory.value = category }
}
