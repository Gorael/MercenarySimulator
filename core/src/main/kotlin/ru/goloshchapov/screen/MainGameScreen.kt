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
import ru.flexbox.FlexContainer
import ru.flexbox.FlexDirection
import ru.flexbox.FlexItem
import ru.flexbox.FlexJustify
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

    override fun show() {
        // === UI Stage ===
        stage = Stage(ScreenViewport())
        val multiplexer = InputMultiplexer()
        multiplexer.addProcessor(stage)
        multiplexer.addProcessor(inputProcessor)
        Gdx.input.inputProcessor = multiplexer
        // Flexbox sidebar
        val sidebar = Table().apply {
            setWidth(200f)
            setHeight(Gdx.graphics.height.toFloat())
            setPosition(0f, 0f)
            // Фон sidebar (опционально)
            background = object : Drawable {
                override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
                    batch.color = com.badlogic.gdx.graphics.Color(0.2f, 0.2f, 0.3f, 1f)
                    batch.draw(BuildingPreviewActor.whiteTexture, x, y, width, height)
                    batch.color = com.badlogic.gdx.graphics.Color.WHITE
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

        // Горизонтальный контейнер для иконок построек
        val itemRow = FlexContainer().apply {
            setDirection(FlexDirection.ROW)
            setGap(10f)
            setPadding(5f)
            setJustify(FlexJustify.SPACE_EVENLY)
            setWidthPercent(1f)
            setHeight(150f)
            setBackgroundColor(com.badlogic.gdx.graphics.Color(0.2f, 0.5f, 0.2f, 0.5f)) // зеленый
        }

        // Примеры построек (иконки и названия)
        val buildings = listOf(
            Pair("HOUSE", BuildingType.HOUSE),
            Pair("FARM", BuildingType.FARM),
            Pair("BARRACKS", BuildingType.BARRACKS),
            Pair("TOWER", BuildingType.TOWER),
            Pair("STORAGE", BuildingType.STORAGE)
        )
        val font = BitmapFont()
        for ((displayName, buildingType) in buildings) {
            val item = object : FlexItem() {
                override fun draw(batch: com.badlogic.gdx.graphics.g2d.Batch, parentAlpha: Float) {
                    super.draw(batch, parentAlpha)
                    font.color = com.badlogic.gdx.graphics.Color.BLACK
                    font.data.setScale(1.0f)
                    font.draw(batch, displayName, x + 50f, y + height - 20f)
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
        sidebar.add(itemRow).width(200f).height(150f).padBottom(20f).row()

        // === Секция ресурсов ===
        val resourceRow = FlexContainer().apply {
            setDirection(FlexDirection.ROW)
            setGap(10f)
            setPadding(5f)
            setJustify(FlexJustify.SPACE_EVENLY)
            setWidthPercent(1f)
            setHeight(150f)
            setBackgroundColor(com.badlogic.gdx.graphics.Color(0.7f, 0.2f, 0.2f, 0.5f)) // красный
        }
        val resources = listOf(ResourceType.WOOD, ResourceType.STONE, ResourceType.FOOD, ResourceType.IRON)
        for (resType in resources) {
            val item = object : FlexItem() {
                override fun draw(batch: com.badlogic.gdx.graphics.g2d.Batch, parentAlpha: Float) {
                    super.draw(batch, parentAlpha)
                    font.color = com.badlogic.gdx.graphics.Color.WHITE
                    font.data.setScale(1.0f)
                    font.draw(batch, resType.name, x + 50f, y + height - 20f)
                }
            }.apply {
                setBackgroundColor(com.badlogic.gdx.graphics.Color(0.2f, 0.3f, 0.4f, 1f))
                setSize(120f, 80f)
                setMargin(5f)
                setPadding(5f)
                val preview = ResourcePreviewActor(resType)
                preview.setPosition(5f, 20f)
                addActor(preview)
                addListener(object : InputListener() {
                    override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                        dragPreview?.remove()
                        val s = this@apply.stage
                        if (s != null) {
                            dragPreview = ResourcePreviewActor(resType).apply {
                                alpha = 0.6f
                                setPosition(Gdx.input.x - 20f, Gdx.graphics.height - Gdx.input.y - 20f)
                                s.addActor(this)
                                s.root.children.removeValue(this, true)
                                s.addActor(this)
                            }
                        }
                        dragBuildingType = null
                        dragResourceType = resType
                        return true
                    }
                    override fun touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int) {
                        dragPreview?.let {
                            val screenX = Gdx.input.x.toFloat()
                            val screenY = Gdx.graphics.height - Gdx.input.y.toFloat()
                            it.setPosition(screenX - 20f, screenY - 20f)
                        }
                    }
                    override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
                        dragPreview?.let { preview ->
                            val screenX = Gdx.input.x.toFloat()
                            val screenY = Gdx.input.y.toFloat()
                            if (screenX > Gdx.graphics.width * 0.2f) {
                                val worldCoords3 = camera.unproject(Vector3(screenX, screenY, 0f))
                                val worldCoords = Vector2(worldCoords3.x, worldCoords3.y)
                                dragResourceType?.let { type ->
                                    addResourceAt(worldCoords, type)
                                }
                            }
                            preview.remove()
                            dragPreview = null
                            dragResourceType = null
                        }
                    }
                })
            }
            resourceRow.addActor(item)
        }
        sidebar.add(resourceRow).width(200f).height(150f).row()
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

    // Добавить ресурсную точку в игровой мир
    private fun addResourceAt(worldCoords: Vector2, type: ResourceType) {
        val node = ResourceNode(
            type = type,
            amount = 100,
            position = worldCoords
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

// Preview для ресурсов (голубая точка и др.)
class ResourcePreviewActor(val type: ResourceType, val size: Float = 40f) : Actor() {
    var alpha: Float = 1f
    override fun draw(batch: Batch, parentAlpha: Float) {
        batch.color = getColor(type).cpy().also { it.a = alpha * parentAlpha }
        batch.draw(BuildingPreviewActor.whiteTexture, x, y, size, size)
        batch.color = Color.WHITE
    }
    private fun getColor(type: ResourceType): Color = when(type) {
        ResourceType.WOOD -> Color.SKY
        ResourceType.STONE -> Color.LIGHT_GRAY
        ResourceType.FOOD -> Color.GREEN
        ResourceType.IRON -> Color.NAVY
        else -> Color.WHITE
    }
}
