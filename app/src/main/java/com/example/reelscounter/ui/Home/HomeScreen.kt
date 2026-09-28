package com.example.reelscounter.ui.Home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reelscounter.service.ReelDetectionService
import com.example.reelscounter.ui.theme.PaperLight
import com.example.reelscounter.ui.theme.ReelsAccent
import com.example.reelscounter.ui.theme.ShortsAccent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Home screen: a donut ring showing today's Reels/Shorts split, two
 * bold platform blocks with percentages, and a 7-day trend chart —
 * built to feel like a real habit-tracking app, not a debug readout.
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var isEnabled by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context, ReelDetectionService::class.java))
    }

    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val reelsCount by viewModel.reelsCount.collectAsStateWithLifecycle()
    val shortsCount by viewModel.shortsCount.collectAsStateWithLifecycle()
    val weeklyCounts by viewModel.weeklyCounts.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isEnabled = isAccessibilityServiceEnabled(context, ReelDetectionService::class.java)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val todayLabel = remember {
        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Text(
            text = "Reels Counter",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = todayLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(32.dp))

        ProgressRing(
            reelsCount = reelsCount,
            shortsCount = shortsCount,
            total = totalCount,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PlatformBlock(
                label = "Reels",
                count = reelsCount,
                total = totalCount,
                background = ReelsAccent,
                modifier = Modifier.weight(1f)
            )
            PlatformBlock(
                label = "Shorts",
                count = shortsCount,
                total = totalCount,
                background = ShortsAccent,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "This week",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(20.dp))

        WeeklyChart(counts = weeklyCounts)

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = if (isEnabled) "Detection is active" else "Detection is off",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (!isEnabled) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Text("Enable detection")
            }
        }
    }
}

/**
 * A donut ring split proportionally between Reels and Shorts — the
 * split itself is the information, not decoration. Today's total sits
 * animated in the center.
 */
@Composable
private fun ProgressRing(
    reelsCount: Int,
    shortsCount: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val combinedTotal = (reelsCount + shortsCount).coerceAtLeast(1)
    val reelsFraction = if (reelsCount + shortsCount > 0) reelsCount / combinedTotal.toFloat() else 0f
    val shortsFraction = if (reelsCount + shortsCount > 0) shortsCount / combinedTotal.toFloat() else 0f

    val reelsSweep by animateFloatAsState(
        targetValue = reelsFraction * 360f,
        animationSpec = tween(700),
        label = "reelsSweep"
    )
    val shortsSweep by animateFloatAsState(
        targetValue = shortsFraction * 360f,
        animationSpec = tween(700),
        label = "shortsSweep"
    )

    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val strokeWidth = 18.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val arcSize = Size(diameter, diameter)

            // Background track
            drawArc(
                color = Color(0x22000000),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            if (reelsSweep > 0f) {
                drawArc(
                    color = ReelsAccent,
                    startAngle = -90f,
                    sweepAngle = reelsSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            if (shortsSweep > 0f) {
                drawArc(
                    color = ShortsAccent,
                    startAngle = -90f + reelsSweep,
                    sweepAngle = shortsSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedCount(
                targetValue = total,
                fontSize = 48.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "today",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * A bold flat-color block for one platform — deliberately not a soft
 * rounded card with a shadow. High-contrast text directly on the
 * accent color, with the percentage of today's total as a second
 * data point (not just decoration).
 */
@Composable
private fun PlatformBlock(
    label: String,
    count: Int,
    total: Int,
    background: Color,
    modifier: Modifier = Modifier
) {
    val percentage = if (total > 0) (count * 100 / total) else 0

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = PaperLight
        )
        Spacer(modifier = Modifier.height(4.dp))
        AnimatedCount(
            targetValue = count,
            fontSize = 32.sp,
            color = PaperLight
        )
        Text(
            text = "$percentage% of today",
            style = MaterialTheme.typography.labelMedium,
            color = PaperLight.copy(alpha = 0.8f)
        )
    }
}

/**
 * Seven bars, oldest to newest (today last). Today's bar is
 * highlighted in solid ink; the rest are a quiet muted tone — the
 * point is to show the week's shape, not to compete with the ring.
 */
@Composable
private fun WeeklyChart(counts: List<Int>) {
    val maxCount = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    val dayLetters = remember {
        (6 downTo 0).map { daysAgo ->
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, -daysAgo)
            SimpleDateFormat("EEEEE", Locale.getDefault()).format(calendar.time)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        counts.forEachIndexed { index, count ->
            val isToday = index == counts.lastIndex
            val heightFraction = (count / maxCount.toFloat()).coerceIn(0f, 1f)
            val animatedHeight by animateFloatAsState(
                targetValue = (heightFraction * 72).coerceAtLeast(4f),
                animationSpec = tween(600),
                label = "barHeight"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(animatedHeight.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isToday) {
                                MaterialTheme.colorScheme.onBackground
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            }
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = dayLetters.getOrElse(index) { "" },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isToday) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Animates from the previous value to the new one whenever it changes. */
@Composable
private fun AnimatedCount(targetValue: Int, fontSize: androidx.compose.ui.unit.TextUnit, color: Color) {
    val animatedValue = remember { Animatable(targetValue.toFloat()) }
    LaunchedEffect(targetValue) {
        animatedValue.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = tween(durationMillis = 600)
        )
    }
    Text(
        text = animatedValue.value.toInt().toString(),
        style = androidx.compose.ui.text.TextStyle(
            fontFamily = FontFamily.Serif,
            fontSize = fontSize
        ),
        color = color
    )
}

/**
 * Checks Android's system setting for the list of currently-enabled
 * accessibility services and looks for ours in it.
 */
fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
    val expectedComponentName = ComponentName(context, serviceClass)
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)
    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val enabledComponentName = ComponentName.unflattenFromString(componentNameString)
        if (enabledComponentName != null && enabledComponentName == expectedComponentName) {
            return true
        }
    }
    return false
}