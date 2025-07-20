package ru.goloshchapov.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.KtxScreen
import ru.goloshchapov.ResourceManager
import ru.goloshchapov.ResourceNode
import ru.goloshchapov.ResourceType
import ru.goloshchapov.TaskManager
import ru.goloshchapov.building.Building
import ru.goloshchapov.building.BuildingType
import ru.goloshchapov.processor.DesktopInputProcessor
import ru.goloshchapov.world.GameWorld

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
    private lateinit var stage: Stage
    private var dragPreview: Actor? = null
    private var dragBuildingType: BuildingType? = null
    private var dragResourceType: ResourceType? = null
    private var isDragging = false

    companion object {
        private const val SIDEBAR_WIDTH = 200f
        private const val CELL_WIDTH = 180f
        private const val CELL_HEIGHT = 60f
        private const val COLOR_SIZE = 40f
        private const val CELL_PAD = 10f
        private val CELL_BORDER_COLOR = Color.LIGHT_GRAY
        private val SIDEBAR_BG_COLOR = Color(0.2f, 0.2f, 0.3f, 1f)
    }

    override fun show() {
        // === UI Stage ===
        stage = Stage(ScreenViewport())
        val multiplexer = InputMultiplexer()
        multiplexer.addProcessor(stage)
        multiplexer.addProcessor(inputProcessor)
        // Добавим глобальный InputProcessor для ловли отпускания мыши вне Stage
        multiplexer.addProcessor(object : com.badlogic.gdx.InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                println("[GLOBAL] touchUp: isDragging=$isDragging, screenX=$screenX, screenY=$screenY, button=$button")
                handleDrop(screenX.toFloat(), screenY.toFloat(), button)
                return false
            }
        })
        Gdx.input.inputProcessor = multiplexer

        val sidebar = Table().apply {
            setWidth(SIDEBAR_WIDTH)
            setHeight(Gdx.graphics.height.toFloat())
            setPosition(0f, 0f)
            background = object : Drawable {
                override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
                    batch.color = SIDEBAR_BG_COLOR
                    batch.draw(BuildingPreviewActor.whiteTexture, x, y, width, height)
                    batch.color = Color.WHITE
                }
                override fun getLeftWidth() = 0f
                override fun setLeftWidth(width: Float) {}
                override fun getRightWidth() = 0f
                override fun setRightWidth(width: Float) {}
                override fun getTopHeight() = 0f
                override fun setTopHeight(height: Float) {}
                override fun getBottomHeight() = 0f
                override fun setBottomHeight(height: Float) {}
                override fun getMinWidth() = 0f
                override fun setMinWidth(width: Float) {}
                override fun getMinHeight() = 0f
                override fun setMinHeight(height: Float) {}
            }
            pad(10f)
        }

        // Постройки
        val buildings = listOf(
            Pair("HOUSE", BuildingType.HOUSE),
            Pair("FARM", BuildingType.FARM),
            Pair("BARRACKS", BuildingType.BARRACKS),
            Pair("TOWER", BuildingType.TOWER),
            Pair("STORAGE", BuildingType.STORAGE)
        )
        val font = BitmapFont()
        // Добавлять каждый stack по вертикали
        for ((displayName, buildingType) in buildings) {
            val cell = createUiCell(displayName, buildingType.color) {
                dragPreview?.remove()
                dragPreview = createDragPreview(buildingType.color)
                stage.addActor(dragPreview)
                dragBuildingType = buildingType
                dragResourceType = null
                isDragging = true
            }
            sidebar.add(cell).width(CELL_WIDTH).height(CELL_HEIGHT).padBottom(CELL_PAD).row()
        }

        // Ресурсы
        val resources = listOf(ResourceType.WOOD, ResourceType.STONE, ResourceType.FOOD, ResourceType.IRON)
        // Добавлять каждый stack по вертикали
        for (resType in resources) {
            val color = when(resType) {
                ResourceType.WOOD -> Color.SKY
                ResourceType.STONE -> Color.LIGHT_GRAY
                ResourceType.FOOD -> Color.GREEN
                ResourceType.IRON -> Color.NAVY
                else -> Color.WHITE
            }
            val cell = createUiCell(resType.name, color) {
                dragPreview?.remove()
                dragPreview = createDragPreview(color)
                stage.addActor(dragPreview)
                dragBuildingType = null
                dragResourceType = resType
                isDragging = true
            }
            sidebar.add(cell).width(CELL_WIDTH).height(CELL_HEIGHT).padBottom(CELL_PAD).row()
        }
        stage.addActor(sidebar)
        // --- Глобальный drag-and-drop обработчик ---
        stage.addListener(object : InputListener() {
            override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
                val screenX = Gdx.input.x.toFloat()
                val screenY = Gdx.input.y.toFloat()
                println("[STAGE] touchUp: isDragging=$isDragging, screenX=$screenX, screenY=$screenY, button=$button")
                handleDrop(screenX, screenY, button)
            }
        })
        // --- конец drag-and-drop ---
    }

    override fun render(delta: Float) {
        if (dragPreview != null) {
            val screenX = Gdx.input.x.toFloat()
            val screenY = Gdx.graphics.height - Gdx.input.y.toFloat()
            dragPreview!!.setPosition(screenX - COLOR_SIZE / 2, screenY - COLOR_SIZE / 2)
        }
        update(delta)
        draw()
        stage.act(delta)
        stage.draw()
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

        // --- ОТРИСОВКА СЕТКИ ПРИ DRAG ---
        if (isDragging) {
            val gridSize = gameWorld.worldConfig.blockSize
            // Границы видимой области камеры
            val camLeft = camera.position.x - camera.viewportWidth * 0.5f * camera.zoom
            val camRight = camera.position.x + camera.viewportWidth * 0.5f * camera.zoom
            val camBottom = camera.position.y - camera.viewportHeight * 0.5f * camera.zoom
            val camTop = camera.position.y + camera.viewportHeight * 0.5f * camera.zoom
            val minX = (camLeft / gridSize).toInt() * gridSize
            val maxX = ((camRight / gridSize).toInt() + 1) * gridSize
            val minY = (camBottom / gridSize).toInt() * gridSize
            val maxY = ((camTop / gridSize).toInt() + 1) * gridSize
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            shapeRenderer.color = Color.DARK_GRAY
            for (x in minX..maxX step gridSize) {
                shapeRenderer.line(x.toFloat(), minY.toFloat(), x.toFloat(), maxY.toFloat())
            }
            for (y in minY..maxY step gridSize) {
                shapeRenderer.line(minX.toFloat(), y.toFloat(), maxX.toFloat(), y.toFloat())
            }
            shapeRenderer.end()

            // Подсветка клетки под мышью
            val screenX = Gdx.input.x.toFloat()
            val screenY = Gdx.input.y.toFloat()
            if (screenX > SIDEBAR_WIDTH) {
                val worldCoords3 = camera.unproject(Vector3(screenX, screenY, 0f))
                val gridX = (worldCoords3.x / gridSize).toInt() * gridSize
                val gridY = (worldCoords3.y / gridSize).toInt() * gridSize
                val cellFree = isCellFree(gridX, gridY)
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                shapeRenderer.color = if (cellFree) Color(0f, 1f, 0f, 0.3f) else Color(1f, 0f, 0f, 0.3f)
                shapeRenderer.rect(gridX.toFloat(), gridY.toFloat(), gridSize.toFloat(), gridSize.toFloat())
                shapeRenderer.end()
            }
        }
    }

    // Проверка занятости клетки
    private fun isCellFree(gridX: Int, gridY: Int): Boolean {
        val blockSize = gameWorld.worldConfig.blockSize
        // Проверяем террейн
        val x = gridX / blockSize
        val y = gridY / blockSize
        try {
            val terrainField = gameWorld.javaClass.getDeclaredField("terrain")
            terrainField.isAccessible = true
            val terrain = terrainField.get(gameWorld) as Array<Array<*>>
            val terrainType = terrain.getOrNull(x)?.getOrNull(y)
            // Разрешаем строить только на FIELD
            if (terrainType != null && terrainType.toString() != "FIELD") return false
        } catch (_: Exception) {}
        // Проверяем здания
        try {
            val buildingsField = gameWorld.javaClass.getDeclaredField("buildings")
            buildingsField.isAccessible = true
            val arr = buildingsField.get(gameWorld) as Array<Building>
            for (b in arr) {
                if (b.x == gridX && b.y == gridY) return false
            }
        } catch (_: Exception) {}
        // Проверяем ресурсы
        try {
            val resourcesField = gameWorld.javaClass.getDeclaredField("resources")
            resourcesField.isAccessible = true
            val arr = resourcesField.get(gameWorld) as MutableList<ResourceNode>
            for (r in arr) {
                if (r.position.x.toInt() == gridX && r.position.y.toInt() == gridY) return false
            }
        } catch (_: Exception) {}
        return true
    }

    override fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
        stage.dispose()
    }

    override fun resize(width: Int, height: Int) {
        camera.setToOrtho(false, width.toFloat(), height.toFloat())
        camera.update()
        stage.viewport.update(width, height, true)
    }

    // Добавить строение в игровой мир
    private fun addBuildingAt(worldCoords: Vector2, type: BuildingType) {
        val blockSize = gameWorld.worldConfig.blockSize
        // Snap to grid
        val gridX = (worldCoords.x / blockSize).toInt() * blockSize
        val gridY = (worldCoords.y / blockSize).toInt() * blockSize
        val building = Building(
            x = gridX,
            y = gridY,
            width = blockSize,
            height = blockSize,
            type = type
        )
        try {
            val buildingsField = gameWorld.javaClass.getDeclaredField("buildings")
            buildingsField.isAccessible = true
            val arr = buildingsField.get(gameWorld) as Array<Building>
            val newArr = arr + building
            buildingsField.set(gameWorld, newArr)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Добавить ресурсную точку в игровой мир
    private fun addResourceAt(worldCoords: Vector2, type: ResourceType) {
        val blockSize = gameWorld.worldConfig.blockSize
        // Snap to grid
        val gridX = (worldCoords.x / blockSize).toInt() * blockSize
        val gridY = (worldCoords.y / blockSize).toInt() * blockSize
        val node = ResourceNode(
            type = type,
            amount = 100,
            position = Vector2(gridX.toFloat(), gridY.toFloat())
        )
        try {
            val resourcesField = gameWorld.javaClass.getDeclaredField("resources")
            resourcesField.isAccessible = true
            val arr = resourcesField.get(gameWorld) as MutableList<ResourceNode>
            arr.add(node)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createUiCell(
        displayName: String,
        color: Color,
        onDragInit: () -> Unit
    ): Table {
        val row = Table()
        row.background = object : Drawable {
            override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
                batch.color = CELL_BORDER_COLOR
                batch.draw(BuildingPreviewActor.whiteTexture, x, y, width, height)
                batch.color = Color.WHITE
            }
            override fun getLeftWidth() = 0f
            override fun setLeftWidth(width: Float) {}
            override fun getRightWidth() = 0f
            override fun setRightWidth(width: Float) {}
            override fun getTopHeight() = 0f
            override fun setTopHeight(height: Float) {}
            override fun getBottomHeight() = 0f
            override fun setBottomHeight(height: Float) {}
            override fun getMinWidth() = 0f
            override fun setMinWidth(width: Float) {}
            override fun getMinHeight() = 0f
            override fun setMinHeight(height: Float) {}
        }
        val colorActor = object : Actor() {
            override fun draw(batch: Batch, parentAlpha: Float) {
                batch.color = color.cpy().also { it.a = 1f }
                batch.draw(BuildingPreviewActor.whiteTexture, x, y, COLOR_SIZE, COLOR_SIZE)
                batch.color = Color.WHITE
            }
        }
        val labelStyle = com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(BitmapFont(), Color.BLACK)
        val label = com.badlogic.gdx.scenes.scene2d.ui.Label(displayName, labelStyle)
        label.setFontScale(0.8f)
        row.add(colorActor).width(COLOR_SIZE).height(COLOR_SIZE).pad(CELL_PAD)
        row.add(label).left().padLeft(10f)
        row.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                onDragInit()
                return true
            }
        })
        return row
    }

    private fun createDragPreview(color: Color): Actor {
        return object : Actor() {
            override fun draw(batch: Batch, parentAlpha: Float) {
                batch.color = color.cpy().also { it.a = 0.6f }
                batch.draw(BuildingPreviewActor.whiteTexture, this.x, this.y, COLOR_SIZE, COLOR_SIZE)
                batch.color = Color.WHITE
            }
        }.apply {
            setPosition(Gdx.input.x - COLOR_SIZE / 2, Gdx.graphics.height - Gdx.input.y - COLOR_SIZE / 2)
            touchable = com.badlogic.gdx.scenes.scene2d.Touchable.disabled
        }
    }

    // --- drop-логика ---
    private fun handleDrop(screenX: Float, screenY: Float, button: Int) {
        if (isDragging) {
            // Вставка только по левой кнопке мыши
            if (button != 0) return
            dragPreview?.let { preview ->
                if (screenX > SIDEBAR_WIDTH) {
                    val worldCoords3 = camera.unproject(Vector3(screenX, screenY, 0f))
                    val gridX = (worldCoords3.x / gameWorld.worldConfig.blockSize).toInt() * gameWorld.worldConfig.blockSize
                    val gridY = (worldCoords3.y / gameWorld.worldConfig.blockSize).toInt() * gameWorld.worldConfig.blockSize
                    if (isCellFree(gridX, gridY)) {
                        val worldCoords = Vector2(gridX.toFloat(), gridY.toFloat())
                        dragBuildingType?.let { type ->
                            addBuildingAt(worldCoords, type)
                        }
                        dragResourceType?.let { type ->
                            addResourceAt(worldCoords, type)
                        }
                        preview.remove()
                        dragPreview = null
                        dragBuildingType = null
                        dragResourceType = null
                        isDragging = false
                    } else {
                        println("[DROP] Клетка занята: $gridX, $gridY")
                        // Не сбрасываем перенос, пользователь может выбрать другую клетку
                    }
                } else {
                    // Если мышь вне игрового поля, можно сбросить перенос
                    preview.remove()
                    dragPreview = null
                    dragBuildingType = null
                    dragResourceType = null
                    isDragging = false
                }
            }
        }
    }
}

// Мини-превью здания для UI и drag-and-drop
class BuildingPreviewActor(val type: BuildingType, width: Float = 40f, height: Float = 40f) : Actor() {
    var alpha: Float = 1f
    init { setSize(width, height) }
    override fun draw(batch: Batch, parentAlpha: Float) {
        batch.color = type.color.cpy().also { it.a = alpha * parentAlpha }
        batch.draw(whiteTexture, x, y, this.width, this.height)
        batch.color = com.badlogic.gdx.graphics.Color.WHITE // reset
    }
    companion object {
        val whiteTexture: Texture by lazy {
            val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
            pixmap.setColor(Color.WHITE)
            pixmap.fill()
            val tex = Texture(pixmap)
            pixmap.dispose()
            tex
        }
    }
}
