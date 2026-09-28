package com.example.reelscounter.ui.Home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reelscounter.domain.model.Platform
import com.example.reelscounter.domain.usecase.GetTodayCountByPlatformUseCase
import com.example.reelscounter.domain.usecase.GetTodayCountUseCase
import com.example.reelscounter.domain.usecase.GetWeeklyCountsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Exposes today's counts (total + per-platform) and the last 7 days'
 * daily totals, all as live-updating StateFlows, so HomeScreen just
 * collects and displays them.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    getTodayCountUseCase: GetTodayCountUseCase,
    getTodayCountByPlatformUseCase: GetTodayCountByPlatformUseCase,
    getWeeklyCountsUseCase: GetWeeklyCountsUseCase
) : ViewModel() {

    val totalCount: StateFlow<Int> = getTodayCountUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val reelsCount: StateFlow<Int> = getTodayCountByPlatformUseCase(Platform.INSTAGRAM)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val shortsCount: StateFlow<Int> = getTodayCountByPlatformUseCase(Platform.YOUTUBE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val weeklyCounts: StateFlow<List<Int>> = getWeeklyCountsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(7) { 0 })
}