package com.example.class_project

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prompt_history")
data class PromptHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalText: String,
    val modelName: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
    val wastedTokens: Int,
    val estimatedCostUsd: Double,
    val efficiencyScore: Int,
    val issueCount: Int,
    val createdAt: Long = System.currentTimeMillis()
)
