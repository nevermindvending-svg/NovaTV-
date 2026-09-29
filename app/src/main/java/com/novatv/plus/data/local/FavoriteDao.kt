package com.novatv.plus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_channels ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT streamUrl FROM favorite_channels")
    fun getAllFavoriteUrls(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_channels WHERE streamUrl = :streamUrl)")
    suspend fun isFavorite(streamUrl: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorite_channels WHERE streamUrl = :streamUrl")
    suspend fun deleteFavorite(streamUrl: String)

    @Query("DELETE FROM favorite_channels WHERE channelId = :channelId")
    suspend fun deleteFavoriteById(channelId: Int)

    @Query("SELECT COUNT(*) FROM favorite_channels")
    suspend fun getCount(): Int
}
