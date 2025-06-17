package ru.goloshchapov

import com.badlogic.gdx.math.Vector2
import ru.goloshchapov.world.GameWorld

/**
 * Управляет распределением задач между жителями.
 * @property gameWorld Ссылка на игровой мир
 * @property resourceManager Ссылка на менеджер ресурсов
 */
class TaskManager(
    private val gameWorld: GameWorld,
    private val resourceManager: ResourceManager
) {
    private val availableTasks = mutableListOf<Task>()

    fun updateTasks() {
        assignTasks()
        generateNewTasks()
    }

    private fun assignTasks() {
        gameWorld.villagers
            .filter { it.state == VillagerState.IDLE }
            .forEach { villager ->
                availableTasks.firstOrNull()?.let { task ->
                    villager.currentTask = task
                    villager.state = VillagerState.MOVING(task.targetPosition)
                    availableTasks.remove(task)
                }
            }
    }

    private fun generateNewTasks() {
        // Генерация задач по заготовке древесины
        if (resourceManager.getResource(ResourceType.WOOD) < 500) {
            gameWorld.resources
                .filter { it.type == ResourceType.WOOD && it.amount > 0 }
                .forEach { addUniqueTask(Task.Timber(it.position, 2f)) }
        }
    }

    private fun addUniqueTask(newTask: Task) {
        if (availableTasks.none { it.targetPosition.epsilonEquals(newTask.targetPosition) }) {
            availableTasks.add(newTask)
        }
    }
}

/**
 * Базовый класс для всех типов задач в игре.
 */
sealed class Task(val targetPosition: Vector2, val duration: Float) {
    class Timber(pos: Vector2, duration: Float = 2f) : Task(pos, duration)
    class Mine(pos: Vector2, duration: Float = 3f) : Task(pos, duration)
    class Build(pos: Vector2, duration: Float = 5f) : Task(pos, duration)
}
