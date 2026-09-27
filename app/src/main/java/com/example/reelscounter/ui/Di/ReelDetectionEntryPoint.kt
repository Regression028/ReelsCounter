package com.example.reelscounter.ui.Di

import com.example.reelscounter.domain.usecase.IncrementCounterUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


/**
 * AccessibilityService is not one of the Android component types Hilt
 * can inject into automatically (unlike Activity, Fragment, ViewModel).
 * This EntryPoint is the officially-supported workaround: it lets any
 * plain class manually ask Hilt's graph for a specific dependency by
 * calling EntryPoints.get(...), instead of using @Inject fields.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReelDetectionEntryPoint {
    fun incrementCounterUseCase(): IncrementCounterUseCase
}