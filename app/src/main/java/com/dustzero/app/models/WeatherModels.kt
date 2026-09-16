package com.dustzero.app.models

import kotlinx.serialization.Serializable

@Serializable
data class WeatherCondition(
    val temp: Double,
    val description: String,
    val icon: String
)

@Serializable
data class ForecastItem(
    val dt: Long,
    val timestamp: String,
    val temp: Double,
    val pop: Int,
    val conditions: String
)

@Serializable
data class WeatherResponse(
    val current: WeatherCondition,
    val next_12_hours: List<ForecastItem>
)

// App-level wrapper that includes the locally generated insight
data class WeatherData(
    val current: WeatherCondition,
    val next12Hours: List<ForecastItem>,
    val cleaningInsight: String
)
