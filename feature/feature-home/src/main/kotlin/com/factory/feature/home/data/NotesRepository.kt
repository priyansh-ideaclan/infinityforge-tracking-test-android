package com.factory.feature.home.data

import com.factory.core.common.AppResult
import com.factory.core.database.NoteDao
import com.factory.core.database.NoteEntity
import com.factory.core.logging.Logger
import com.factory.core.network.safeApiCall
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Offline-first: [notes] always reads from Room; [refresh] fetches from the network and
 * writes through to Room on success. A network failure leaves the last-cached data
 * visible instead of clearing the screen — this is the behavior `HomeViewModel`'s tests
 * rely on via `FakeNotesRepository`.
 */
interface NotesRepository {
    val notes: Flow<List<NoteEntity>>
    suspend fun refresh(): AppResult<Unit>
}

class DefaultNotesRepository @Inject constructor(
    private val notesApi: NotesApi,
    private val noteDao: NoteDao,
    private val logger: Logger,
) : NotesRepository {

    override val notes: Flow<List<NoteEntity>> = noteDao.observeAll()

    override suspend fun refresh(): AppResult<Unit> =
        safeApiCall(logger) {
            val remoteNotes = notesApi.getNotes()
            noteDao.upsertAll(
                remoteNotes.map {
                    NoteEntity(
                        id = it.id,
                        title = it.title,
                        body = it.body,
                        createdAtEpochMillis = it.createdAtEpochMillis,
                    )
                },
            )
        }
}
