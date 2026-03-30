package com.selfcode.vkplus.data.api

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.selfcode.vkplus.data.local.SettingsStore
import com.selfcode.vkplus.data.local.TokenStorage
import com.selfcode.vkplus.data.repository.VKRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class OfflineWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsStore: SettingsStore,
    private val repository: VKRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val forceOffline = settingsStore.forceOffline.first()
        if (forceOffline) {
            repository.setOffline()
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "vkplus_offline_worker"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<OfflineWorker>(5, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
