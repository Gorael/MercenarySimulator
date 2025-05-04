package ru.goloshchapov

import com.badlogic.gdx.math.Vector2

/**
 * Управляет хранением и модификацией ресурсов.
 */
class ResourceManager {
    private val resources = enumValues<ResourceType>().associateWith { 0 }.toMutableMap()

    fun addResource(type: ResourceType, amount: Int) {
        resources[type] = resources[type]!! + amount
    }

    fun getResource(type: ResourceType) = resources[type]!!

    fun hasResources(required: Map<ResourceType, Int>): Boolean {
        return required.all { (type, amount) -> resources[type]!! >= amount }
    }
}

/**
 * Ресурсный узел на карте мира.
 * @property type Тип ресурса
 * @property amount Оставшееся количество
 * @property position Позиция на карте
 */
class ResourceNode(
    val type: ResourceType,
    var amount: Int,
    val position: Vector2
)

enum class ResourceType {
    WOOD, STONE, FOOD, IRON
}
