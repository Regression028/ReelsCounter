package com.example.reelscounter.ui.Home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reelscounter.domain.model.Platform
import com.example.reelscounter.domain.usecase.GetTodayCountByPlatformUseCase
import com.example.reelscounter.domain.usecase.GetTodayCountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Exposes today's counts as live-updating StateFlows — one combined
 * total, and one each for Reels (Instagram) and Shorts (YouTube) — so
 * HomeScreen just collects and displays them, with no database or
 * use-case knowledge needed in the Composable itself.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    getTodayCountUseCase: GetTodayCountUseCase,
    getTodayCountByPlatformUseCase: GetTodayCountByPlatformUseCase
) : ViewModel() {

    val totalCount: StateFlow<Int> = getTodayCountUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val reelsCount: StateFlow<Int> = getTodayCountByPlatformUseCase(Platform.INSTAGRAM)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val shortsCount: StateFlow<Int> = getTodayCountByPlatformUseCase(Platform.YOUTUBE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}