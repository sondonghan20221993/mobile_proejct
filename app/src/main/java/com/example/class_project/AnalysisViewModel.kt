package com.example.class_project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class AnalysisViewModel(
    private val repository: PromptHistoryRepository
) : ViewModel() {
    fun saveResult(result: AnalysisResult) {
        viewModelScope.launch {
            repository.saveAnalysisResult(result)
        }
    }
}

class AnalysisViewModelFactory(
    private val repository: PromptHistoryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalysisViewModel::class.java)) {
            return AnalysisViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
