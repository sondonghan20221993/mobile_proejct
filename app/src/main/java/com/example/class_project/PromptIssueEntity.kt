package com.example.class_project

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prompt_issues",
    foreignKeys = [ForeignKey(
        entity = PromptHistoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["historyId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("historyId")]
)
data class PromptIssueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val historyId: Long,
    val type: String,
    val description: String,
    val suggestedFix: String,
    val estimatedSavings: Int
)
