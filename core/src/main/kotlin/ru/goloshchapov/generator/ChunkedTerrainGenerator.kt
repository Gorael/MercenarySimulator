package ru.goloshchapov.generator

import ru.goloshchapov.terrain.TerrainType
import ru.goloshchapov.terrain.WorldConfig

class ChunkedTerrainGenerator(val worldConfig: WorldConfig) : Generator<Array<Array<TerrainType>>> {

    override fun generate(): Array<Array<TerrainType>> {
        val terrain: Array<Array<TerrainType>> =
            Array(worldConfig.width) { Array(worldConfig.height) { TerrainType.FIELD } }

        for (i in (worldConfig.width/4-worldConfig.blockSize/2)..(worldConfig.width/4+worldConfig.blockSize/2)) {
            for (j in (worldConfig.height/4-worldConfig.blockSize/2)..(worldConfig.height/4+worldConfig.blockSize/2)) {
                terrain[i][j] = TerrainType.FOREST
            }
        }

        for (i in (worldConfig.width/4*3-worldConfig.blockSize/2)..(worldConfig.width/4*3+worldConfig.blockSize/2)) {
            for (j in (worldConfig.height/4-worldConfig.blockSize/2)..(worldConfig.height/4+worldConfig.blockSize/2)) {
                terrain[i][j] = TerrainType.RIVER
            }
        }

        return terrain
    }
}
