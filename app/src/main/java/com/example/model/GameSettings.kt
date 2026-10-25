package com.example.model

enum class AppScreen {
    START_MENU,
    SIMULATION
}

enum class TemperatureUnit(val symbol: String) {
    CELSIUS("°C"),
    KELVIN("K")
}

data class GameSettings(
    val masterVolume: Float = 0.8f, // 0.0 to 1.0
    val audioEnabled: Boolean = true,
    val pixelTileSize: Float = 14f, // 14f for chunky 16-bit, 8f for fine
    val coastalFoamAnimation: Boolean = true,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val autoPauseOnCataclysm: Boolean = true,
    val showMiniMap: Boolean = true,
    val showActiveToolPill: Boolean = true,
    val showClimateTicker: Boolean = true
)
