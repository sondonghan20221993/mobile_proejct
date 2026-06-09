package com.example.class_project

import androidx.room.Embedded
import androidx.room.Relation

data class PromptHistoryWithIssues(
    @Embedded val history: PromptHistoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "historyId"
    )
    val issues: List<PromptIssueEntity> = emptyList()
)
