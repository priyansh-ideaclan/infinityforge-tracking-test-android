package com.factory.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NoteDaoTest {

    private lateinit var database: FactoryDatabase
    private lateinit var noteDao: NoteDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FactoryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        noteDao = database.noteDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `upsertAll then observeAll returns notes ordered newest first`() = runTest {
        noteDao.upsertAll(
            listOf(
                NoteEntity(id = "1", title = "First", body = "…", createdAtEpochMillis = 100L),
                NoteEntity(id = "2", title = "Second", body = "…", createdAtEpochMillis = 200L),
            ),
        )

        val notes = noteDao.observeAll().first()

        assertEquals(listOf("2", "1"), notes.map { it.id })
    }

    @Test
    fun `upsertAll with same id replaces the existing row`() = runTest {
        noteDao.upsertAll(listOf(NoteEntity(id = "1", title = "Old", body = "…", createdAtEpochMillis = 100L)))
        noteDao.upsertAll(listOf(NoteEntity(id = "1", title = "New", body = "…", createdAtEpochMillis = 100L)))

        val notes = noteDao.observeAll().first()

        assertEquals(1, notes.size)
        assertEquals("New", notes.single().title)
    }

    @Test
    fun `clear removes all notes`() = runTest {
        noteDao.upsertAll(listOf(NoteEntity(id = "1", title = "A", body = "…", createdAtEpochMillis = 100L)))

        noteDao.clear()

        assertEquals(emptyList<NoteEntity>(), noteDao.observeAll().first())
    }
}
