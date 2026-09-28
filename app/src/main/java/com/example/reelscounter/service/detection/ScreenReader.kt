package com.example.reelscounter.service.detection

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.example.reelscounter.domain.model.Platform

/**
 * Turns the live accessibility tree of Instagram / YouTube into a [Reading].
 * This is the ONLY place that knows third-party view ids and text, so if either app
 * changes its UI this is the one file to update (see the uiautomator dump steps in
 * the project notes).
 *
 * Every lookup is wrapped so a changed or odd UI degrades to [Reading.Unreadable] or
 * [Reading.Absent] (no count) instead of throwing.
 *
 * Nothing read here is ever logged or stored: identifiers stay in memory only,
 * long enough to compare "same item as before?".
 */
internal object ScreenReader {

    // ---- Instagram (ids confirmed in window_dump.xml) --------------------------------
    private const val IG_REEL_ITEM_ID = "com.instagram.android:id/clips_media_component"

    // ---- YouTube Shorts (ids confirmed in window_dump_yt.xml) ------------------------
    // Presence of any of these means the Shorts player (not a normal video page) is showing.
    private val YT_SHORTS_CONTEXT_IDS = listOf(
        "com.google.android.youtube:id/reel_recycler",
        "com.google.android.youtube:id/reel_player_page_container"
    )

    // The "Go to channel" node sits inside one of these Shorts-only containers. Requiring
    // it stops comment avatars, playlists or normal video pages from being mistaken for a Short.
    private val YT_SHORTS_ANCESTOR_IDS = setOf(
        "com.google.android.youtube:id/reel_player_footer_container",
        "com.google.android.youtube:id/reel_player_overlay_container",
        "com.google.android.youtube:id/reel_player_overlay_root",
        "com.google.android.youtube:id/reel_player_page_container"
    )

    // English wording; see limitations. Matched with startsWith on the content-description.
    private const val YT_CHANNEL_PREFIX = "Go to channel"
    private const val MAX_ANCESTOR_DEPTH = 16

    /** Never throws. */
    fun read(root: AccessibilityNodeInfo, platform: String): Reading = try {
        when (platform) {
            Platform.INSTAGRAM -> readInstagram(root)
            Platform.YOUTUBE -> readYoutube(root)
            else -> Reading.Unreadable
        }
    } catch (e: Exception) {
        Reading.Unreadable
    }

    private fun readInstagram(root: AccessibilityNodeInfo): Reading {
        val nodes = root.findAccessibilityNodeInfosByViewId(IG_REEL_ITEM_ID).orEmpty()
        try {
            if (nodes.isEmpty()) return Reading.Absent
            // Instagram keeps neighbouring pages in the tree; only the on-screen one matters.
            // Mid-swipe two can be visible: take the one covering more of the screen.
            val current = nodes
                .filter { it.isVisibleToUser }
                .maxByOrNull { area(it) }
                ?: return Reading.Unreadable
            val desc = current.contentDescription?.toString()?.trim()
            return if (desc.isNullOrEmpty()) Reading.Unreadable else Reading.Item(desc)
        } finally {
            nodes.forEach { it.recycleCompat() }
        }
    }

    private fun readYoutube(root: AccessibilityNodeInfo): Reading {
        // 1) Is the Shorts player on screen at all?
        var inShorts = false
        for (id in YT_SHORTS_CONTEXT_IDS) {
            val found = root.findAccessibilityNodeInfosByViewId(id).orEmpty()
            val hit = found.isNotEmpty()
            found.forEach { it.recycleCompat() }
            if (hit) {
                inShorts = true
                break
            }
        }
        if (!inShorts) return Reading.Absent

        // 2) Which Short? The channel button inside the Shorts overlay.
        val nodes = root.findAccessibilityNodeInfosByText(YT_CHANNEL_PREFIX).orEmpty()
        try {
            val candidates = nodes.filter { node ->
                node.isVisibleToUser &&
                        node.contentDescription?.toString()
                            ?.startsWith(YT_CHANNEL_PREFIX, ignoreCase = true) == true &&
                        hasShortsAncestor(node)
            }
            // Zero: ad / panel covering it / UI changed. Two+: mid-swipe. Either way: don't guess.
            if (candidates.size != 1) return Reading.Unreadable
            val desc = candidates[0].contentDescription?.toString()?.trim()
            return if (desc.isNullOrEmpty()) Reading.Unreadable else Reading.Item(desc)
        } finally {
            nodes.forEach { it.recycleCompat() }
        }
    }

    private fun hasShortsAncestor(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node.parent
        var depth = 0
        try {
            while (current != null && depth < MAX_ANCESTOR_DEPTH) {
                if (current.viewIdResourceName in YT_SHORTS_ANCESTOR_IDS) return true
                val next = current.parent
                current.recycleCompat()
                current = next
                depth++
            }
            return false
        } finally {
            current?.recycleCompat()
        }
    }

    private fun area(node: AccessibilityNodeInfo): Int {
        val r = Rect()
        node.getBoundsInScreen(r)
        return maxOf(0, r.width()) * maxOf(0, r.height())
    }
}

/**
 * Nodes returned by find getParent must be recycled on API < 33 or the framework's
* node pool can run dry and detection silently stops after a while. From API 33 recycle()
* is a deprecated no-op. Safe to call on any version.
*/
@Suppress("DEPRECATION")
internal fun AccessibilityNodeInfo.recycleCompat() {
    try {
        recycle()
    } catch (e: Exception) {
        // Already recycled or framework quirk: nothing useful to do.
    }
}
