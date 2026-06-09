package com.example.class_project

import androidx.lifecycle.LiveData

class PromptHistoryRepository(private val dao: PromptHistoryDao) {

    fun observeHistory(): LiveData<List<PromptHistoryWithIssues>> = dao.observeAll()

    suspend fun saveAnalysisResult(result: AnalysisResult) {
        val historyId = dao.insertHistory(
            PromptHistoryEntity(
                originalText = result.originalText,
                modelName = result.modelName,
                inputTokens = result.inputTokens,
                outputTokens = result.outputTokens,
                totalTokens = result.totalTokens,
                wastedTokens = result.wastedTokens,
                estimatedCostUsd = result.estimatedCostUsd,
                efficiencyScore = result.efficiencyScore,
                issueCount = result.issues.size
            )
        )
        if (result.issues.isNotEmpty()) {
            dao.insertIssues(result.issues.map { issue ->
                PromptIssueEntity(
                    historyId = historyId,
                    type = issue.type.name,
                    description = issue.description,
                    suggestedFix = issue.suggestedFix,
                    estimatedSavings = issue.estimatedSavings
                )
            })
        }
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}
