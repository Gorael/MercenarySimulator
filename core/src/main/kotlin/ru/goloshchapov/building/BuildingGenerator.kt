package ru.goloshchapov.building

import ru.goloshchapov.generator.Generator
import ru.goloshchapov.terrain.WorldConfig
import kotlin.random.Random

class BuildingGenerator(private val worldConfig: WorldConfig) : Generator<Array<Building>> {
    override fun generate(): Array<Building> {
        val buildings: Array<Building> = Array(10) {Building()}
        repeat(10) { index ->
            run {
                buildings[index] = Building(
                    Random.nextInt(1, worldConfig.width - 1) * worldConfig.blockSize,
                    Random.nextInt(1, worldConfig.height - 1) * worldConfig.blockSize,
                    Random.nextInt(1, 5) * worldConfig.blockSize,
                    Random.nextInt(1, 5) * worldConfig.blockSize,
                    BuildingType.getRandom()
                )
            }
        }
        return buildings
    }

}
