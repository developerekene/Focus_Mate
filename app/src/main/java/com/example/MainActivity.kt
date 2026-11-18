package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.TimerStatus
import com.example.ui.navigation.FocusMateAppScaffold
import com.example.ui.theme.FocusMateTheme
import com.example.ui.viewmodel.FocusViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as FocusMateApplication

        setContent {
            val viewModel: FocusViewModel = viewModel(
                factory = FocusViewModel.Factory(
                    timerEngine = app.timerEngine,
                    repository = app.repository,
                    settingsDataStore = app.settingsDataStore
                )
            )

            val timerState by viewModel.timerState.collectAsStateWithLifecycle()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val summary by viewModel.summary.collectAsStateWithLifecycle()
            val sessionHistory by viewModel.sessionHistory.collectAsStateWithLifecycle()

            val view = LocalView.current
            val shouldKeepScreenOn = settings.keepScreenOn && timerState.status == TimerStatus.RUNNING
            DisposableEffect(view, shouldKeepScreenOn) {
                view.keepScreenOn = shouldKeepScreenOn
                onDispose {
                    view.keepScreenOn = false
                }
            }

            val context = LocalContext.current
            var askedNotificationPermission by remember { mutableStateOf(false) }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) {
                viewModel.startOrPause()
            }

            val handleStartOrPause: () -> Unit = {
                if (
                    timerState.status != TimerStatus.RUNNING &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !askedNotificationPermission &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    askedNotificationPermission = true
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.startOrPause()
                }
            }

            FocusMateTheme(themeMode = settings.themeMode) {
                FocusMateAppScaffold(
                    timerState = timerState,
                    settings = settings,
                    summary = summary,
                    sessionHistory = sessionHistory,
                    viewModel = viewModel,
                    onStartOrPauseRequested = handleStartOrPause
                )
            }
        }
    }
}
