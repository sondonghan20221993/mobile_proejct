package com.example.class_project

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONArray
import org.json.JSONObject

class PromptHistoryRepository(
    context: Context
) {
    private val sharedPreferences =
        context.getSharedPreferences("prompt_history_storage", Context.MODE_PRIVATE)
    private val historyLiveData = MutableLiveData(loadHistory())

    fun observeHistory(): LiveData<List<PromptHistoryWithIssues>> = historyLiveData

    suspend fun saveAnalysisResult(result: AnalysisResult) {
        val currentHistory = historyLiveData.value.orEmpty()
        val historyId = (currentHistory.maxOfOrNull { it.history.id } ?: 0L) + 1L
        val nextIssueId = currentHistory.flatMap { it.issues }.maxOfOrNull { it.id } ?: 0L

        val newEntry = PromptHistoryWithIssues(
            history = PromptHistoryEntity(
                id = historyId,
                originalText = result.originalText,
                modelName = result.modelName,
                inputTokens = result.inputTokens,
                outputTokens = result.outputTokens,
                totalTokens = result.totalTokens,
                wastedTokens = result.wastedTokens,
                estimatedCostUsd = result.estimatedCostUsd,
                efficiencyScore = result.efficiencyScore,
                issueCount = result.issues.size
            ),
            issues = result.issues.mapIndexed { index, issue ->
                PromptIssueEntity(
                    id = nextIssueId + index + 1L,
                    historyId = historyId,
                    type = issue.type.name,
                    description = issue.description,
                    suggestedFix = issue.suggestedFix,
                    estimatedSavings = issue.estimatedSavings
                )
            }
        )

        val updatedHistory = listOf(newEntry) + currentHistory
        persistHistory(updatedHistory)
        historyLiveData.postValue(updatedHistory)
    }

    private fun loadHistory(): List<PromptHistoryWithIssues> {
        val rawJson = sharedPreferences.getString(HISTORY_KEY, null) ?: return emptyList()
        val historyArray = JSONArray(rawJson)

        return buildList {
            for (index in 0 until historyArray.length()) {
                val entryObject = historyArray.getJSONObject(index)
                val historyObject = entryObject.getJSONObject("history")
                val issuesArray = entryObject.getJSONArray("issues")

                add(
                    PromptHistoryWithIssues(
                        history = PromptHistoryEntity(
                            id = historyObject.getLong("id"),
                            originalText = historyObject.getString("originalText"),
                            modelName = historyObject.optString("modelName", TokenUsageEstimator.DEFAULT_MODEL_PROFILE.modelName),
                            inputTokens = historyObject.optInt("inputTokens", historyObject.optInt("totalTokens", 0)),
                            outputTokens = historyObject.optInt("outputTokens", 0),
                            totalTokens = historyObject.getInt("totalTokens"),
                            wastedTokens = historyObject.getInt("wastedTokens"),
                            estimatedCostUsd = historyObject.optDouble("estimatedCostUsd", 0.0),
                            efficiencyScore = historyObject.getInt("efficiencyScore"),
                            issueCount = historyObject.getInt("issueCount"),
                            createdAt = historyObject.getLong("createdAt")
                        ),
                        issues = buildList {
                            for (issueIndex in 0 until issuesArray.length()) {
                                val issueObject = issuesArray.getJSONObject(issueIndex)
                                add(
                                    PromptIssueEntity(
                                        id = issueObject.getLong("id"),
                                        historyId = issueObject.getLong("historyId"),
                                        type = issueObject.getString("type"),
                                        description = issueObject.getString("description"),
                                        suggestedFix = issueObject.getString("suggestedFix"),
                                        estimatedSavings = issueObject.getInt("estimatedSavings")
                                    )
                                )
                            }
                        }
                    )
                )
            }
        }
    }

    private fun persistHistory(history: List<PromptHistoryWithIssues>) {
        val historyArray = JSONArray()

        history.forEach { entry ->
            val entryObject = JSONObject().apply {
                put(
                    "history",
                    JSONObject().apply {
                        put("id", entry.history.id)
                        put("originalText", entry.history.originalText)
                        put("modelName", entry.history.modelName)
                        put("inputTokens", entry.history.inputTokens)
                        put("outputTokens", entry.history.outputTokens)
                        put("totalTokens", entry.history.totalTokens)
                        put("wastedTokens", entry.history.wastedTokens)
                        put("estimatedCostUsd", entry.history.estimatedCostUsd)
                        put("efficiencyScore", entry.history.efficiencyScore)
                        put("issueCount", entry.history.issueCount)
                        put("createdAt", entry.history.createdAt)
                    }
                )
                put(
                    "issues",
                    JSONArray().apply {
                        entry.issues.forEach { issue ->
                            put(
                                JSONObject().apply {
                                    put("id", issue.id)
                                    put("historyId", issue.historyId)
                                    put("type", issue.type)
                                    put("description", issue.description)
                                    put("suggestedFix", issue.suggestedFix)
                                    put("estimatedSavings", issue.estimatedSavings)
                                }
                            )
                        }
                    }
                )
            }

            historyArray.put(entryObject)
        }

        sharedPreferences.edit().putString(HISTORY_KEY, historyArray.toString()).apply()
    }

    companion object {
        private const val HISTORY_KEY = "history_json"
    }
}
