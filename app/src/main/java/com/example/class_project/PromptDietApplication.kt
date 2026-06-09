package com.example.class_project

import android.app.Application

class PromptDietApplication : Application() {
    val repository by lazy {
        PromptHistoryRepository(AppDatabase.getInstance(this).promptHistoryDao())
    }
}
