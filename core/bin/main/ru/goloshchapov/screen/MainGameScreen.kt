package ru.goloshchapov.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import ktx.app.KtxScreen
import ru.goloshchapov.world.GameWorld
import ru.goloshchapov.ResourceManager
import ru.goloshchapov.TaskManager
import ru.goloshchapov.processor.DesktopInputProcessor

/**
 * Основной игровой экран, обрабатывающий рендеринг и обновление игрового состояния.
 * @property gameWorld Ссылка на игровой мир
 * @property resourceManager Менеджер ресурсов
 * @property taskManager Менеджер задач
 * @property camera Основная камера
 * @property batch Спрайтбатч для отрисовки
 */
class MainGameScreen(
    private val gameWorld: GameWorld,
    private val resourceManager: ResourceManager,
    private val taskManager: TaskManager
) : KtxScreen {
    private val camera = OrthographicCamera().apply {
        setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        zoom = 1f
    }
    private val batch = SpriteBatch()
    private val shapeRenderer = ShapeRenderer()
    private val inputProcessor = DesktopInputProcessor(camera, gameWorld)

    override fun show() {
        Gdx.input.inputProcessor = inputProcessor
    }

    override fun render(delta: Float) {
        update(delta)
        draw()
    }

    private fun update(delta: Float) {
        gameWorld.update(delta)
        taskManager.updateTasks()
    }

    private fun draw() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Обновляем матрицы для рендеринга
        batch.projectionMatrix = camera.combined
        shapeRenderer.projectionMatrix = camera.combined

        gameWorld.draw(shapeRenderer)
    }


    override fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
    }

    override fun resize(width: Int, height: Int) {
        camera.setToOrtho(false, width.toFloat(), height.toFloat())
        camera.update()
    }
}
