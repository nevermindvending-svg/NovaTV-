package com.novatv.plus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity for storing user's favorited channels persistently.
 * Using streamUrl as PrimaryKey ensures favorites are never forgotten,
 * even across app restarts, channel re-indexing, or network playlist updates.
 */
@Entity(tableName = "favorite_channels")
data class FavoriteEntity(
    @PrimaryKey
    val streamUrl: String,
    val channelId: Int,
    val name: String,
    val logoUrl: String?,
    val category: String,
    val programTitle: String?,
    val addedAt: Long = System.currentTimeMillis()
)
