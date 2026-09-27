package com.example.reelscounter.ui.Home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reelscounter.domain.usecase.GetTodayCountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Exposes today's reel/short count as a live-updating StateFlow, so
 * HomeScreen just collects it and displays a number — no database or
 * use-case knowledge needed in the Composable itself.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    getTodayCountUseCase: GetTodayCountUseCase
) : ViewModel() {

    val todayCount: StateFlow<Int> = getTodayCountUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
}