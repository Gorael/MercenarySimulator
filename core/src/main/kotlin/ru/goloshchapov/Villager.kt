package ru.goloshchapov

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import ru.goloshchapov.world.GameWorld

/**
 * Представляет жителя деревни с инвентарем и состоянием.
 * @property position Текущая позиция на карте
 * @property state Текущее состояние поведения
 * @property speed Скорость перемещения (пикселей в секунду)
 * @property inventory Инвентарь для переноски ресурсов
 */
class Villager(
    var position: Vector2 = Vector2(MathUtils.random(100f, 1180f), MathUtils.random(100f, 620f)),
    var state: VillagerState = VillagerState.IDLE,
    private val speed: Float = 100f
) {
    var currentTask: Task? = null
    val inventory = mutableMapOf<ResourceType, Int>()

    /**
     * Обновляет состояние жителя каждый кадр
     * @param delta Время с последнего кадра
     * @param world Ссылка на игровой мир
     */
    fun update(delta: Float, world: GameWorld) {
        when (state) {
            is VillagerState.MOVING -> handleMovement(delta, world)
            is VillagerState.WORKING -> handleWork(delta, world)
            else -> {}
        }
    }

    private fun handleMovement(delta: Float, world: GameWorld) {
        val target = (state as VillagerState.MOVING).target
        val direction = target.cpy().sub(position).nor()
        val distance = position.dst(target)

        if (distance > 5f) {
            position.add(direction.scl(speed * delta))
        } else {
            currentTask?.let { task ->
                state = when (task) {
                    is Task.Timber -> VillagerState.WORKING(task.duration)
                    else -> VillagerState.WORKING(2f)
                }
            }
        }
    }

    private fun handleWork(delta: Float, world: GameWorld) {
        val remainingTime = (state as VillagerState.WORKING).remainingTime - delta
        if (remainingTime <= 0) {
            completeTask(world)
            state = VillagerState.IDLE
        } else {
            state = VillagerState.WORKING(remainingTime)
        }
    }

    private fun completeTask(world: GameWorld) {
        when (val task = currentTask) {
            is Task.Timber -> {
                val node = world.resources.find { it.position.epsilonEquals(task.targetPosition, 5f) }
                node?.let {
                    it.amount -= 10
                    inventory[ResourceType.WOOD] = inventory.getOrDefault(ResourceType.WOOD, 0) + 10
                }
            }
            // Обработка других типов задач
            is Task.Build -> TODO()
            is Task.Mine -> TODO()
            null -> TODO()
        }
        currentTask = null
    }
}

/**
 * Состояния жителя в формате FSM (Конечный автомат)
 */
sealed class VillagerState {
    object IDLE : VillagerState()
    data class MOVING(val target: Vector2) : VillagerState()
    data class WORKING(val remainingTime: Float) : VillagerState()
}
