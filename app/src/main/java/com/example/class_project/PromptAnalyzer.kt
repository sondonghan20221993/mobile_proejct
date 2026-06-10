package com.example.class_project

class PromptAnalyzer(
    private val dictionary: PromptKeywordDictionary = PromptKeywordDictionary.default(),
    private val tokenUsageEstimator: TokenUsageEstimator = TokenUsageEstimator()
) {

    fun analyze(text: String): AnalysisResult {
        val normalizedText = normalizeText(text)
        val signals = extractSignals(text, normalizedText)
        val issues = buildList {
            detectRedundancy(signals)?.let(::add)
            detectVerbosity(signals)?.let(::add)
            detectScopeIssues(signals)?.let(::add)
        }

        val predictedOutputText = buildPredictedOutputText(issues)
        val tokenUsage = tokenUsageEstimator.estimate(text, predictedOutputText)
        val wastedTokens = issues.sumOf { it.estimatedSavings }.coerceAtMost(tokenUsage.totalTokens)
        val efficiencyScore = calculateEfficiencyScore(issues)

        return AnalysisResult(
            originalText = text,
            modelName = tokenUsage.modelName,
            inputTokens = tokenUsage.inputTokens,
            outputTokens = tokenUsage.outputTokens,
            totalTokens = tokenUsage.totalTokens,
            wastedTokens = wastedTokens,
            estimatedCostUsd = tokenUsage.estimatedCostUsd,
            efficiencyScore = efficiencyScore,
            issues = issues
        )
    }

    private fun buildPredictedOutputText(issues: List<AnalysisIssue>): String {
        if (issues.isEmpty()) {
            return "탐지된 비효율 패턴이 없습니다. 아주 깔끔한 프롬프트입니다."
        }

        return buildString {
            append("분석 결과 ")
            issues.forEach { issue ->
                append(issue.description)
                append(' ')
                append(issue.suggestedFix)
                append(' ')
            }
        }.trim()
    }

    private fun normalizeText(text: String): String {
        return text
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractSignals(originalText: String, normalizedText: String): PromptSignals {
        val rawTokens = normalizedText.split(" ").filter { it.isNotBlank() }
        val normalizedTokens = rawTokens
            .map { token -> token.trim() }
            .filter { token -> token.length >= 2 }
            .map { token -> simplifyToken(token) }
        val sentences = originalText
            .split(Regex("[.!?\\n]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return PromptSignals(
            originalText = originalText,
            normalizedText = normalizedText,
            rawTokens = rawTokens,
            normalizedTokens = normalizedTokens,
            sentences = sentences
        )
    }

    private fun simplifyToken(token: String): String {
        val suffixes = listOf(
            "해주세요", "해줘",          // 정중 요청 (긴 것 우선)
            "했어요", "했어",            // 과거형 (긴 것 우선)
            "합니다", "한다",            // 공식 현재형
            "해요", "해서", "하면", "하여", "하는", "하고", "하다",  // 동사 활용형
            "적인지", "적인",            // 형용사형 (긴 것 우선)
            "입니다",                    // 공식 계사
            "으로", "에서", "에게", "까지", "부터", "보다", "처럼"  // 조사
        )
        val suffixStripped = suffixes.firstOrNull { token.endsWith(it) && token.length > it.length + 1 }
            ?.let { token.removeSuffix(it) }
            ?: token
        return dictionary.synonymGroups[suffixStripped] ?: suffixStripped
    }

    private fun detectRedundancy(signals: PromptSignals): AnalysisIssue? {
        val duplicates = signals.normalizedTokens
            .groupingBy { it }
            .eachCount()
            .filter { (token, count) -> token.length >= 2 && count >= 3 }
            .toList()
            .sortedByDescending { it.second }
        val repeatedSentences = signals.sentences
            .groupingBy { it.lowercase() }
            .eachCount()
            .filter { (_, count) -> count >= 2 }
            .keys

        if (duplicates.isNotEmpty() || repeatedSentences.isNotEmpty()) {
            val duplicateWordsLabel = duplicates.take(3).joinToString(", ") { "${it.first}(${it.second})" }
            val repeatedSentenceLabel = if (repeatedSentences.isNotEmpty()) "반복 문장 ${repeatedSentences.size}개" else null
            val evidence = listOfNotNull(
                duplicateWordsLabel.takeIf { it.isNotBlank() },
                repeatedSentenceLabel
            ).joinToString(", ")

            val redundancyFix = if (duplicates.isNotEmpty()) {
                val topWord = duplicates.first().first
                "'$topWord' 등 반복 단어를 한 번만 쓰거나 대명사로 대체하세요."
            } else {
                "반복되는 문장을 하나로 통합하세요."
            }
            return AnalysisIssue(
                type = IssueType.REDUNDANCY,
                description = "같은 표현이 반복되어 핵심 요청이 흐려집니다: $evidence",
                suggestedFix = redundancyFix,
                estimatedSavings = (duplicates.sumOf { it.second - 1 } + repeatedSentences.size * 2).coerceAtLeast(2)
            )
        }

        return null
    }

    private fun detectVerbosity(signals: PromptSignals): AnalysisIssue? {
        val foundFillers = dictionary.fillerPhrases.filter { filler ->
            if (!signals.originalText.contains(filler, ignoreCase = true)) return@filter false
            if (filler !in dictionary.conditionalFillerPhrases) return@filter true
            // 조건부 filler: 뒤에 출력 형식/제약 키워드가 따라오면 조건문으로 판단해 제외
            val idx = signals.originalText.lowercase().indexOf(filler.lowercase())
            val window = signals.originalText.substring(idx + filler.length).take(20).lowercase()
            val isConditional = (dictionary.outputFormatKeywords + dictionary.constraintKeywords)
                .any { keyword ->
                    val ki = window.indexOf(keyword)
                    ki >= 0 && (ki == 0 || window[ki - 1] == ' ')
                }
            !isConditional
        }
        val fillerDensity = if (signals.rawTokens.isNotEmpty()) {
            foundFillers.size.toFloat() / signals.rawTokens.size
        } else {
            0f
        }
        val foundConnectors = dictionary.connectorKeywords
            .filter { signals.originalText.contains(it) }
            .sortedByDescending { it.length }
        val repeatedConnectors = foundConnectors.count { candidate ->
            foundConnectors.none { longer ->
                longer.length > candidate.length && longer.contains(candidate)
            }
        }

        if (foundFillers.size >= 2 || fillerDensity >= 0.15f || repeatedConnectors >= 2) {
            val evidence = buildList {
                if (foundFillers.isNotEmpty()) {
                    add("수식어 ${foundFillers.joinToString(", ")}")
                }
                if (repeatedConnectors >= 2) {
                    add("연결 표현 반복")
                }
            }.joinToString(", ")

            val verbosityFix = if (foundFillers.isNotEmpty()) {
                val fillerList = foundFillers.take(2).joinToString(", ") { "'$it'" }
                if (repeatedConnectors >= 2) "$fillerList 같은 수식어를 제거하고, 요청도 하나로 압축하세요."
                else "$fillerList 같은 수식어를 제거하고 핵심만 전달하세요."
            } else {
                "연결 표현을 줄여 하나의 명확한 요청으로 압축하세요."
            }
            return AnalysisIssue(
                type = IssueType.VERBOSITY,
                description = "핵심 요청보다 설명성 표현이 많습니다: $evidence",
                suggestedFix = verbosityFix,
                estimatedSavings = (foundFillers.size + repeatedConnectors).coerceAtLeast(2)
            )
        }

        return null
    }

    private fun detectScopeIssues(signals: PromptSignals): AnalysisIssue? {
        if (signals.normalizedText.isBlank()) {
            return null
        }

        val hasAction = dictionary.actionKeywords.any { signals.normalizedText.contains(it) }
        val hasOutputFormat = dictionary.outputFormatKeywords.any { signals.normalizedText.contains(it) }
        val hasConstraint = dictionary.constraintKeywords.any { signals.normalizedText.contains(it) }
        val hasContext = dictionary.contextKeywords.any { signals.normalizedText.contains(it) } || signals.rawTokens.size >= 8
        val missingSignals = mutableListOf<String>()

        if (!hasAction) missingSignals.add("목표")
        if (!hasOutputFormat) missingSignals.add("출력 형식")
        if (!hasConstraint) missingSignals.add("제약 조건")
        if (!hasContext) missingSignals.add("맥락 정보")

        val isShortButPrecise = hasAction && hasOutputFormat && signals.rawTokens.size in 3..8
        if (missingSignals.size >= 2 && !isShortButPrecise) {
            val description = if (signals.rawTokens.size <= 2) {
                "요청이 너무 짧아 의도를 파악하기 어렵습니다."
            } else {
                "요청에 필요한 정보가 부족합니다: ${missingSignals.joinToString(", ")}"
            }

            val scopeFix = buildString {
                val hints = buildList {
                    if (!hasAction) add("무엇을 해달라는지(분석·요약·설명 등)")
                    if (!hasOutputFormat) add("어떤 형식으로(리스트·표·단계별 등)")
                    if (!hasContext) add("어떤 상황인지 배경")
                }
                if (hints.isNotEmpty()) append(hints.joinToString(", ") + "을 추가하세요.")
                else append("구체적인 배경 정보나 원하는 답변 형식을 추가하세요.")
            }
            return AnalysisIssue(
                type = IssueType.LACK_OF_SCOPE,
                description = description,
                suggestedFix = scopeFix,
                estimatedSavings = 0
            )
        }

        return null
    }

    private fun calculateEfficiencyScore(issues: List<AnalysisIssue>): Int {
        val penalty = issues.sumOf { issue ->
            when (issue.type) {
                IssueType.REDUNDANCY -> 12
                IssueType.VERBOSITY -> 8
                IssueType.LACK_OF_SCOPE -> 15
                IssueType.OTHER -> 5
            }
        }
        return (100 - penalty).coerceIn(0, 100)
    }
}

private data class PromptSignals(
    val originalText: String,
    val normalizedText: String,
    val rawTokens: List<String>,
    val normalizedTokens: List<String>,
    val sentences: List<String>
)
