package ru.goloshchapov.terrain

import com.badlogic.gdx.graphics.Color

enum class TerrainType(val color: Color) {
    FIELD(Color(0.4f, 0.6f, 0.3f, 1f)),
    FOREST(Color(0x013220ff)),
    RIVER(Color(0x120a8fff)),
    LAKE(Color(0x19868fff)),
    MOUNTAIN(Color(0x4D4D4Dff))
}
