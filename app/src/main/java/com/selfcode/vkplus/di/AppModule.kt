package com.selfcode.vkplus.di

import android.content.Context
import androidx.work.WorkManager
import com.selfcode.vkplus.auth.VKConfig
import com.selfcode.vkplus.data.api.LongPollService
import com.selfcode.vkplus.data.api.PrivacyInterceptor
import com.selfcode.vkplus.data.api.VKApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(privacyInterceptor: PrivacyInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(privacyInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(VKConfig.BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideVKApi(retrofit: Retrofit): VKApi = retrofit.create(VKApi::class.java)

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideTranslateUseCase(repository: VKRepository): TranslateUseCase =
        TranslateUseCase(repository)
}
