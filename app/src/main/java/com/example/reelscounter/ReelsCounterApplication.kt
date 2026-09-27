package com.example.reelscounter

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt needs a custom Application class to bootstrap its dependency
 * graph when the app process starts. This class does nothing itself —
 * the @HiltAndroidApp annotation is what matters; it triggers Hilt's
 * code generation for the whole app.
 */
@HiltAndroidApp
class ReelsCounterApplication : Application()