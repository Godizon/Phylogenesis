package com.example.engine

import com.example.model.CloudParticle
import com.example.model.PlanetaryState
import com.example.model.RainZone
import com.example.model.WeatherState
import kotlin.math.cos
import kotlin.math.sin

object WeatherEngine {

    fun createInitialWeather(planetary: PlanetaryState): WeatherState {
        val clouds = mutableListOf<CloudParticle>()
        val cloudCount = if (planetary.isSuperEarth) 18 else 12

        for (i in 0 until cloudCount) {
            val yRatio = (i.toFloat() / cloudCount.toFloat()) * 0.8f + 0.1f
            // Trade winds: East-to-west near equator, West-to-east at mid latitudes
            val speedX = if (yRatio in 0.35f..0.65f) -0.015f else 0.020f
            val speedY = (sin(i.toDouble() * 1.5).toFloat()) * 0.003f

            clouds.add(
                CloudParticle(
                    id = i,
                    xRatio = (i * 0.17f) % 1.0f,
                    yRatio = yRatio,
                    widthTiles = 3 + (i % 5),
                    heightTiles = 2 + (i % 3),
                    speedX = speedX,
                    speedY = speedY,
                    density = 0.55f + (i % 4) * 0.1f
                )
            )
        }

        val initialRain = if (planetary.hydrospherePercent > 50f) {
            listOf(
                RainZone(centerTileX = 14, centerTileY = 12, radiusTiles = 4, precipitationRateMmHr = 22f),
                RainZone(centerTileX = 28, centerTileY = 16, radiusTiles = 3, precipitationRateMmHr = 15f)
            )
        } else emptyList()

        return WeatherState(
            clouds = clouds,
            dayNightCycleProgress = 0.25f,
            globalHumidityPercent = if (planetary.isSuperEarth) 78f else 64f,
            rainZones = initialRain,
            cyclonicVortexStrength = if (planetary.isEyeballWorld) 0.85f else 0.15f
        )
    }

    fun stepWeather(current: WeatherState, planetary: PlanetaryState, dtSeconds: Float): WeatherState {
        // 1. Advance cloud drift
        val updatedClouds = current.clouds.map { cloud ->
            var newX = cloud.xRatio + cloud.speedX * dtSeconds
            if (newX < 0f) newX += 1.0f
            if (newX > 1.0f) newX -= 1.0f

            var newY = cloud.yRatio + cloud.speedY * dtSeconds
            if (newY < 0.05f || newY > 0.95f) {
                // Bounce back into troposphere
                newY = newY.coerceIn(0.05f, 0.95f)
            }

            // On Eyeball worlds, draw clouds towards substellar center (0.5, 0.5)
            if (planetary.isEyeballWorld) {
                val dx = 0.5f - newX
                val dy = 0.5f - newY
                newX += dx * 0.01f * dtSeconds
                newY += dy * 0.01f * dtSeconds
            }

            cloud.copy(xRatio = newX, yRatio = newY)
        }

        // 2. Advance Day/Night lighting progress (0.0 to 1.0)
        val newProgress = if (planetary.isEyeballWorld) {
            0.5f // Fixed perpetual noon on day side, twilight on ring
        } else {
            (current.dayNightCycleProgress + current.dayNightSpeed * dtSeconds * 60f) % 1.0f
        }

        return current.copy(
            clouds = updatedClouds,
            dayNightCycleProgress = newProgress
        )
    }
}
