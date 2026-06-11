package com.example.class_project

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.class_project.databinding.FragmentStatisticsBinding

class StatisticsFragment : Fragment() {
    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatisticsViewModel by viewModels {
        StatisticsViewModelFactory(
            (requireActivity().application as PromptDietApplication).repository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)

        binding.boxDetailCauses.setOnClickListener {
            findNavController().navigate(R.id.action_to_detail_statistics)
        }

        viewModel.statisticsState.observe(viewLifecycleOwner) { state ->
            binding.tvTotalAnalysesValue.text = state.totalAnalyses.toString()
            binding.tvAverageScoreValue.text = "${state.averageEfficiencyScore}점\n$${"%.6f".format(state.totalEstimatedCostUsd)}"
            binding.tvBestScoreValue.text = "${state.bestEfficiencyScore}점"
            binding.tvWastedTokensValue.text =
                "입력 ${state.totalInputTokens}\n출력 ${state.totalOutputTokens}\n낭비 ${state.totalWastedTokens}"
            binding.tvTopIssueValue.text = state.topIssueLabel
            binding.tvTrendValue.text = state.recentTrendLabel
            binding.tvBreakdownValue.text = state.issueBreakdownLabel
            binding.tvEmptyState.text = state.emptyStateLabel
            binding.chartIssueBreakdown.setEntries(state.chartEntries)
            binding.layoutStatsContent.visibility = if (state.hasData) View.VISIBLE else View.GONE
            binding.tvEmptyState.visibility = if (state.hasData) View.GONE else View.VISIBLE
            binding.boxDetailCauses.text =
                if (state.hasData) "세부 추정 원인 보기" else "분석 데이터가 쌓이면 상세 통계를 볼 수 있습니다."
            binding.boxDetailCauses.isEnabled = state.hasData
            binding.boxDetailCauses.alpha = if (state.hasData) 1f else 0.6f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
