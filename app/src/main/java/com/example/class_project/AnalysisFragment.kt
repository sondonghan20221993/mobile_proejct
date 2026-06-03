package com.example.class_project

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        val resultText = StringBuilder()
        resultText.append("📊 분석 결과\n")
        resultText.append("기준 모델: ${result.modelName}\n")
        resultText.append("입력 토큰(추정): ${result.inputTokens}\n")
        resultText.append("출력 토큰(추정): ${result.outputTokens}\n")
        resultText.append("효율성 점수: ${result.efficiencyScore}점\n")
        resultText.append("전체 토큰(추정): ${result.totalTokens}\n")
        resultText.append("낭비된 토큰(추정): ${result.wastedTokens}\n\n")
        resultText.append("예상 비용(USD): ${"%.6f".format(result.estimatedCostUsd)}\n\n")

        if (result.issues.isEmpty()) {
            resultText.append("✅ 탐지된 비효율 패턴이 없습니다. 아주 깔끔한 프롬프트입니다!")
        } else {
            resultText.append("🔍 탐지된 문제점:\n")
            result.issues.forEachIndexed { index, issue ->
                resultText.append("${index + 1}. [${issue.type}] ${issue.description}\n")
                resultText.append("   💡 개선 제안: ${issue.suggestedFix}\n")
                resultText.append("   📉 절약 가능 토큰: ${issue.estimatedSavings}\n\n")
            }
        }

        binding.tvAnalysisResult.text = resultText.toString()
        binding.svResultContainer.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
