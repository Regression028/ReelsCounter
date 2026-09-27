package com.example.reelscounter.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.reelscounter.domain.usecase.IncrementCounterUseCase
import com.example.reelscounter.ui.Di.ReelDetectionEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Detects "a new reel/short has appeared on screen" inside Instagram and
 * YouTube by watching accessibility events and checking whether an
 * identifying piece of on-screen text (creator username/handle) has
 * changed since the last check.
 *
 * Both platforms' identifiers below were confirmed against a real
 * device via `adb shell uiautomator dump`. Instagram is matched by a
 * stable resource-id; YouTube has no equivalent stable id, so it's
 * matched by a fixed text prefix instead ("Go to channel"), which is
 * more fragile — it depends on YouTube's current English wording and
 * could break if that changes or the device uses another language.
 * Both platforms can change their view structure across app updates,
 * so expect to revisit this periodically.
 */
class ReelDetectionService : AccessibilityService() {

    companion object {
        private const val TAG = "ReelDetectionService"

        const val PACKAGE_INSTAGRAM = "com.instagram.android"
        const val PACKAGE_YOUTUBE = "com.google.android.youtube"

        // Minimum time between two counted events, to avoid one swipe
        // firing multiple accessibility events and over-counting.
        private const val DEBOUNCE_MS = 800L

        // Confirmed via `adb shell uiautomator dump` on real Instagram:
        // this node's content-desc is "Reel by <username>. Double tap to
        // play or pause." and it changes every time a new reel loads.
        // Watching for that text to change is far more reliable than
        // just checking whether a container view exists (which is
        // always true while you're on the Reels tab).
        private const val REEL_MEDIA_COMPONENT_ID_INSTAGRAM =
            "com.instagram.android:id/clips_media_component"

        // YouTube Shorts has no single resource-id with an identifying
        // content-desc like Instagram's clips_media_component. Instead,
        // confirmed via the same uiautomator dump technique: a node with
        // content-desc "Go to channel @<handle>" reliably appears per
        // short and changes with the creator. We search for it by text
        // match instead of by resource-id.
    }

    private var lastCountedAt = 0L
    // AccessibilityService has no built-in coroutine scope like a
    // ViewModel does, so we make our own. SupervisorJob means one
    // failed save doesn't cancel future ones.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var incrementCounterUseCase: IncrementCounterUseCase
    private var lastPackage: String? = null
    private var lastReelDescriptionInstagram: String? = null
    private var lastReelDescriptionYoutube: String? = null

    // Swap this out for a real dependency (e.g. inject a UseCase via
    // EntryPoint/Hilt) once the Di module is wired up. For now this is a
    // simple callback you can point at Logcat, then at Room later.
    var onReelDetected: (platform: String) -> Unit = { platform ->
        Log.d(TAG, "Reel detected on $platform at ${System.currentTimeMillis()}")
        serviceScope.launch {
            incrementCounterUseCase(platform)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Manually pull the use case out of Hilt's graph via the
        // EntryPoint, since Hilt can't @Inject into this class directly.
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ReelDetectionEntryPoint::class.java
        )
        incrementCounterUseCase = entryPoint.incrementCounterUseCase()

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            packageNames = arrayOf(PACKAGE_INSTAGRAM, PACKAGE_YOUTUBE)
            notificationTimeout = 100
        }
        serviceInfo = info
        Log.d(TAG, "Service connected, watching $PACKAGE_INSTAGRAM and $PACKAGE_YOUTUBE")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg != PACKAGE_INSTAGRAM && pkg != PACKAGE_YOUTUBE) return

        // Step 1 (do this first): just log every event so you can see
        // what fires while you manually scroll reels/shorts. Once you
        // know the pattern, tighten the condition below.
        Log.v(TAG, "Event: type=${AccessibilityEvent.eventTypeToString(event.eventType)} pkg=$pkg")

        // Only these event types are worth checking — they fire when
        // Instagram redraws content or the user scrolls, which is when
        // a new reel's content-desc would appear.
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> checkForNewReel(pkg)
        }
    }

    /**
     * Reads an identifying piece of text for the currently visible
     * reel/short and compares it against the last one we saw. A change
     * means a genuinely new reel/short loaded — far more reliable than
     * checking whether a container view exists, since that container is
     * present the whole time you're on the tab.
     */
    private fun checkForNewReel(pkg: String) {
        val root = rootInActiveWindow ?: return

        when (pkg) {
            PACKAGE_INSTAGRAM -> {
                val nodes: List<AccessibilityNodeInfo> = try {
                    root.findAccessibilityNodeInfosByViewId(REEL_MEDIA_COMPONENT_ID_INSTAGRAM)
                } catch (e: Exception) {
                    emptyList()
                }
                val current = nodes.firstOrNull()?.contentDescription?.toString() ?: return
                handlePossibleChange(
                    pkg = pkg,
                    current = current,
                    last = lastReelDescriptionInstagram,
                    update = { lastReelDescriptionInstagram = it }
                )
            }
            PACKAGE_YOUTUBE -> {
                // No stable resource-id available here, so we search by
                // the fixed text prefix "Go to channel" — this matches
                // against node content-desc, e.g. "Go to channel @someone".
                val nodes: List<AccessibilityNodeInfo> = try {
                    root.findAccessibilityNodeInfosByText("Go to channel")
                } catch (e: Exception) {
                    emptyList()
                }
                val current = nodes.firstOrNull()?.contentDescription?.toString() ?: return
                handlePossibleChange(
                    pkg = pkg,
                    current = current,
                    last = lastReelDescriptionYoutube,
                    update = { lastReelDescriptionYoutube = it }
                )
            }
        }
    }

    private fun handlePossibleChange(
        pkg: String,
        current: String,
        last: String?,
        update: (String) -> Unit
    ) {
        if (current == last) return
        // Ignore the very first read after (re)opening the app — that's
        // just "we found the current reel", not a swipe.
        val isFirstRead = last == null
        update(current)
        if (!isFirstRead) {
            maybeCount(pkg)
        }
    }

    private fun maybeCount(pkg: String) {
        val now = System.currentTimeMillis()
        if (pkg == lastPackage && now - lastCountedAt < DEBOUNCE_MS) return
        lastCountedAt = now
        lastPackage = pkg
        onReelDetected(pkg)
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
    }
}