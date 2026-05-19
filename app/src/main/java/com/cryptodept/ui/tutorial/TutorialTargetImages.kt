package com.cryptodept.ui.tutorial

import androidx.annotation.DrawableRes
import com.cryptodept.R
import com.cryptodept.domain.tutorial.TutorialTargetId

/**
 * Maps each TutorialTargetId to an optional demo image (drawable resource).
 */
object TutorialTargetImages {
    
    @DrawableRes
    fun imageForTarget(targetId: TutorialTargetId): Int? = when (targetId) {
        TutorialTargetId.DASH_PRICE_TICKER -> R.drawable.demo_01_price_ticker
        TutorialTargetId.DASH_AI_NARRATIVE -> R.drawable.demo_02_ai_market_narrative
        TutorialTargetId.DASH_SENTIMENT_GAUGE -> R.drawable.demo_04_sentiment_pulse
        TutorialTargetId.DASH_NETWORK_HEALTH -> R.drawable.demo_05_network_health
        TutorialTargetId.MARKETS_GLOBAL_STATS -> R.drawable.demo_08_global_stats
        TutorialTargetId.MARKETS_LIST -> R.drawable.demo_09_coin_list
        TutorialTargetId.MARKETS_SORT_FILTER -> R.drawable.demo_10_sort_and_filter
        TutorialTargetId.MARKETS_FAVORITES -> R.drawable.demo_11_favorites
        
        // MERGED GROUPS
        TutorialTargetId.ANALYSIS_INDICATORS -> R.drawable.demo_02_ai_market_narrative // Using AI summary as representative for analysis
        TutorialTargetId.TOOLS_POSITION_SIZER -> R.drawable.demo_10_sort_and_filter // Placeholder or relevant tool image
        TutorialTargetId.ALERTS_LIST -> R.drawable.demo_05_network_health // Placeholder for alerts

        TutorialTargetId.SETTINGS_THEME -> R.drawable.demo_30_appearance_and_locale
        else -> null
    }
}
