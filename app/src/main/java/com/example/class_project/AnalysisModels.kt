package com.example.class_project

data class AnalysisResult(
    val originalText: String,
    val modelName: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
    val wastedTokens: Int,
    val estimatedCostUsd: Double,
    val efficiencyScore: Int, // 0-100
    val issues: List<AnalysisIssue>
)

data class AnalysisIssue(
    val type: IssueType,
    val description: String,
    val suggestedFix: String,
    val estimatedSavings: Int
)

enum class IssueType {
    REDUNDANCY,    // 중복 요청
    VERBOSITY,     // 장황한 표현
    LACK_OF_SCOPE, // 범위 없는 요청
    OTHER
}
