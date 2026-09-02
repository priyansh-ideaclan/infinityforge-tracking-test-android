package com.factory.feature.home.di

import com.factory.core.database.NoteDao
import com.factory.core.logging.Logger
import com.factory.feature.home.data.DefaultNotesRepository
import com.factory.feature.home.data.NotesApi
import com.factory.feature.home.data.NotesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HomeModule {

    @Provides
    @Singleton
    fun provideNotesApi(retrofit: Retrofit): NotesApi = retrofit.create(NotesApi::class.java)

    @Provides
    @Singleton
    fun provideNotesRepository(
        notesApi: NotesApi,
        noteDao: NoteDao,
        logger: Logger,
    ): NotesRepository = DefaultNotesRepository(notesApi, noteDao, logger)
}
