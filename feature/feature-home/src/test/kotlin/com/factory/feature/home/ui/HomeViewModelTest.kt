package com.factory.feature.home.ui

import app.cash.turbine.test
import com.factory.core.common.AppError
import com.factory.core.common.AppResult
import com.factory.core.database.NoteEntity
import com.factory.core.testing.FakeAnalyticsTracker
import com.factory.core.testing.MainDispatcherRule
import com.factory.feature.home.data.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

private class FakeNotesRepository : NotesRepository {
    private val notesFlow = MutableStateFlow<List<NoteEntity>>(emptyList())
    override val notes = notesFlow

    var refreshResult: AppResult<Unit> = AppResult.Success(Unit)
    var refreshedNotes: List<NoteEntity> = emptyList()

    override suspend fun refresh(): AppResult<Unit> {
        if (refreshResult is AppResult.Success) {
            notesFlow.value = refreshedNotes
        }
        return refreshResult
    }
}

/**
 * `HomeViewModel.uiState` is a `WhileSubscribed` `StateFlow`, so it only starts
 * combining its upstream flows once something actually collects it — these tests use
 * Turbine's `.test { }` rather than reading `.value` directly, or they'd only ever see
 * the initial default state.
 */
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `refresh success populates notes and clears error`() = runTest {
        val repository = FakeNotesRepository().apply {
            refreshedNotes = listOf(NoteEntity(id = "1", title = "T", body = "B", createdAtEpochMillis = 1L))
        }
        val viewModel = HomeViewModel(repository, FakeAnalyticsTracker())

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.notes.size)
            assertNull(state.errorMessage)
        }
    }

    @Test
    fun `refresh failure surfaces the error message`() = runTest {
        val repository = FakeNotesRepository().apply {
            refreshResult = AppResult.Failure(AppError.Network(message = "offline"))
        }
        val viewModel = HomeViewModel(repository, FakeAnalyticsTracker())

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("offline", state.errorMessage)
        }
    }
}
