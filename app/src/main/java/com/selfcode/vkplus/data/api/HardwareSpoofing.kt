package com.selfcode.vkplus.data.api

import kotlin.random.Random

data class SpoofedDevice(
    val model: String,
    val brand: String,
    val androidVersion: String,
    val sdkVersion: Int,
    val screenWidth: Int,
    val screenHeight: Int,
    val dpi: Int,
    val batteryLevel: Int,
    val userAgent: String
)

object HardwareSpoofing {
    private val devices = listOf(
        Triple("Samsung Galaxy S23", "samsung", listOf("13" to 33, "14" to 34)),
        Triple("Samsung Galaxy S22", "samsung", listOf("13" to 33, "12" to 32)),
        Triple("Samsung Galaxy A54", "samsung", listOf("13" to 33, "14" to 34)),
        Triple("Google Pixel 7", "google", listOf("13" to 33, "14" to 34)),
        Triple("Google Pixel 7a", "google", listOf("13" to 33, "14" to 34)),
        Triple("Google Pixel 6", "google", listOf("12" to 32, "13" to 33)),
        Triple("Xiaomi 13", "xiaomi", listOf("13" to 33)),
        Triple("Xiaomi Redmi Note 12", "xiaomi", listOf("12" to 32, "13" to 33)),
        Triple("OnePlus 11", "oneplus", listOf("13" to 33)),
        Triple("Realme GT 5", "realme", listOf("13" to 33)),
    )

    private val screenConfigs = listOf(
        Triple(1080, 2340, 420),
        Triple(1080, 2400, 440),
        Triple(1440, 3200, 560),
        Triple(1080, 2316, 400),
        Triple(1080, 2408, 420),
        Triple(1440, 3088, 515),
    )

    private val abis = listOf("arm64-v8a", "armeabi-v7a")

    fun generate(): SpoofedDevice {
        val (model, brand, androidVersions) = devices.random()
        val (androidVer, sdk) = androidVersions.random()
        val (width, height, dpi) = screenConfigs.random()
        val battery = Random.nextInt(15, 95)
        val abi = abis.first()
        val modelSlug = model.replace(" ", "_").lowercase()

        val ua = buildString {
            append("VKAndroidApp/8.10-17315 ")
            append("(Android $androidVer; SDK $sdk; $abi; ")
            append("$brand $model; ru; ${width}x${height})")
        }

        return SpoofedDevice(
            model = model,
            brand = brand,
            androidVersion = androidVer,
            sdkVersion = sdk,
            screenWidth = width,
            screenHeight = height,
            dpi = dpi,
            batteryLevel = battery,
            userAgent = ua
        )
    }

    fun generateHeaders(device: SpoofedDevice): Map<String, String> = mapOf(
        "User-Agent" to device.userAgent,
        "X-VK-Android-Client" to "new",
        "X-Screen-Width" to device.screenWidth.toString(),
        "X-Screen-Height" to device.screenHeight.toString(),
        "X-Screen-DPI" to device.dpi.toString(),
        "X-Battery-Level" to device.batteryLevel.toString(),
        "X-Android-SDK" to device.sdkVersion.toString(),
        "X-Device-Model" to device.model,
    )
}
