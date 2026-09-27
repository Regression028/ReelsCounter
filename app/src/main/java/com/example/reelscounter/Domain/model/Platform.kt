package com.example.reelscounter.domain.model

/**
 * Package names used to identify which platform a detected reel/short
 * came from. Shared between the service (which detects and records
 * events) and the UI (which needs to query counts per platform), so
 * neither has to depend on the other for these string constants.
 */
object Platform {
    const val INSTAGRAM = "com.instagram.android"
    const val YOUTUBE = "com.google.android.youtube"
}