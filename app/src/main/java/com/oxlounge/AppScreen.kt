package com.oxlounge

import androidx.compose.ui.graphics.Color

enum class AppScreen {
    SPIN_WHEEL,
    SEGMENT_EDIT,
    PROMO_STATS
}

data class PrizeSegment(
    val id: String,
    val name: String,
    val hexColor: String,
    val probabilityWeight: Int = 1,
    val isWin: Boolean = true,
    val winCount: Int = 0
) {
    fun getColor(): Color {
        return try {
            Color(android.graphics.Color.parseColor(hexColor))
        } catch (e: Exception) {
            Color.Gray
        }
    }
}

data class WinHistoryItem(
    val prizeName: String,
    val timestamp: Long,
    val customerCode: String = ""
)
