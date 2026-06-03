package com.example.class_project

import kotlin.math.ceil

data class TokenUsage(
    val modelName: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
    val estimatedCostUsd: Double
)

data class ModelTokenProfile(
    val modelName: String,
    val charactersPerInputToken: Double,
    val charactersPerOutputToken: Double,
    val inputCostPerMillionTokensUsd: Double,
    val outputCostPerMillionTokensUsd: Double
)

class TokenUsageEstimator(
    private val profile: ModelTokenProfile = DEFAULT_MODEL_PROFILE
) {
    fun estimate(inputText: String, predictedOutputText: String): TokenUsage {
        val inputTokens = estimateTokens(inputText, profile.charactersPerInputToken)
        val outputTokens = estimateTokens(predictedOutputText, profile.charactersPerOutputToken)
        val estimatedCostUsd =
            (inputTokens / 1_000_000.0) * profile.inputCostPerMillionTokensUsd +
                (outputTokens / 1_000_000.0) * profile.outputCostPerMillionTokensUsd

        return TokenUsage(
            modelName = profile.modelName,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            totalTokens = inputTokens + outputTokens,
            estimatedCostUsd = estimatedCostUsd
        )
    }

    private fun estimateTokens(text: String, charactersPerToken: Double): Int {
        val normalizedLength = text.trim().length.coerceAtLeast(1)
        return ceil(normalizedLength / charactersPerToken).toInt().coerceAtLeast(1)
    }

    companion object {
        val DEFAULT_MODEL_PROFILE = ModelTokenProfile(
            modelName = "gpt-4o-mini-estimate",
            charactersPerInputToken = 2.6,
            charactersPerOutputToken = 3.2,
            inputCostPerMillionTokensUsd = 0.15,
            outputCostPerMillionTokensUsd = 0.60
        )
    }
}
