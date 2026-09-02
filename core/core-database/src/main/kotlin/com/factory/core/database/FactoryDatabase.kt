package com.factory.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [NoteEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class FactoryDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * `databaseName` is sourced from `APP_SPEC.yaml`'s `room.database_name` by
     * `scripts/configure_app.py` (written into `AppModule`'s constant, not here — this
     * module only owns Room *mechanics*, not the app-specific database name).
     */
    @Provides
    @Singleton
    fun provideFactoryDatabase(
        @ApplicationContext context: Context,
        databaseName: DatabaseName,
    ): FactoryDatabase = Room.databaseBuilder(
        context,
        FactoryDatabase::class.java,
        databaseName.value,
    ).build()

    @Provides
    fun provideNoteDao(database: FactoryDatabase): NoteDao = database.noteDao()
}

/** Wrapper so Hilt can inject a plain string without ambiguity with other strings. */
data class DatabaseName(val value: String)
