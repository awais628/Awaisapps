package com.example.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {
    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<MoodEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: MoodEntry): Long

    @Delete
    suspend fun deleteEntry(entry: MoodEntry)

    @Query("DELETE FROM mood_entries WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM mood_entries")
    suspend fun clearAll()
}
