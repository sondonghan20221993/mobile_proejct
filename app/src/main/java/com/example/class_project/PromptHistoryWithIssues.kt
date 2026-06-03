package com.example.class_project

data class PromptHistoryWithIssues(
    val history: PromptHistoryEntity,
    val issues: List<PromptIssueEntity>
)
