package com.example.reelscounter.ui.Home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reelscounter.service.ReelDetectionService
import com.example.reelscounter.ui.theme.ReelsAccent
import com.example.reelscounter.ui.theme.ShortsAccent

/**
 * Home screen, styled as a personal ledger entry rather than a tech
 * dashboard: today's total as a big serif hero number, then Reels and
 * Shorts as separate line items with hairline dividers.
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

    // Re-check every time the user returns to this screen (e.g. after
    // toggling the service in Settings and pressing back), since Android
    // doesn't notify us when the toggle changes.
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 40.dp)
    ) {
        Text(
            text = "Today",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        AnimatedCount(
            targetValue = totalCount,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "reels & shorts, combined",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(40.dp))

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        LedgerRow(
            label = "Reels",
            value = reelsCount,
            accent = ReelsAccent
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        LedgerRow(
            label = "Shorts",
            value = shortsCount,
            accent = ShortsAccent
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

        Spacer(modifier = Modifier.height(48.dp))

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
 * One line item in the ledger: a small colored dot marking the
 * platform, its label, and its count — right-aligned, like a figure in
 * an account book.
 */
@Composable
private fun LedgerRow(label: String, value: Int, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        AnimatedCount(
            targetValue = value,
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontSize = 32.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/**
 * The one deliberate motion moment in this screen: numbers count up
 * smoothly from their previous value to the new one whenever they
 * change, instead of just snapping — this is what happens when a new
 * reel/short gets detected while the screen is open.
 */
@Composable
private fun AnimatedCount(targetValue: Int, style: TextStyle, color: Color) {
    val animatedValue = remember { Animatable(targetValue.toFloat()) }
    LaunchedEffect(targetValue) {
        animatedValue.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = tween(durationMillis = 600)
        )
    }
    Text(
        text = animatedValue.value.toInt().toString(),
        style = style,
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