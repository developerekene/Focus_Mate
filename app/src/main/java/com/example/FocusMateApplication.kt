package com.example

import android.app.Application
import com.example.data.local.FocusDatabase
import com.example.data.local.FocusRepository
import com.example.data.local.SettingsDataStore
import com.example.service.NotificationFeedbackHelper
import com.example.service.TimerEngine

class FocusMateApplication : Application() {

    val database: FocusDatabase by lazy { FocusDatabase.getInstance(this) }
    val repository: FocusRepository by lazy { FocusRepository(database.focusSessionDao()) }
    val settingsDataStore: SettingsDataStore by lazy { SettingsDataStore(this) }
    val feedbackHelper: NotificationFeedbackHelper by lazy { NotificationFeedbackHelper(this) }

    val timerEngine: TimerEngine by lazy {
        TimerEngine(
            context = this,
            repository = repository,
            settingsDataStore = settingsDataStore,
            feedbackHelper = feedbackHelper
        )
    }

    override fun onCreate() {
        super.onCreate()
        feedbackHelper.ensureChannels()
    }
}
