package com.factory.feature.home.data

import kotlinx.serialization.Serializable
import retrofit2.http.GET

@Serializable
data class NoteDto(
    val id: String,
    val title: String,
    val body: String,
    val createdAtEpochMillis: Long,
)

/**
 * Example endpoint demonstrating the Retrofit + kotlinx.serialization setup end-to-end.
 * Points at `EnvironmentConfig.baseUrl` + "notes" — replace with your real API when
 * building a real app from this factory; this is not meant to survive into production.
 */
interface NotesApi {
    @GET("notes")
    suspend fun getNotes(): List<NoteDto>
}
