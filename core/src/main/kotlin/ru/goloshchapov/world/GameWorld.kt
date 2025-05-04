package ru.goloshchapov.world

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import ru.goloshchapov.ResourceManager
import ru.goloshchapov.ResourceNode
import ru.goloshchapov.ResourceType
import ru.goloshchapov.Villager
import ru.goloshchapov.building.Building
import ru.goloshchapov.generator.BuildingGenerator
import ru.goloshchapov.generator.ChunkedTerrainGenerator
import ru.goloshchapov.terrain.TerrainType
import ru.goloshchapov.terrain.WorldConfig

/**
 * Представляет игровой мир, содержащий всех персонажей, здания и ресурсы.
 * @property villagers Список всех жителей
 * @property resources Список ресурсных узлов
 */
class GameWorld(
    private val resourceManager: ResourceManager,
    val worldConfig: WorldConfig
) {
    private val terrainGenerator = ChunkedTerrainGenerator(worldConfig)
    private val buildingGenerator = BuildingGenerator(worldConfig)

    private var terrain: Array<Array<TerrainType>> = emptyArray()
    private var buildings: Array<Building> = emptyArray()

    val villagers = mutableListOf<Villager>()
    val resources = mutableListOf<ResourceNode>()

    /**
     * Генерирует начальное состояние игрового мира
     */
    fun generateWorld() {
        terrain = terrainGenerator.generate()
        buildings = buildingGenerator.generate()

        repeat(5) { villagers.add(Villager()) }

        // Генерация деревьев
        repeat(20) {
            resources.add(
                ResourceNode(
                    ResourceType.WOOD,
                    100,
                    Vector2(
                        MathUtils.random(1, worldConfig.width - 1) * worldConfig.blockSize
                            .toFloat(),
                        MathUtils.random(1, worldConfig.height - 1) * worldConfig.blockSize
                            .toFloat()
                    )
                )
            )
        }
    }

    fun update(delta: Float) {
        villagers.forEach { it.update(delta, this) }
    }

    fun draw(shapeRenderer: ShapeRenderer) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        drawTerrain(shapeRenderer)
        drawBuildings(shapeRenderer)
        if (worldConfig.isDebugEnabled) {
            drawTerrainLines(shapeRenderer)
        }
        drawResources(shapeRenderer)
        drawVillagers(shapeRenderer)

        shapeRenderer.end()
    }

    private fun drawBuildings(shapeRenderer: ShapeRenderer) {
        for (building in buildings) {
            shapeRenderer.color = building.type.color
            shapeRenderer.rect(
                building.x.toFloat(),
                building.y.toFloat(),
                building.width.toFloat(),
                building.height.toFloat()
            )
        }
    }

    /**
     * Отрисовка жителей.
     */
    private fun drawVillagers(shapeRenderer: ShapeRenderer) {
        shapeRenderer.color = Color.WHITE
        villagers.forEach {
            shapeRenderer.circle(
                it.position.x + (worldConfig.blockSize / 2),
                it.position.y + (worldConfig.blockSize / 2),
                (worldConfig.blockSize / 2).toFloat()
            )
        }
    }

    /**
     * Отрисовка ресурсов.
     */
    private fun drawResources(shapeRenderer: ShapeRenderer) {
        shapeRenderer.color = Color.BLUE
        resources.forEach { resource ->
            shapeRenderer.circle(
                resource.position.x + (worldConfig.blockSize / 2),
                resource.position.y + (worldConfig.blockSize / 2),
                (worldConfig.blockSize / 2).toFloat()
            )
        }
    }

    private fun drawTerrain(shapeRenderer: ShapeRenderer) {
        for (x in 0 until worldConfig.width) {
            for (y in 0 until worldConfig.height) {
                shapeRenderer.color = terrain[x][y].color
                shapeRenderer.rect(
                    (x * worldConfig.blockSize).toFloat(),
                    (y * worldConfig.blockSize).toFloat(),
                    worldConfig.blockSize.toFloat(),
                    worldConfig.blockSize.toFloat()
                )
            }
        }
    }

    private fun drawTerrainLines(shapeRenderer: ShapeRenderer) {
        shapeRenderer.color = Color.RED
        for (x in 0 until worldConfig.width + 1) {
            shapeRenderer.line(
                (x * worldConfig.blockSize).toFloat(),
                0f,
                (x * worldConfig.blockSize).toFloat(),
                (worldConfig.height * worldConfig.blockSize).toFloat()
            )
        }
        for (y in 0 until worldConfig.height + 1) {
            shapeRenderer.line(
                0f,
                (y * worldConfig.blockSize).toFloat(),
                (worldConfig.width * worldConfig.blockSize).toFloat(),
                (y * worldConfig.blockSize).toFloat()
            )
        }
    }
}
