package com.example.model

/**
 * 16-bit pixel cloud entity drifting across planetary tiles.
 */
data class CloudParticle(
    val id: Int,
    val xRatio: Float, // 0.0 to 1.0 (wraps horizontally)
    val yRatio: Float, // 0.0 to 1.0
    val widthTiles: Int, // 3 to 9 tiles
    val heightTiles: Int, // 2 to 5 tiles
    val speedX: Float,
    val speedY: Float,
    val density: Float // 0.4 to 0.95
)

/**
 * Localized precipitation storm cell.
 */
data class RainZone(
    val centerTileX: Int,
    val centerTileY: Int,
    val radiusTiles: Int,
    val precipitationRateMmHr: Float
)

/**
 * Complete atmospheric weather and orbital illumination state.
 */
data class WeatherState(
    val clouds: List<CloudParticle> = emptyList(),
    val dayNightCycleProgress: Float = 0.25f, // 0.0 to 1.0 (moves terminator line)
    val dayNightSpeed: Float = 0.0005f,
    val globalHumidityPercent: Float = 62.0f,
    val rainZones: List<RainZone> = emptyList(),
    val windDirectionDegrees: Float = 75f,
    val cyclonicVortexStrength: Float = 0.0f // Elevated on tidally locked worlds
)
