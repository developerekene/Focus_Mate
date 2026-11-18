package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ProductivitySummary
import com.example.model.SessionType
import com.example.model.ThemeMode
import com.example.model.TimerSettings
import com.example.model.TimerState
import com.example.model.TimerStatus
import com.example.ui.theme.TimerDigitsStyle
import com.example.ui.theme.phaseAccentColor
import com.example.ui.theme.phaseContainerColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val PRESET_TAGS = listOf("Deep Work", "Study", "Coding", "Writing", "Planning")

@Composable
fun TimerScreen(
    timerState: TimerState,
    settings: TimerSettings,
    summary: ProductivitySummary,
    onStartOrPause: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onSelectMode: (SessionType) -> Unit,
    onApplyCustomDuration: (Int) -> Unit,
    onUpdateTag: (String) -> Unit,
    onToggleZenMode: () -> Unit,
    onExitZenMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (timerState.distractionFreeMode) {
        BackHandler { onExitZenMode() }
    }

    val isDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val accentColor by animateColorAsState(
        targetValue = phaseAccentColor(timerState.sessionType, isDark),
        animationSpec = tween(300),
        label = "phaseAccent"
    )
    val containerColor by animateColorAsState(
        targetValue = phaseContainerColor(timerState.sessionType, isDark),
        animationSpec = tween(300),
        label = "phaseContainer"
    )

    var showCustomDurationDialog by remember { mutableStateOf(false) }
    var showCustomTagDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = if (timerState.status == TimerStatus.RUNNING) 0.14f else 0.06f),
                        MaterialTheme.colorScheme.background
                    ),
                    radius = 1200f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar: Daily goal pill + Distraction-Free toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = !timerState.distractionFreeMode,
                    enter = fadeIn(tween(220)),
                    exit = fadeOut(tween(180))
                ) {
                    DailyGoalPill(
                        completedToday = summary.todayCompletedFocusSessions,
                        dailyGoal = settings.dailyGoalSessions,
                        todayMinutes = summary.todayFocusMinutes,
                        accentColor = accentColor
                    )
                }

                if (timerState.distractionFreeMode) {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("zen_mode_button")
                ) {
                    IconButton(onClick = onToggleZenMode) {
                        Icon(
                            imageVector = if (timerState.distractionFreeMode) {
                                Icons.Rounded.FullscreenExit
                            } else {
                                Icons.Rounded.Fullscreen
                            },
                            contentDescription = stringResource(
                                if (timerState.distractionFreeMode) {
                                    R.string.timer_zen_exit
                                } else {
                                    R.string.timer_zen_enter
                                }
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mode selector
            AnimatedVisibility(
                visible = !timerState.distractionFreeMode,
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(180))
            ) {
                SessionModeSelector(
                    selectedType = timerState.sessionType,
                    accentColor = accentColor,
                    onSelectMode = onSelectMode
                )
            }

            if (timerState.distractionFreeMode) {
                Text(
                    text = sessionTypeLabel(timerState.sessionType),
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Hero Circular Countdown Dial
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val dialSize = (maxWidth * 0.72f).coerceIn(200.dp, 290.dp)
                CountdownDial(
                    timerState = timerState,
                    accentColor = accentColor,
                    containerColor = containerColor,
                    dialSize = dialSize,
                    onDialClick = {
                        if (timerState.status != TimerStatus.RUNNING) {
                            showCustomDurationDialog = true
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cycle indicator dots
            PomodoroCycleDots(
                completedRounds = timerState.completedRoundsInCycle,
                totalRounds = timerState.sessionsUntilLongBreak,
                accentColor = accentColor
            )

            // Session Tag selector (Focus mode only & not Zen)
            AnimatedVisibility(
                visible = timerState.sessionType == SessionType.FOCUS && !timerState.distractionFreeMode,
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(180))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tags = remember(timerState.sessionTag) {
                            if (PRESET_TAGS.contains(timerState.sessionTag)) {
                                PRESET_TAGS
                            } else {
                                listOf(timerState.sessionTag) + PRESET_TAGS
                            }
                        }
                        tags.forEach { tag ->
                            val selected = tag == timerState.sessionTag
                            FilterChip(
                                selected = selected,
                                onClick = { onUpdateTag(tag) },
                                label = { Text(tag, maxLines = 1) },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Label,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = containerColor,
                                    selectedLabelColor = accentColor,
                                    selectedLeadingIconColor = accentColor
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                    selectedBorderColor = accentColor.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.testTag("tag_chip_$tag")
                            )
                        }
                        FilterChip(
                            selected = false,
                            onClick = { showCustomTagDialog = true },
                            label = { Text("Custom") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("custom_tag_chip")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Controls Row: Reset, Start/Pause, Skip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val canReset = timerState.status != TimerStatus.IDLE ||
                    timerState.remainingSeconds != timerState.totalDurationSeconds ||
                    timerState.isCustomDurationActive

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable(enabled = canReset, onClick = onReset)
                        .testTag("reset_timer_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.timer_reset),
                            tint = if (canReset) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Button(
                    onClick = onStartOrPause,
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 1.dp
                    ),
                    modifier = Modifier
                        .height(64.dp)
                        .widthIn(min = 168.dp)
                        .testTag("start_pause_button")
                ) {
                    Icon(
                        imageVector = if (timerState.status == TimerStatus.RUNNING) {
                            Icons.Rounded.Pause
                        } else {
                            Icons.Rounded.PlayArrow
                        },
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (timerState.status) {
                            TimerStatus.RUNNING -> stringResource(R.string.timer_pause)
                            TimerStatus.PAUSED -> stringResource(R.string.timer_resume)
                            TimerStatus.IDLE -> stringResource(R.string.timer_start)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onSkip)
                        .testTag("skip_timer_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = stringResource(R.string.timer_skip),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCustomDurationDialog) {
        CustomDurationDialog(
            sessionType = timerState.sessionType,
            initialMinutes = (timerState.totalDurationSeconds / 60).coerceAtLeast(1),
            accentColor = accentColor,
            onDismiss = { showCustomDurationDialog = false },
            onApply = { minutes ->
                onApplyCustomDuration(minutes)
                showCustomDurationDialog = false
            }
        )
    }

    if (showCustomTagDialog) {
        CustomTagDialog(
            initialTag = timerState.sessionTag,
            onDismiss = { showCustomTagDialog = false },
            onApply = { newTag ->
                onUpdateTag(newTag)
                showCustomTagDialog = false
            }
        )
    }
}

@Composable
private fun DailyGoalPill(
    completedToday: Int,
    dailyGoal: Int,
    todayMinutes: Int,
    accentColor: Color
) {
    val goalReached = completedToday >= dailyGoal
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("daily_goal_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = if (goalReached) MaterialTheme.colorScheme.secondary else accentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.daily_goal_progress_format, completedToday, dailyGoal),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "· ${todayMinutes}m",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SessionModeSelector(
    selectedType: SessionType,
    accentColor: Color,
    onSelectMode: (SessionType) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SessionType.entries.forEach { type ->
                val isSelected = type == selectedType
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) accentColor else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "modeTabBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(200),
                    label = "modeTabText"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(bgColor)
                        .clickable { onSelectMode(type) }
                        .testTag("mode_${type.storageKey}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sessionTypeLabel(type),
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CountdownDial(
    timerState: TimerState,
    accentColor: Color,
    containerColor: Color,
    dialSize: androidx.compose.ui.unit.Dp,
    onDialClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = timerState.progress,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "dialProgress"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
    val innerSurface = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)

    Box(
        modifier = Modifier
            .size(dialSize)
            .clip(CircleShape)
            .clickable(
                enabled = timerState.status != TimerStatus.RUNNING,
                onClick = onDialClick
            )
            .testTag("timer_dial_button"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val inset = strokeWidth / 2f + 16.dp.toPx()
            val arcDiameter = size.minDimension - inset * 2f
            val topLeft = Offset(
                (size.width - arcDiameter) / 2f,
                (size.height - arcDiameter) / 2f
            )
            val arcSize = Size(arcDiameter, arcDiameter)
            val center = Offset(size.width / 2f, size.height / 2f)

            // Subtle inner disk
            drawCircle(
                color = innerSurface,
                radius = arcDiameter / 2f - strokeWidth * 0.7f,
                center = center
            )

            // Subtle outer ticks (60 markers around the circle)
            val outerTickRadius = arcDiameter / 2f + 12.dp.toPx()
            val innerTickRadius = arcDiameter / 2f + 6.dp.toPx()
            for (i in 0 until 60) {
                val angleRad = (i * 6.0 - 90.0) * (PI / 180.0)
                val isMajor = i % 5 == 0
                val startR = if (isMajor) innerTickRadius - 2.dp.toPx() else innerTickRadius
                val p1 = Offset(
                    center.x + (startR * cos(angleRad)).toFloat(),
                    center.y + (startR * sin(angleRad)).toFloat()
                )
                val p2 = Offset(
                    center.x + (outerTickRadius * cos(angleRad)).toFloat(),
                    center.y + (outerTickRadius * sin(angleRad)).toFloat()
                )
                drawLine(
                    color = if (isMajor) accentColor.copy(alpha = 0.45f) else tickColor,
                    start = p1,
                    end = p2,
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active progress arc
            val sweep = (1f - animatedProgress).coerceIn(0f, 1f) * 360f
            if (sweep > 0.5f) {
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = containerColor
            ) {
                Text(
                    text = when {
                        timerState.status == TimerStatus.PAUSED -> stringResource(R.string.timer_status_paused)
                        timerState.status == TimerStatus.RUNNING && timerState.sessionType == SessionType.FOCUS ->
                            stringResource(R.string.timer_status_focusing)
                        timerState.status == TimerStatus.RUNNING -> stringResource(R.string.timer_status_break)
                        else -> stringResource(R.string.timer_status_ready)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = timerState.formattedTime,
                style = TimerDigitsStyle,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag("timer_countdown_text")
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (timerState.status != TimerStatus.RUNNING) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.timer_customize_duration),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (timerState.isCustomDurationActive) {
                            "Custom · Tap to edit"
                        } else {
                            "Tap to customize"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (timerState.sessionType == SessionType.FOCUS) {
                Text(
                    text = timerState.sessionTag,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PomodoroCycleDots(
    completedRounds: Int,
    totalRounds: Int,
    accentColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val currentRound = (completedRounds % totalRounds) + 1
        repeat(totalRounds) { index ->
            val filled = index < completedRounds
            val active = index == completedRounds
            Box(
                modifier = Modifier
                    .size(if (active) 12.dp else 9.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            filled -> accentColor
                            active -> accentColor.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        }
                    )
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.cycle_progress_format, currentRound, totalRounds),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CustomDurationDialog(
    sessionType: SessionType,
    initialMinutes: Int,
    accentColor: Color,
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit
) {
    val maxMinutes = if (sessionType == SessionType.FOCUS) 120 else 60
    val presets = if (sessionType == SessionType.FOCUS) {
        listOf(15, 25, 45, 60, 90)
    } else {
        listOf(3, 5, 10, 15, 20)
    }
    var sliderValue by remember { mutableFloatStateOf(initialMinutes.coerceIn(1, maxMinutes).toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${stringResource(R.string.custom_duration_title)} (${sessionTypeLabel(sessionType)})",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.minutes_unit, sliderValue.roundToInt()),
                    style = MaterialTheme.typography.displayLarge,
                    color = accentColor
                )

                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 1f..maxMinutes.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor
                    ),
                    modifier = Modifier.testTag("custom_duration_slider")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = sliderValue.roundToInt() == preset,
                            onClick = { sliderValue = preset.toFloat() },
                            label = { Text("${preset}m") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(sliderValue.roundToInt()) },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier.testTag("apply_custom_duration_button")
            ) {
                Text(stringResource(R.string.apply_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}

@Composable
private fun CustomTagDialog(
    initialTag: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialTag) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.session_tag_label)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(28) },
                singleLine = true,
                label = { Text("Focus task or label") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_tag_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { onApply(text) },
                enabled = text.isNotBlank(),
                modifier = Modifier.testTag("save_custom_tag_button")
            ) {
                Text(stringResource(R.string.apply_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}

@Composable
fun sessionTypeLabel(type: SessionType): String = when (type) {
    SessionType.FOCUS -> stringResource(R.string.session_focus)
    SessionType.SHORT_BREAK -> stringResource(R.string.session_short_break)
    SessionType.LONG_BREAK -> stringResource(R.string.session_long_break)
}
