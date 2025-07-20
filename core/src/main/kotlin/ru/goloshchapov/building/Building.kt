package ru.goloshchapov.building

import com.badlogic.gdx.graphics.Color
import kotlin.random.Random

data class Building(
    val x: Int = 0,
    val y: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val type: BuildingType = BuildingType.HOUSE
)

enum class BuildingType(val color: Color) {
    HOUSE(Color.ORANGE),
    FARM(Color.YELLOW),
    BARRACKS(Color.FIREBRICK),
    TOWER(Color.SKY),
    STORAGE(Color.BROWN);

    companion object {
        fun getRandom(): BuildingType {
            return entries[Random.nextInt(0, entries.size)]
        }
    }
}
