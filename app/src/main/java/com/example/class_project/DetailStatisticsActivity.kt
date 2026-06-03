package com.example.class_project

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import com.example.class_project.databinding.ActivityDetailStatisticsBinding

class DetailStatisticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailStatisticsBinding
    private val viewModel: StatisticsViewModel by viewModels {
        StatisticsViewModelFactory(
            (application as PromptDietApplication).repository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.history.observe(this) { entries ->
            binding.tvDetailData.text = if (entries.isEmpty()) {
                "아직 저장된 분석 이력이 없습니다."
            } else {
                entries.joinToString("\n\n") { entry ->
                    buildString {
                        append("입력: ${entry.history.originalText}\n")
                        append("기준 모델: ${entry.history.modelName}\n")
                        append("입력/출력 토큰(추정): ${entry.history.inputTokens}/${entry.history.outputTokens}\n")
                        append("효율성 점수: ${entry.history.efficiencyScore}점\n")
                        append("전체/낭비 토큰: ${entry.history.totalTokens}/${entry.history.wastedTokens}\n")
                        append("예상 비용(USD): ${"%.6f".format(entry.history.estimatedCostUsd)}\n")
                        append("탐지 이슈 수: ${entry.history.issueCount}\n")
                        if (entry.issues.isEmpty()) {
                            append("문제 없음")
                        } else {
                            append(
                                entry.issues.joinToString("\n") { issue ->
                                    "- ${issue.type}: ${issue.description}"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
