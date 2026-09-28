package com.example.reelscounter.service

import android.accessibilityservice.AccessibilityService
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.reelscounter.domain.model.Platform
import com.example.reelscounter.domain.usecase.IncrementCounterUseCase
import com.example.reelscounter.service.detection.DetectionResult
import com.example.reelscounter.service.detection.Reading
import com.example.reelscounter.service.detection.ReelDetectionEngine
import com.example.reelscounter.service.detection.ScreenReader
import com.example.reelscounter.service.detection.recycleCompat
import com.example.reelscounter.ui.Di.ReelDetectionEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Thin Android glue. It only:
 *  1. receives accessibility events from Instagram / YouTube,
 *  2. asks [ScreenReader] what is on screen,
 *  3. asks [ReelDetectionEngine] whether that means "a new Reel/Short",
 *  4. saves one row through [IncrementCounterUseCase] off the main thread.
 *
 * All decision logic lives in the engine; all third-party view ids live in ScreenReader.
 * Event types, package filter and flags are declared once in
 * res/xml/accessibility_service_config.xml.
 */
class ReelDetectionService : AccessibilityService() {

    private val engine = ReelDetectionEngine()
    private val mainHandler = Handler(Looper.getMainLooper())

    // Only used for database writes. Cancelled in onDestroy().
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var incrementCounterUseCase: IncrementCounterUseCase? = null

    // One pending "look again once the new item has settled" per platform.
    private val recheckRunnables = HashMap<String, Runnable>()

    private val isDebuggable: Boolean
        get() = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        engine.reset()
        clearRechecks()
        incrementCounterUseCase = try {
            EntryPointAccessors.fromApplication(
                applicationContext,
                ReelDetectionEntryPoint::class.java
            ).incrementCounterUseCase()
        } catch (e: Exception) {
            Log.e(TAG, "Could not get IncrementCounterUseCase; detection is disabled", e)
            null
        }
        debugLog("Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null || incrementCounterUseCase == null) return
            val platform = platformFor(event.packageName?.toString()) ?: return

            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    val reading = readScreen(platform)
                    if (reading != null) engine.onWindowStateChanged(platform, reading)
                    evaluate(platform)
                }

                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                AccessibilityEvent.TYPE_VIEW_SCROLLED -> evaluate(platform)
            }
        } catch (e: Exception) {
            // Third-party UI can do surprising things. Never let that crash the service.
            debugLog("Event handling failed: ${e.javaClass.simpleName}")
        }
    }

    /** Looks at the screen once and lets the engine decide. */
    private fun evaluate(platform: String) {
        val reading = readScreen(platform)
        if (reading == null) {
            // Another app/window is in front (or no window): the user left. Forget any unsettled item.
            engine.cancelPending(platform)
            cancelRecheck(platform)
            return
        }

        when (engine.onReading(platform, reading, SystemClock.elapsedRealtime())) {
            DetectionResult.COUNT -> {
                cancelRecheck(platform)
                recordCount(platform)
            }
            DetectionResult.PENDING -> scheduleRecheck(platform)
            // Leave any already-scheduled recheck alone: if we saw a new item, then a moment
            // of "unreadable" mid-transition, the recheck is what confirms it once it settles.
            DetectionResult.NONE -> Unit
        }
    }

    /**
     * Reads the active window, or returns null if it is missing or belongs to a
     * different app than [platform]. Always recycles the root node.
     */
    private fun readScreen(platform: String): Reading? {
        val root = rootInActiveWindow ?: return null
        try {
            if (root.packageName?.toString() != platform) return null
            return ScreenReader.read(root, platform)
        } finally {
            root.recycleCompat()
        }
    }

    private fun scheduleRecheck(platform: String) {
        if (recheckRunnables.containsKey(platform)) return
        val runnable = Runnable {
            recheckRunnables.remove(platform)
            try {
                evaluate(platform)
            } catch (e: Exception) {
                debugLog("Recheck failed: ${e.javaClass.simpleName}")
            }
        }
        recheckRunnables[platform] = runnable
        mainHandler.postDelayed(runnable, engine.recheckDelayMs)
    }

    private fun cancelRecheck(platform: String) {
        recheckRunnables.remove(platform)?.let { mainHandler.removeCallbacks(it) }
    }

    private fun clearRechecks() {
        mainHandler.removeCallbacksAndMessages(null)
        recheckRunnables.clear()
    }

    private fun recordCount(platform: String) {
        val useCase = incrementCounterUseCase ?: return
        debugLog("Counted one item on ${if (platform == Platform.INSTAGRAM) "instagram" else "youtube"}")
        serviceScope.launch {
            try {
                // A count we already decided on should still be saved if the service is stopping.
                withContext(NonCancellable) { useCase(platform) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save count", e)
            }
        }
    }

    private fun platformFor(pkg: String?): String? = when (pkg) {
        Platform.INSTAGRAM -> Platform.INSTAGRAM
        Platform.YOUTUBE -> Platform.YOUTUBE
        else -> null
    }

    /** Debug builds only. Never pass usernames, descriptions or other screen content here. */
    private fun debugLog(message: String) {
        if (isDebuggable) Log.d(TAG, message)
    }

    override fun onInterrupt() {
        clearRechecks()
        engine.reset()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        clearRechecks()
        engine.reset()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        clearRechecks()
        engine.reset()
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "ReelDetectionService"
    }
}
