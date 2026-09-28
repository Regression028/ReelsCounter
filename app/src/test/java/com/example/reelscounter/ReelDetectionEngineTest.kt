package com.example.reelscounter.service.detection

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReelDetectionEngineTest {

    private val ig = "instagram"
    private val yt = "youtube"
    private lateinit var engine: ReelDetectionEngine
    private var now = 0L

    @Before
    fun setUp() {
        engine = ReelDetectionEngine(settleMs = 200, absenceRearmMs = 1_500)
        now = 1_000
    }

    /** Shows [id] and lets it settle, returning the final result. */
    private fun show(platform: String, id: String): DetectionResult {
        engine.onReading(platform, Reading.Item(id), now)
        now += 250
        return engine.onReading(platform, Reading.Item(id), now)
    }

    @Test
    fun firstItemOnEnteringFeedIsNotCounted() {
        assertEquals(DetectionResult.NONE, show(ig, "A"))
    }

    @Test
    fun nextItemsAreCountedOnceEach() {
        show(ig, "A")
        assertEquals(DetectionResult.COUNT, show(ig, "B"))
        assertEquals(DetectionResult.COUNT, show(ig, "C"))
    }

    @Test
    fun repeatedEventsForSameItemNeverDoubleCount() {
        show(ig, "A")
        assertEquals(DetectionResult.COUNT, show(ig, "B"))
        repeat(20) {
            now += 50
            assertEquals(DetectionResult.NONE, engine.onReading(ig, Reading.Item("B"), now))
        }
    }

    @Test
    fun newItemIsPendingUntilItHasSettled() {
        show(ig, "A")
        assertEquals(DetectionResult.PENDING, engine.onReading(ig, Reading.Item("B"), now))
        now += 100
        assertEquals(DetectionResult.PENDING, engine.onReading(ig, Reading.Item("B"), now))
        now += 150
        assertEquals(DetectionResult.COUNT, engine.onReading(ig, Reading.Item("B"), now))
    }

    @Test
    fun dragThatSnapsBackDoesNotCount() {
        show(ig, "A")
        engine.onReading(ig, Reading.Item("B"), now)
        now += 80
        assertEquals(DetectionResult.NONE, engine.onReading(ig, Reading.Item("A"), now))
        now += 500
        assertEquals(DetectionResult.NONE, engine.onReading(ig, Reading.Item("A"), now))
    }

    @Test
    fun rapidSwipesThatEachSettleAreAllCounted() {
        show(ig, "A")
        var counted = 0
        for (id in listOf("B", "C", "D", "E")) {
            if (show(ig, id) == DetectionResult.COUNT) counted++
        }
        assertEquals(4, counted)
    }

    @Test
    fun itemFlashedForLessThanSettleTimeIsSkipped() {
        show(ig, "A")
        engine.onReading(ig, Reading.Item("B"), now)
        now += 60
        engine.onReading(ig, Reading.Item("C"), now)
        now += 250
        // Only C stayed; B never did. One count, not two.
        assertEquals(DetectionResult.COUNT, engine.onReading(ig, Reading.Item("C"), now))
    }

    @Test
    fun unreadableNeverChangesStateOrCounts() {
        show(ig, "A")
        assertEquals(DetectionResult.NONE, engine.onReading(ig, Reading.Unreadable, now + 10))
        assertEquals(DetectionResult.NONE, engine.onReading(ig, Reading.Item("A"), now + 20))
    }

    @Test
    fun commentsPanelOpeningAndClosingOnSameItemDoesNotCount() {
        show(ig, "A")
        engine.onWindowStateChanged(ig, Reading.Absent)   // panel/dialog window appears
        now += 3_000
        engine.onReading(ig, Reading.Absent, now)
        now += 200
        assertEquals(DetectionResult.NONE, engine.onReading(ig, Reading.Item("A"), now))
        // ...and the next real swipe still counts (baseline was not left armed).
        assertEquals(DetectionResult.COUNT, show(ig, "B"))
    }

    @Test
    fun reenteringFeedWithDifferentItemIsBaselineNotCount() {
        show(ig, "A")
        assertEquals(DetectionResult.COUNT, show(ig, "B"))
        engine.onWindowStateChanged(ig, Reading.Item("Z"))   // app resumed showing a different item
        assertEquals(DetectionResult.NONE, show(ig, "Z"))
        assertEquals(DetectionResult.COUNT, show(ig, "Y"))
    }

    @Test
    fun leavingFeedForAWhileRearmsBaseline() {
        show(ig, "A")
        engine.onReading(ig, Reading.Absent, now)
        now += 2_000
        engine.onReading(ig, Reading.Absent, now)
        assertEquals(DetectionResult.NONE, show(ig, "Q"))
    }

    @Test
    fun switchingBetweenAppsKeepsPlatformsIndependent() {
        show(ig, "A")
        assertEquals(DetectionResult.COUNT, show(ig, "B"))

        engine.onWindowStateChanged(yt, Reading.Item("@x"))
        assertEquals(DetectionResult.NONE, show(yt, "@x"))     // YouTube baseline
        assertEquals(DetectionResult.COUNT, show(yt, "@y"))

        engine.onWindowStateChanged(ig, Reading.Item("B"))       // back to the same reel
        assertEquals(DetectionResult.NONE, engine.onReading(ig, Reading.Item("B"), now))
        assertEquals(DetectionResult.COUNT, show(ig, "C"))
    }

    @Test
    fun unsettledItemDroppedWhenUserLeaves() {
        show(ig, "A")
        engine.onReading(ig, Reading.Item("B"), now)
        engine.cancelPending(ig)
        now += 500
        // B is treated as brand new again, not as already-settled.
        assertEquals(DetectionResult.PENDING, engine.onReading(ig, Reading.Item("B"), now))
    }

    @Test
    fun resetMakesNextItemABaseline() {
        show(ig, "A")
        assertEquals(DetectionResult.COUNT, show(ig, "B"))
        engine.reset()   // service restarted
        assertEquals(DetectionResult.NONE, show(ig, "C"))
    }
}
