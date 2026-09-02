package com.factory.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factory.core.analytics.AnalyticsEvent
import com.factory.core.analytics.AnalyticsTracker
import com.factory.core.common.AppResult
import com.factory.core.database.NoteEntity
import com.factory.feature.home.data.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val notes: List<NoteEntity> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val notesRepository: NotesRepository,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val isRefreshing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        notesRepository.notes,
        isRefreshing,
        errorMessage,
    ) { notes, refreshing, error ->
        HomeUiState(notes = notes, isRefreshing = refreshing, errorMessage = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        analyticsTracker.track(AnalyticsEvent.ScreenViewed(screenName = "home"))
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            when (val result = notesRepository.refresh()) {
                is AppResult.Success -> errorMessage.value = null
                is AppResult.Failure -> errorMessage.value = result.error.message
            }
            isRefreshing.value = false
        }
    }

    fun onErrorDismissed() {
        errorMessage.update { null }
    }
}
