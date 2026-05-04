package com.cryptodept.util

import android.app.Activity
import android.content.Context
import com.cryptodept.data.datastore.PreferencesManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val journalRepository: com.cryptodept.domain.repository.JournalRepository
) {
    private val reviewManager = ReviewManagerFactory.create(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    fun requestReviewIfAppropriate(activity: Activity) {
        scope.launch {
            val launches = preferencesManager.getLaunchCount()
            val lastReview = preferencesManager.getLastReviewPromptTime()
            val daysSinceLastReview = (System.currentTimeMillis() - lastReview) / 86_400_000
            
            // Profitability check
            val statsResult = journalRepository.getStats()
            val isProfitable = (statsResult.getOrNull()?.averagePnL ?: 0.0) > 0
            
            // Show after 7+ launches AND 30+ days since last prompt AND profitable portfolio
            if (launches >= 7 && (lastReview == 0L || daysSinceLastReview >= 30) && isProfitable) {
                reviewManager.requestReviewFlow().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val reviewInfo = task.result
                        reviewManager.launchReviewFlow(activity, reviewInfo)
                        scope.launch {
                            preferencesManager.saveLastReviewPromptTime(System.currentTimeMillis())
                        }
                    }
                }
            }
        }
    }
}
