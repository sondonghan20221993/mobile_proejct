package com.example.class_project

data class PromptIssueEntity(
    val historyId: Long,
    val type: String,
    val description: String,
    val suggestedFix: String,
    val estimatedSavings: Int,
    val id: Long = 0
)
