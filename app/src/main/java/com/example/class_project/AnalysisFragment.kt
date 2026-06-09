package com.example.class_project

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.class_project.databinding.FragmentAnalysisBinding

class AnalysisFragment : Fragment() {
    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnalysisViewModel by viewModels {
        AnalysisViewModelFactory(
            (requireActivity().application as PromptDietApplication).repository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        
        val analyzer = PromptAnalyzer()

        binding.btnStartAnalysis.setOnClickListener {
            val content = binding.etChatContent.text.toString()
            if (content.isNotBlank()) {
                val result = analyzer.analyze(content)
                viewModel.saveResult(result)
                displayResult(result)
                Toast.makeText(requireContext(), "분석 결과가 저장되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "분석할 프롬프트를 입력해 주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayResult(result: AnalysisResult) {
        val ssb = SpannableStringBuilder()

        fun appendBold(text: String) {
            val start = ssb.length
            ssb.append(text)
            ssb.setSpan(StyleSpan(Typeface.BOLD), start, ssb.length, 0)
        }

        fun appendColored(text: String, color: Int) {
            val start = ssb.length
            ssb.append(text)
            ssb.setSpan(ForegroundColorSpan(color), start, ssb.length, 0)
        }

        fun appendHighlighted(text: String, bgColor: Int, fgColor: Int) {
            val start = ssb.length
            ssb.append(text)
            ssb.setSpan(BackgroundColorSpan(bgColor), start, ssb.length, 0)
            ssb.setSpan(ForegroundColorSpan(fgColor), start, ssb.length, 0)
        }

        appendBold("📊 분석 결과\n")
        ssb.append("기준 모델: ${result.modelName}\n")
        ssb.append("입력 토큰(추정): ${result.inputTokens}  출력 토큰(추정): ${result.outputTokens}\n")
        ssb.append("전체 토큰(추정): ${result.totalTokens}  낭비 토큰(추정): ${result.wastedTokens}\n")
        ssb.append("예상 비용(USD): ${"%.6f".format(result.estimatedCostUsd)}\n")
        ssb.append("효율성 점수: ")

        val score = result.efficiencyScore
        val scoreColor = when {
            score >= 80 -> 0xFF1B7C4A.toInt()
            score >= 50 -> 0xFFB45309.toInt()
            else -> 0xFFB91C1C.toInt()
        }
        appendHighlighted(" ${score}점 ", 0x22000000 or (scoreColor and 0x00FFFFFF), scoreColor)
        ssb.append("\n\n")

        if (result.issues.isEmpty()) {
            appendColored("✅ 탐지된 비효율 패턴이 없습니다. 아주 깔끔한 프롬프트입니다!", 0xFF1B7C4A.toInt())
        } else {
            appendBold("🔍 탐지된 문제점:\n")
            result.issues.forEachIndexed { index, issue ->
                val issueColor = when (issue.type) {
                    IssueType.REDUNDANCY -> 0xFF1E3A5F.toInt()
                    IssueType.VERBOSITY -> 0xFF2E7D6E.toInt()
                    IssueType.LACK_OF_SCOPE -> 0xFF7B4F9E.toInt()
                    else -> 0xFF6B7280.toInt()
                }
                val typeLabel = when (issue.type) {
                    IssueType.REDUNDANCY -> "중복 표현"
                    IssueType.VERBOSITY -> "장황한 표현"
                    IssueType.LACK_OF_SCOPE -> "범위 부족"
                    else -> "기타"
                }
                ssb.append("${index + 1}. ")
                appendHighlighted(" $typeLabel ", (0x33000000 or (issueColor and 0x00FFFFFF)), issueColor)
                ssb.append("\n")
                ssb.append("   ${issue.description}\n")
                appendColored("   💡 ${issue.suggestedFix}\n", 0xFF374151.toInt())
                if (issue.estimatedSavings > 0) {
                    ssb.append("   📉 절약 가능 토큰: ${issue.estimatedSavings}\n")
                }
                ssb.append("\n")
            }

            // 원문에서 중복 단어 하이라이트
            val redundancyIssue = result.issues.find { it.type == IssueType.REDUNDANCY }
            if (redundancyIssue != null) {
                ssb.append("─────────────────\n")
                appendBold("📝 원문 (중복 표현 강조)\n")
                val originalSsb = SpannableStringBuilder(result.originalText)
                val words = result.originalText.lowercase()
                    .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
                    .split(Regex("\\s+"))
                    .filter { it.length >= 2 }
                    .groupingBy { it }
                    .eachCount()
                    .filter { it.value >= 3 }
                    .keys
                words.forEach { word ->
                    var start = result.originalText.lowercase().indexOf(word)
                    while (start >= 0) {
                        originalSsb.setSpan(BackgroundColorSpan(0x331E3A5F), start, start + word.length, 0)
                        originalSsb.setSpan(ForegroundColorSpan(0xFF1E3A5F.toInt()), start, start + word.length, 0)
                        start = result.originalText.lowercase().indexOf(word, start + 1)
                    }
                }
                ssb.append(originalSsb)
                ssb.append("\n")
            }
        }

        binding.tvAnalysisResult.text = ssb
        if (binding.svResultContainer.visibility != View.VISIBLE) {
            binding.svResultContainer.visibility = View.VISIBLE
            binding.svResultContainer.startAnimation(
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
