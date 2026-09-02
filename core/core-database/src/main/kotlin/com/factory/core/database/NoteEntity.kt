package com.factory.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Example entity demonstrating the Room setup end-to-end (see `feature-home`, which is
 * the factory's "networking + Room" example screen). Apps built from this factory
 * replace this with their own entities; it is not meant to survive into a real product.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val createdAtEpochMillis: Long,
)
