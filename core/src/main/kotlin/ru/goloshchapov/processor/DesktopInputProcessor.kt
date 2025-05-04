package ru.goloshchapov.processor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import ru.goloshchapov.world.GameWorld

class DesktopInputProcessor(val camera: OrthographicCamera, val gameWorld: GameWorld) : InputAdapter() {
    private val cameraMoveButton = Input.Buttons.RIGHT
    private val minZoom = 0.5f
    private val maxZoom = 3f
    private val zoomSpeed = 0.2f
    private val lastMousePos = Vector3()

    override fun keyDown(keycode: Int): Boolean {
        when (keycode) {
            Input.Keys.D -> {
                gameWorld.worldConfig.isDebugEnabled = !gameWorld.worldConfig.isDebugEnabled
                Gdx.app.log("Game is debug enabled", gameWorld.worldConfig.isDebugEnabled.toString())
                return true
            }
            else -> return false
        }
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button == cameraMoveButton) {
            lastMousePos.set(Vector2(screenX.toFloat(), screenY.toFloat()), 0f)
        }
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (Gdx.input.isButtonPressed(cameraMoveButton)) {
            // Конвертируем экранные координаты в мировые
            val delta = Vector2(
                (lastMousePos.x - screenX) * camera.zoom,
                (screenY - lastMousePos.y) * camera.zoom
            )
            camera.translate(delta)
            camera.update()
            lastMousePos.set(Vector2(screenX.toFloat(), screenY.toFloat()), 0f)
        }
        return true
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        // Инвертируем направление скролла
        val invertedAmountY = -amountY

        // Получаем экранные координаты мыши
        val screenX = Gdx.input.x.toFloat()
        val screenY = Gdx.input.y.toFloat()

        // Конвертируем в мировые координаты до зума
        val beforeZoom = Vector3(screenX, screenY, 0f).apply { camera.unproject(this) }

        // Применяем зум с правильным направлением
        camera.zoom = (camera.zoom * (1 - invertedAmountY * zoomSpeed)).coerceIn(minZoom, maxZoom)
        camera.update()

        // Конвертируем в мировые координаты после зума
        val afterZoom = Vector3(screenX, screenY, 0f).apply { camera.unproject(this) }

        // Корректируем позицию камеры
        camera.position.add(beforeZoom.x - afterZoom.x, beforeZoom.y - afterZoom.y, 0f)
        camera.update()

        return true
    }
}

