package com.example.reelscounter.service.detection

/**
 * What the screen reader saw on the most recent look at the target app.
 * Kept separate from Android types so the engine below is plain Kotlin and
 * can be unit-tested on the JVM without a device.
 */
sealed interface Reading {
    /** The Reels/Shorts feed is not on screen (Home tab, another screen, etc.). */
    data object Absent : Reading

    /**
     * The feed IS on screen but we cannot tell which item is showing right now
     * (mid-swipe with two items visible, an ad, a comments panel covering the
     * identifying node, an unexpected UI change...). Must never change state.
     */
    data object Unreadable : Reading

    /** The feed is on screen and [id] identifies the item currently showing. */
    data class Item(val id: String) : Reading
}

enum class DetectionResult {
    /** A new item was confirmed: record one count. */
    COUNT,

    /** A new item is showing but has not been stable long enough yet: look again shortly. */
    PENDING,

    /** Nothing to do. */
    NONE
}

/**
 * Decides when a "new Reel/Short" has really been swiped to. One independent
 * state per platform, so Instagram -> YouTube -> Instagram never mixes state.
 *
 * Rules:
 *  1. An item only counts after it has been the visible item for [settleMs]
 *     (a half-finished drag that snaps back, or a transient flicker, never counts).
 *  2. The same identifier twice in a row never counts twice.
 *  3. The first stable item after entering the feed is a "baseline" and is NOT counted.
 *     The baseline is re-armed when the app window changes to a different item, or when
 *     the feed has been off screen for [absenceRearmMs].
 *  4. [Reading.Unreadable] never touches state, so overlays/comments/ads can't cause counts.
 *
 * Not thread-safe by design: call it from a single thread (the service's main thread).
 */
class ReelDetectionEngine(
    private val settleMs: Long = DEFAULT_SETTLE_MS,
    private val absenceRearmMs: Long = DEFAULT_ABSENCE_REARM_MS
) {
    companion object {
        /** How long a new item must stay visible before it is counted. Lower = counts faster swipes. */
        const val DEFAULT_SETTLE_MS = 200L

        /** How long the feed must be off screen before the next item counts as "entering the feed" again. */
        const val DEFAULT_ABSENCE_REARM_MS = 1_500L
    }

    private class State {
        var committed: String? = null
        var baselinePending = true
        var candidate: String? = null
        var candidateSince = 0L
        var absentSince = -1L
    }

    private val states = HashMap<String, State>()

    private fun stateOf(platform: String) = states.getOrPut(platform) { State() }

    /** How long callers should wait before looking again after [DetectionResult.PENDING]. */
    val recheckDelayMs: Long get() = settleMs + 30L

    /**
     * Call when the target app's window state changed (app brought to the front,
     * a new screen or dialog appeared). [current] is what is on screen right now.
     *
     * If the same item is still showing, nothing changed for us (e.g. a dialog opened
     * and closed) so the baseline is left alone. Otherwise the next stable item is
     * treated as "entering the feed" rather than as a swipe.
     */
    fun onWindowStateChanged(platform: String, current: Reading) {
        val s = stateOf(platform)
        if (current is Reading.Item && current.id == s.committed) return
        s.baselinePending = true
        s.candidate = null
    }

    fun onReading(platform: String, reading: Reading, nowMs: Long): DetectionResult {
        val s = stateOf(platform)
        when (reading) {
            Reading.Unreadable -> return DetectionResult.NONE

            Reading.Absent -> {
                s.candidate = null
                if (s.absentSince < 0) {
                    s.absentSince = nowMs
                } else if (nowMs - s.absentSince >= absenceRearmMs) {
                    s.baselinePending = true
                }
                return DetectionResult.NONE
            }

            is Reading.Item -> {
                s.absentSince = -1L
                val id = reading.id

                // Still (or again) the item we already accepted: nothing new.
                if (id == s.committed) {
                    s.candidate = null
                    s.baselinePending = false
                    return DetectionResult.NONE
                }

                // A different item: start (or continue) timing how long it stays.
                if (id != s.candidate) {
                    s.candidate = id
                    s.candidateSince = nowMs
                    return DetectionResult.PENDING
                }
                if (nowMs - s.candidateSince < settleMs) return DetectionResult.PENDING

                // Stable long enough: accept it.
                s.committed = id
                s.candidate = null
                if (s.baselinePending) {
                    s.baselinePending = false
                    return DetectionResult.NONE
                }
                return DetectionResult.COUNT
            }
        }
    }

    /** Drop any not-yet-confirmed item (e.g. the user left the app before it settled). */
    fun cancelPending(platform: String) {
        states[platform]?.candidate = null
    }

    /** Forget everything, e.g. when the service (re)connects. Next item per platform is a baseline. */
    fun reset() {
        states.clear()
    }
}
