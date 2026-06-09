package com.example.class_project

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.class_project.databinding.ItemHistoryEntryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter : ListAdapter<PromptHistoryWithIssues, HistoryAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PromptHistoryWithIssues>() {
            override fun areItemsTheSame(old: PromptHistoryWithIssues, new: PromptHistoryWithIssues) =
                old.history.id == new.history.id

            override fun areContentsTheSame(old: PromptHistoryWithIssues, new: PromptHistoryWithIssues) =
                old.history == new.history && old.issues == new.issues
        }
    }

    private val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemHistoryEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: PromptHistoryWithIssues) {
            val h = entry.history
            binding.tvEntryDate.text = dateFormat.format(Date(h.createdAt))
            binding.tvEntryPrompt.text = h.originalText

            val score = h.efficiencyScore
            val ctx = binding.root.context
            val scoreColor = when {
                score >= 80 -> ctx.getColor(R.color.score_good)
                score >= 50 -> ctx.getColor(R.color.score_medium)
                else -> ctx.getColor(R.color.score_bad)
            }
            val scoreBgColor = (0x22 shl 24) or (scoreColor and 0x00FFFFFF)

            binding.tvEntryScore.text = "${score}점"
            binding.tvEntryScore.setTextColor(scoreColor)
            binding.tvEntryScore.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 100f
                setColor(scoreBgColor)
            }
            binding.scoreStripe.setBackgroundColor(scoreColor)

            binding.tvEntryTokens.text =
                "입력 ${h.inputTokens} · 출력 ${h.outputTokens} · 낭비 ${h.wastedTokens} · ${"%.6f".format(h.estimatedCostUsd)} USD"

            binding.tvEntryIssues.text = if (entry.issues.isEmpty()) {
                "✅ 문제 없음"
            } else {
                entry.issues.joinToString("  ·  ") { issue ->
                    when (issue.type) {
                        IssueType.REDUNDANCY.name -> "중복 표현"
                        IssueType.VERBOSITY.name -> "장황한 표현"
                        IssueType.LACK_OF_SCOPE.name -> "범위 부족"
                        else -> "기타"
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
