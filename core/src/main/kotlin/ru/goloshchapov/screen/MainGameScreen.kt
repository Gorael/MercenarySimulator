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
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ru.flexbox.FlexContainer
import ru.flexbox.FlexDirection
import ru.flexbox.FlexItem
import ru.flexbox.FlexJustify
import ru.flexbox.FlexAlign
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import ru.goloshchapov.building.Building
import ru.goloshchapov.building.BuildingType
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.InputMultiplexer

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

    override fun show() {
        // === UI Stage ===
        stage = Stage(ScreenViewport())
        val multiplexer = InputMultiplexer()
        multiplexer.addProcessor(stage)
        multiplexer.addProcessor(inputProcessor)
        Gdx.input.inputProcessor = multiplexer
        // Flexbox sidebar
        val sidebar = FlexContainer().apply {
            setDirection(FlexDirection.COLUMN)
            setWidthPercent(0.2f)
            setHeightPercent(1f)
            setBackgroundColor(com.badlogic.gdx.graphics.Color(0.2f, 0.2f, 0.3f, 1f))
            setAlign(FlexAlign.CENTER)
            setJustify(FlexJustify.CENTER)
            setGap(10f)
            setPadding(10f)
        }

        // Горизонтальный контейнер для иконок построек
        val itemRow = FlexContainer().apply {
            setDirection(FlexDirection.ROW)
            setGap(10f)
            setPadding(5f)
            setJustify(FlexJustify.SPACE_EVENLY)
            setWidthPercent(1f)
        }

        // Примеры построек (иконки и названия)
        val buildings = listOf(
            Triple("Дом", "house.png", BuildingType.HOUSE),
            Triple("Ферма", "farm.png", BuildingType.FARM)
        )
        val font = BitmapFont()
        for ((name, icon, buildingType) in buildings) {
            val item = object : FlexItem() {
                override fun draw(batch: com.badlogic.gdx.graphics.g2d.Batch, parentAlpha: Float) {
                    super.draw(batch, parentAlpha)
                    font.color = com.badlogic.gdx.graphics.Color.WHITE
                    font.data.setScale(1.0f)
                    font.draw(batch, buildingType.name, x + 50f, y + height - 20f)
                }
            }.apply {
                setBackgroundColor(com.badlogic.gdx.graphics.Color(0.3f, 0.3f, 0.4f, 1f))
                setSize(120f, 80f)
                setMargin(5f)
                setPadding(5f)
                val preview = BuildingPreviewActor(buildingType)
                preview.setPosition(5f, 20f)
                addActor(preview)
                // Drag-and-drop listener
                addListener(object : InputListener() {
                    override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                        // Создать preview
                        dragPreview?.remove()
                        val s = this@apply.stage
                        if (s != null) {
                            dragPreview = BuildingPreviewActor(buildingType).apply {
                                alpha = 0.6f
                                setPosition(Gdx.input.x - width / 2, Gdx.graphics.height - Gdx.input.y - height / 2)
                                s.addActor(this)
                                s.root.children.removeValue(this, true)
                                s.addActor(this)
                            }
                        }
                        dragBuildingType = buildingType
                        return true
                    }
                    override fun touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int) {
                        // Preview следует за мышью
                        dragPreview?.let {
                            val screenX = Gdx.input.x.toFloat()
                            val screenY = Gdx.graphics.height - Gdx.input.y.toFloat()
                            it.setPosition(screenX - it.width / 2, screenY - it.height / 2)
                        }
                    }
                    override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
                        // Drop: если мышь над игровой областью (не sidebar), добавить Building
                        dragPreview?.let { preview ->
                            val screenX = Gdx.input.x.toFloat()
                            val screenY = Gdx.input.y.toFloat()
                            // Проверяем, что не над sidebar (20% ширины)
                            if (screenX > Gdx.graphics.width * 0.2f) {
                                // Переводим экранные координаты в мировые
                                val worldCoords3 = camera.unproject(Vector3(screenX, screenY, 0f))
                                val worldCoords = Vector2(worldCoords3.x, worldCoords3.y)
                                dragBuildingType?.let { type ->
                                    addBuildingAt(worldCoords, type)
                                }
                            }
                            preview.remove()
                            dragPreview = null
                            dragBuildingType = null
                        }
                    }
                })
            }
            itemRow.addActor(item)
        }
        sidebar.addActor(itemRow)
        stage.addActor(sidebar)
    }

    override fun render(delta: Float) {
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
        // Пример: фиксированный размер 32x32
        val building = Building(
            x = worldCoords.x.toInt(),
            y = worldCoords.y.toInt(),
            width = 32,
            height = 32,
            type = type
        )
        // Добавить в GameWorld (добавьте метод, если нужно)
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
