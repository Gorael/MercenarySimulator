package ru.goloshchapov.terrain

import kotlin.math.abs
import kotlin.random.Random

class RealisticTerrainGenerator(val config: WorldConfig) {
    private val heightNoise = AdvancedTerrainNoise(config.seed)
    private val moistureNoise = AdvancedTerrainNoise(config.seed + 1)

    // Класс для управления направлением течения
    private data class Direction(val dx: Int, val dy: Int) {
        companion object {
            val NONE = Direction(0, 0)
            val ALL_DIRECTIONS = listOf(
                Direction(-1, 0), Direction(1, 0),
                Direction(0, -1), Direction(0, 1),
                Direction(-1, -1), Direction(1, 1),
                Direction(-1, 1), Direction(1, -1)
            )
        }
    }

    fun generate(): Array<Array<TerrainType>> {
        val heightMap = generateHeightMap()
        val moistureMap = generateMoistureMap()

        applyHydraulicErosion(heightMap, 50)
        val terrain = classifyBaseTerrain(heightMap, moistureMap)
        generateRiverSystem(terrain, heightMap)

        return terrain
    }

    private fun generateHeightMap(): Array<FloatArray> {
        return Array(config.width) { x ->
            FloatArray(config.height) { y ->
                heightNoise.combinedNoise(x.toFloat(), y.toFloat())
            }
        }
    }

    private fun generateMoistureMap(): Array<FloatArray> {
        return Array(config.width) { x ->
            FloatArray(config.height) { y ->
                moistureNoise.combinedNoise(x * 2f, y * 2f) * 0.8f +
                    heightNoise.combinedNoise(x * 5f, y * 5f) * 0.2f
            }
        }
    }

    private fun classifyBaseTerrain(
        heightMap: Array<FloatArray>,
        moistureMap: Array<FloatArray>
    ): Array<Array<TerrainType>> {
        return Array(config.width) { x ->
            Array(config.height) { y ->
                when {
                    heightMap[x][y] > 0.8f -> TerrainType.MOUNTAIN
                    heightMap[x][y] > 0.65f -> TerrainType.FOREST
                    heightMap[x][y] < 0.3f -> TerrainType.LAKE
                    moistureMap[x][y] > 0.75f -> TerrainType.FIELD
                    else -> TerrainType.FIELD
                }
            }
        }
    }

    private fun generateRiverSystem(
        terrain: Array<Array<TerrainType>>,
        heightMap: Array<FloatArray>
    ) {
        val mainSources = findMainRiverSources(heightMap)
        mainSources.forEach { generateMainRiver(it, terrain, heightMap) }
    }

    private data class RiverSource(val x: Int, val y: Int, val strength: Float)

    private fun countHigherNeighbors(heightMap: Array<FloatArray>, x: Int, y: Int): Int {
        var count = 0
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until config.width && ny in 0 until config.height) {
                    if (heightMap[nx][ny] >= heightMap[x][y]) count++
                }
            }
        }
        return count
    }

    private data class RiverNode(
        val x: Int,
        val y: Int,
        var waterVolume: Float = 1f,
        val config: WorldConfig
    ) {
        fun isValid() = x in 0 until config.width && y in 0 until config.height // Исправлены границы
        fun move(dx: Int, dy: Int) = RiverNode(x + dx, y + dy, waterVolume, config)
        fun copy() = RiverNode(x, y, waterVolume, config)
    }

    private fun calculateRainfall(x: Int, y: Int): Float {
        val baseRain = 0.1f * (1 - heightNoise.combinedNoise(x / 50f, y / 50f))
        return baseRain.coerceIn(0f..0.3f)
    }

    private fun calculateRiverWidth(volume: Float): Int {
        return when {
            volume > 5f -> 3
            volume > 2f -> 2
            else -> 1
        }
    }

    private fun generateTributary(
        mainNode: RiverNode,
        terrain: Array<Array<TerrainType>>,
        heightMap: Array<FloatArray>
    ) {
        // Поиск подходящей точки для притока
        for (dx in -3..3) {
            for (dy in -3..3) {
                val x = mainNode.x + dx
                val y = mainNode.y + dy
                if (x in heightMap.indices && y in heightMap[0].indices &&
                    heightMap[x][y] > heightMap[mainNode.x][mainNode.y] &&
                    terrain[x][y] != TerrainType.RIVER
                ) {
                    generateTributaryBranch(x, y, terrain, heightMap)
                    return
                }
            }
        }
    }

    private fun generateTributaryBranch(
        startX: Int,
        startY: Int,
        terrain: Array<Array<TerrainType>>,
        heightMap: Array<FloatArray>
    ) {
        var current = RiverNode(startX, startY, 0.5f, config)
        repeat(200) {
            if (!current.isValid()) return

            val width = calculateRiverWidth(current.waterVolume)
            applyRiver(terrain, current.x, current.y, width)

            val dir = calculateNextDirection(current, heightMap, Direction.NONE)
            current = current.move(dir.dx, dir.dy)
            current.waterVolume *= 0.95f
        }
    }

    private fun findMainRiverSources(heightMap: Array<FloatArray>): List<RiverSource> {
        val sources = mutableListOf<RiverSource>()

        // Убрали шаг 5, проверяем все точки
        for (x in heightMap.indices) {
            for (y in heightMap[x].indices) {
                if (heightMap[x][y] > 0.8f &&
                    countHigherNeighbors(heightMap, x, y) >= 3) { // Уменьшили до 3
                    sources.add(RiverSource(x, y, heightMap[x][y]))
                }
            }
        }

        return sources.sortedByDescending { it.strength }.take(3)
    }

    private fun calculateNextDirection(
        current: RiverNode,
        heightMap: Array<FloatArray>,
        prevDirection: Direction
    ): Direction {
        val candidates = mutableListOf<Direction>() // Исправили опечатку

        Direction.ALL_DIRECTIONS.forEach { dir ->
            val newX = current.x + dir.dx
            val newY = current.y + dir.dy
            if (newX in heightMap.indices && newY in heightMap[0].indices) {
                val slope = heightMap[current.x][current.y] - heightMap[newX][newY]
                if (slope > 0) {
                    repeat((slope * 10).toInt().coerceAtLeast(1)) { // Гарантируем хотя бы 1 повтор
                        candidates.add(dir)
                    }
                }
            }
        }

        repeat(3) { candidates.add(prevDirection) }

        return candidates.randomOrNull() ?: Direction.NONE
    }

    // Упрощенная гидравлическая эрозия
    private fun applyHydraulicErosion(heightMap: Array<FloatArray>, iterations: Int) {
        repeat(iterations) {
            for (x in 1 until config.width - 1) {
                for (y in 1 until config.height - 1) {
                    val maxDelta = getMaxSlope(heightMap, x, y)
                    if (maxDelta > 0.03f) {
                        val amount = maxDelta * 0.5f
                        heightMap[x][y] -= amount
                        heightMap[x + 1][y] += amount * 0.25f
                        heightMap[x - 1][y] += amount * 0.25f
                        heightMap[x][y + 1] += amount * 0.25f
                        heightMap[x][y - 1] += amount * 0.25f
                    }
                }
            }
        }
    }

    private fun getMaxSlope(map: Array<FloatArray>, x: Int, y: Int): Float {
        val current = map[x][y]
        return maxOf(
            abs(current - map[x + 1][y]),
            abs(current - map[x - 1][y]),
            abs(current - map[x][y + 1]),
            abs(current - map[x][y - 1])
        )
    }

    private fun generateMainRiver(
        source: RiverSource,
        terrain: Array<Array<TerrainType>>,
        heightMap: Array<FloatArray>
    ) {
        var current = RiverNode(source.x, source.y, waterVolume = source.strength, config = config)
        var prevDirection = Direction.NONE
        val path = mutableListOf<RiverNode>()

        repeat(1000) {
            if (!current.isValid()) return

            // Проверяем, достигли ли мы озера
            if (terrain[current.x][current.y] == TerrainType.LAKE) {
                applyRiverDelta(terrain, current.x, current.y)
                return
            }

            // Обновление ширины реки
            val width = calculateRiverWidth(current.waterVolume)
            applyRiver(terrain, current.x, current.y, width)

            // Добавление притоков
            if (Random.nextFloat() < 0.1f * current.waterVolume) {
                generateTributary(current, terrain, heightMap)
            }

            // Расчет нового направления
            val newDir = calculateNextDirection(current, heightMap, prevDirection)
            prevDirection = newDir

            path.add(current.copy())
            current = current.move(newDir.dx, newDir.dy)
            current.waterVolume *= 0.98f

            // Добавление воды от осадков
            current.waterVolume += calculateRainfall(current.x, current.y)
        }
    }

    private fun applyRiver(
        terrain: Array<Array<TerrainType>>,
        x: Int,
        y: Int,
        width: Int
    ) {
        for (dx in -width..width) {
            for (dy in -width..width) {
                val xx = (x + dx).coerceIn(0 until config.width)
                val yy = (y + dy).coerceIn(0 until config.height)
                // Не перезаписываем озера и горы
                when (terrain[xx][yy]) {
                    TerrainType.LAKE, TerrainType.MOUNTAIN -> continue
                    else -> terrain[xx][yy] = TerrainType.RIVER
                }
            }
        }
    }

    private fun applyRiverDelta(
        terrain: Array<Array<TerrainType>>,
        x: Int,
        y: Int
    ) {
        // Создаем дельту реки на границе с озером
        for (dx in -2..2) {
            for (dy in -2..2) {
                val xx = (x + dx).coerceIn(0 until config.width)
                val yy = (y + dy).coerceIn(0 until config.height)
                if (terrain[xx][yy] == TerrainType.LAKE) continue
                terrain[xx][yy] = TerrainType.RIVER
            }
        }
    }
}
