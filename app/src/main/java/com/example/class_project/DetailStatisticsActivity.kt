package com.example.class_project

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.class_project.databinding.ActivityDetailStatisticsBinding

class DetailStatisticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailStatisticsBinding
    private val viewModel: StatisticsViewModel by viewModels {
        StatisticsViewModelFactory(
            (application as PromptDietApplication).repository
        )
    }
    private val adapter = HistoryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter

        binding.btnClearHistory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("기록 삭제")
                .setMessage("모든 분석 기록을 삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ -> viewModel.clearHistory() }
                .setNegativeButton("취소", null)
                .show()
        }

        viewModel.history.observe(this) { entries ->
            adapter.submitList(entries)
            val hasData = entries.isNotEmpty()
            binding.tvEmptyDetail.visibility = if (hasData) View.GONE else View.VISIBLE
            binding.rvHistory.visibility = if (hasData) View.VISIBLE else View.GONE
            binding.btnClearHistory.isEnabled = hasData
            binding.btnClearHistory.alpha = if (hasData) 1f else 0.5f
        }
    }
}
