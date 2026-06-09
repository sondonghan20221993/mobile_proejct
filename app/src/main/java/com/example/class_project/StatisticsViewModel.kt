package com.example.class_project

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class StatisticsUiState(
    val totalAnalyses: Int,
    val averageEfficiencyScore: Int,
    val bestEfficiencyScore: Int,
    val totalInputTokens: Int,
    val totalOutputTokens: Int,
    val totalWastedTokens: Int,
    val totalEstimatedCostUsd: Double,
    val topIssueLabel: String,
    val issueBreakdownLabel: String,
    val recentTrendLabel: String,
    val emptyStateLabel: String,
    val hasData: Boolean,
    val chartEntries: List<IssueBarChartView.BarEntry> = emptyList()
)

class StatisticsViewModel(
    private val repository: PromptHistoryRepository
) : ViewModel() {
    val history: LiveData<List<PromptHistoryWithIssues>> = repository.observeHistory()

    fun clearHistory() {
        viewModelScope.launch { repository.deleteAll() }
    }
    val statisticsState = MediatorLiveData<StatisticsUiState>().apply {
        addSource(history) { entries ->
            value = entries.toStatisticsUiState()
        }
    }

    private fun List<PromptHistoryWithIssues>.toStatisticsUiState(): StatisticsUiState {
        if (isEmpty()) {
            return StatisticsUiState(
                totalAnalyses = 0,
                averageEfficiencyScore = 0,
                bestEfficiencyScore = 0,
                totalInputTokens = 0,
                totalOutputTokens = 0,
                totalWastedTokens = 0,
                totalEstimatedCostUsd = 0.0,
                topIssueLabel = "아직 분석 데이터가 없습니다.",
                issueBreakdownLabel = "분석 탭에서 첫 프롬프트를 분석해 보세요.",
                recentTrendLabel = "최근 분석 데이터가 없습니다.",
                emptyStateLabel = "아직 저장된 분석 기록이 없습니다.\n분석 탭에서 프롬프트를 입력하고 첫 결과를 만들어 보세요.",
                hasData = false
            )
        }

        val issueCounts = flatMap { it.issues }
            .groupingBy { it.type }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }

        val topIssue = issueCounts.firstOrNull()
        val averageScore = map { it.history.efficiencyScore }.average().toInt()
        val bestScore = maxOf { it.history.efficiencyScore }
        val totalInputTokens = sumOf { it.history.inputTokens }
        val totalOutputTokens = sumOf { it.history.outputTokens }
        val totalWastedTokens = sumOf { it.history.wastedTokens }
        val totalEstimatedCostUsd = sumOf { it.history.estimatedCostUsd }
        val recentScores = take(3).map { it.history.efficiencyScore }
        val breakdown = if (issueCounts.isEmpty()) {
            "탐지된 문제가 없습니다."
        } else {
            issueCounts.joinToString("\n") { (type, count) -> "${type.toDisplayName()}: ${count}회" }
        }
        val trendLabel = when {
            recentScores.size < 2 -> "추세를 판단할 만큼 데이터가 충분하지 않습니다."
            recentScores.first() > recentScores.last() -> "최근 분석이 이전 기록보다 개선되고 있습니다."
            recentScores.first() < recentScores.last() -> "최근 분석 효율이 다소 낮아졌습니다. 원인 분포를 확인해 보세요."
            else -> "최근 분석 효율이 비슷한 수준을 유지하고 있습니다."
        }

        val countMap = issueCounts.toMap()
        val chartEntries = listOf(
            IssueBarChartView.BarEntry("중복", countMap[IssueType.REDUNDANCY.name] ?: 0, 0xFF1E3A5F.toInt()),
            IssueBarChartView.BarEntry("장황", countMap[IssueType.VERBOSITY.name] ?: 0, 0xFF2E7D6E.toInt()),
            IssueBarChartView.BarEntry("범위", countMap[IssueType.LACK_OF_SCOPE.name] ?: 0, 0xFF7B4F9E.toInt())
        )

        return StatisticsUiState(
            totalAnalyses = size,
            averageEfficiencyScore = averageScore,
            bestEfficiencyScore = bestScore,
            totalInputTokens = totalInputTokens,
            totalOutputTokens = totalOutputTokens,
            totalWastedTokens = totalWastedTokens,
            totalEstimatedCostUsd = totalEstimatedCostUsd,
            topIssueLabel = topIssue?.let { "${it.first.toDisplayName()} (${it.second}회)" } ?: "탐지된 문제가 없습니다.",
            issueBreakdownLabel = breakdown,
            recentTrendLabel = trendLabel,
            emptyStateLabel = "",
            hasData = true,
            chartEntries = chartEntries
        )
    }
}

class StatisticsViewModelFactory(
    private val repository: PromptHistoryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
            return StatisticsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

private fun String.toDisplayName(): String {
    return when (this) {
        IssueType.REDUNDANCY.name -> "중복 표현"
        IssueType.VERBOSITY.name -> "장황한 표현"
        IssueType.LACK_OF_SCOPE.name -> "범위 부족"
        else -> "기타"
    }
}
