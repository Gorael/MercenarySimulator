package ru.goloshchapov.terrain

data class WorldConfig(
    val width: Int = 256,
    val height: Int = 256,
    val seed: Long = 85478,
    val noiseScale: Float = 50f,   // Масштаб шума
    val mountainThreshold: Float = 0.8f,
    val lakeThreshold: Float = 0.3f,
    val riverChance: Float = 0.05f, // Шанс генерации реки
    val blockSize: Int = 16,
    var isDebugEnabled: Boolean = false
)
