package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.ThemeMode
import com.example.model.TimerSettings
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: TimerSettings,
    onFocusMinutesChange: (Int) -> Unit,
    onShortBreakMinutesChange: (Int) -> Unit,
    onLongBreakMinutesChange: (Int) -> Unit,
    onSessionsUntilLongBreakChange: (Int) -> Unit,
    onDailyGoalChange: (Int) -> Unit,
    onAutoStartBreaksChange: (Boolean) -> Unit,
    onAutoStartFocusChange: (Boolean) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onTestAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .testTag("settings_screen_title")
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.notif_permission_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = stringResource(R.string.notif_permission_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f)
                            )
                            Button(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                                modifier = Modifier.testTag("grant_notification_permission_button")
                            ) {
                                Text(stringResource(R.string.notif_permission_action))
                            }
                        }
                    }
                }
            }

            // Appearance / Dark Mode
            item {
                SettingsSectionCard(title = stringResource(R.string.settings_section_appearance)) {
                    ThemeModeSelector(
                        selected = settings.themeMode,
                        onSelect = onThemeModeChange
                    )
                }
            }

            // Timer Durations
            item {
                SettingsSectionCard(title = stringResource(R.string.settings_section_durations)) {
                    SliderSettingItem(
                        label = stringResource(R.string.settings_focus_duration),
                        valueText = stringResource(R.string.minutes_unit, settings.focusMinutes),
                        value = settings.focusMinutes.toFloat(),
                        valueRange = 5f..90f,
                        onValueChange = { onFocusMinutesChange(it.roundToInt()) },
                        testTag = "slider_focus_minutes"
                    )
                    SliderSettingItem(
                        label = stringResource(R.string.settings_short_break_duration),
                        valueText = stringResource(R.string.minutes_unit, settings.shortBreakMinutes),
                        value = settings.shortBreakMinutes.toFloat(),
                        valueRange = 1f..30f,
                        onValueChange = { onShortBreakMinutesChange(it.roundToInt()) },
                        testTag = "slider_short_break_minutes"
                    )
                    SliderSettingItem(
                        label = stringResource(R.string.settings_long_break_duration),
                        valueText = stringResource(R.string.minutes_unit, settings.longBreakMinutes),
                        value = settings.longBreakMinutes.toFloat(),
                        valueRange = 5f..60f,
                        onValueChange = { onLongBreakMinutesChange(it.roundToInt()) },
                        testTag = "slider_long_break_minutes"
                    )
                    SliderSettingItem(
                        label = stringResource(R.string.settings_long_break_interval),
                        valueText = "${settings.sessionsUntilLongBreak} rounds",
                        value = settings.sessionsUntilLongBreak.toFloat(),
                        valueRange = 2f..8f,
                        steps = 5,
                        onValueChange = { onSessionsUntilLongBreakChange(it.roundToInt()) },
                        testTag = "slider_long_break_interval"
                    )
                }
            }

            // Daily Focus Goal
            item {
                SettingsSectionCard(title = stringResource(R.string.settings_section_goals)) {
                    val targetMinutes = settings.dailyGoalSessions * settings.focusMinutes
                    val hrs = targetMinutes / 60
                    val mins = targetMinutes % 60
                    val summaryLabel = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
                    SliderSettingItem(
                        label = stringResource(R.string.settings_daily_goal_sessions),
                        valueText = "${settings.dailyGoalSessions} ($summaryLabel)",
                        value = settings.dailyGoalSessions.toFloat(),
                        valueRange = 1f..16f,
                        steps = 14,
                        onValueChange = { onDailyGoalChange(it.roundToInt()) },
                        testTag = "slider_daily_goal"
                    )
                }
            }

            // Flow & Screen
            item {
                SettingsSectionCard(title = stringResource(R.string.settings_section_automation)) {
                    SwitchSettingRow(
                        label = stringResource(R.string.settings_auto_start_breaks),
                        checked = settings.autoStartBreaks,
                        onCheckedChange = onAutoStartBreaksChange,
                        testTag = "switch_auto_start_breaks"
                    )
                    SwitchSettingRow(
                        label = stringResource(R.string.settings_auto_start_focus),
                        checked = settings.autoStartFocus,
                        onCheckedChange = onAutoStartFocusChange,
                        testTag = "switch_auto_start_focus"
                    )
                    SwitchSettingRow(
                        label = stringResource(R.string.settings_keep_screen_on),
                        checked = settings.keepScreenOn,
                        onCheckedChange = onKeepScreenOnChange,
                        testTag = "switch_keep_screen_on"
                    )
                }
            }

            // Sound & Vibration Notifications
            item {
                SettingsSectionCard(title = stringResource(R.string.settings_section_alerts)) {
                    SwitchSettingRow(
                        label = stringResource(R.string.settings_sound_enabled),
                        checked = settings.soundEnabled,
                        onCheckedChange = onSoundEnabledChange,
                        testTag = "switch_sound_enabled"
                    )
                    SwitchSettingRow(
                        label = stringResource(R.string.settings_vibration_enabled),
                        checked = settings.vibrationEnabled,
                        onCheckedChange = onVibrationEnabledChange,
                        testTag = "switch_vibration_enabled"
                    )
                    OutlinedButton(
                        onClick = onTestAlert,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("test_alert_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_test_alert))
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.settings_offline_note),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                shape = MaterialTheme.shapes.large
            )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun SliderSettingItem(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    testTag: String,
    steps: Int = 0
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun SwitchSettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ThemeOptionChip(
                label = stringResource(R.string.theme_system),
                icon = Icons.Rounded.BrightnessAuto,
                selected = selected == ThemeMode.SYSTEM,
                onClick = { onSelect(ThemeMode.SYSTEM) },
                testTag = "theme_system",
                modifier = Modifier.weight(1f)
            )
            ThemeOptionChip(
                label = stringResource(R.string.theme_light),
                icon = Icons.Rounded.LightMode,
                selected = selected == ThemeMode.LIGHT,
                onClick = { onSelect(ThemeMode.LIGHT) },
                testTag = "theme_light",
                modifier = Modifier.weight(1f)
            )
            ThemeOptionChip(
                label = stringResource(R.string.theme_dark),
                icon = Icons.Rounded.DarkMode,
                selected = selected == ThemeMode.DARK,
                onClick = { onSelect(ThemeMode.DARK) },
                testTag = "theme_dark",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ThemeOptionChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
