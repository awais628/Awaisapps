package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mood: String, // RAD, HAPPY, NEUTRAL, SAD, AWFUL
    val journalText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val aiReflection: String? = null,
    val aiRecommendation: String? = null,
    val tag: String? = null
)
