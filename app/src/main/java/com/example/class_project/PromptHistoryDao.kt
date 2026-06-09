package com.example.class_project

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface PromptHistoryDao {

    @Transaction
    @Query("SELECT * FROM prompt_history ORDER BY createdAt DESC")
    fun observeAll(): LiveData<List<PromptHistoryWithIssues>>

    @Insert
    suspend fun insertHistory(history: PromptHistoryEntity): Long

    @Insert
    suspend fun insertIssues(issues: List<PromptIssueEntity>)

    @Query("DELETE FROM prompt_history")
    suspend fun deleteAll()
}
