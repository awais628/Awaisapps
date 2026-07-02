package com.example.db

import kotlinx.coroutines.flow.Flow

class ZenRepository(private val moodDao: MoodDao) {
    val allEntries: Flow<List<MoodEntry>> = moodDao.getAllEntries()

    suspend fun insert(entry: MoodEntry): Long {
        return moodDao.insertEntry(entry)
    }

    suspend fun deleteById(id: Int) {
        moodDao.deleteById(id)
    }

    suspend fun clearAll() {
        moodDao.clearAll()
    }
}
