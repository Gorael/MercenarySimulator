package ru.goloshchapov.terrain

import kotlin.math.abs

class AdvancedTerrainNoise(seed: Long) {
    private val random = java.util.Random(seed)

    // Комбинированный шум для разных элементов
    fun combinedNoise(x: Float, y: Float): Float {
        // Базовый шум для континентов
        val continent = 1 - abs(perlin(x * 0.01f, y * 0.01f, 4, 0.5f))

        // Ridged Multifractal для горных хребтов
        val mountains = ridgedMF(x * 0.02f, y * 0.02f, 6, 2.0f)

        // Детализированный шум для эрозии
        val details = perlin(x * 0.1f, y * 0.1f, 3, 0.3f) * 0.2f

        return (continent * 0.6f + mountains * 0.4f + details).coerceIn(0f..1f)
    }

    // Ridged Multifractal шум для гор
    private fun ridgedMF(x: Float, y: Float, octaves: Int, lacunarity: Float): Float {
        var noise = 0f
        var amplitude = 0.5f
        var frequency = 1.0f

        repeat(octaves) {
            val value = 1 - abs(perlin(x * frequency, y * frequency, 1, 0f))
            noise += value * amplitude
            amplitude *= 0.5f
            frequency *= lacunarity
        }
        return noise
    }

    // Модифицированный шум Перлина с октавами
    private fun perlin(x: Float, y: Float, octaves: Int, persistence: Float): Float {
        var total = 0f
        var frequency = 1f
        var amplitude = 1f
        var maxValue = 0f

        repeat(octaves) {
            total += smoothNoise(x * frequency, y * frequency) * amplitude
            maxValue += amplitude
            amplitude *= persistence
            frequency *= 2
        }
        return total / maxValue
    }

    // Упрощенная реализация шума
    private fun smoothNoise(x: Float, y: Float): Float {
        val xInt = x.toInt()
        val yInt = y.toInt()
        val fracX = x - xInt
        val fracY = y - yInt

        // Билинейная интерполяция
        val v1 = randomNoise(xInt, yInt)
        val v2 = randomNoise(xInt + 1, yInt)
        val v3 = randomNoise(xInt, yInt + 1)
        val v4 = randomNoise(xInt + 1, yInt + 1)

        val i1 = interpolate(v1, v2, fracX)
        val i2 = interpolate(v3, v4, fracX)
        return interpolate(i1, i2, fracY)
    }

    private fun interpolate(a: Float, b: Float, t: Float): Float {
        return a * (1 - t) + b * t
    }

    private fun randomNoise(x: Int, y: Int): Float {
        random.setSeed((x * 49632 + y * 325176).toLong())
        return random.nextFloat()
    }
}
