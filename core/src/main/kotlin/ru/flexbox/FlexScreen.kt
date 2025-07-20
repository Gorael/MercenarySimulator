package ru.flexbox

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import ktx.app.KtxScreen
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.Texture

class FlexScreen(
) : KtxScreen {
    private val camera = OrthographicCamera().apply {
        setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        zoom = 1f
    }
    private val batch = SpriteBatch()
    private lateinit var rootContainer: FlexContainer

    override fun show() {
        camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

        // Create root container that fills the screen
        rootContainer = FlexContainer().apply {
            setSize(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
            setBackgroundColor(Color(0.8f, 0.8f, 0.8f, 1f)) // Light gray background
            setPadding(20f) // Padding around the screen
        }

        // Create a vertical container for the left sidebar
        val sidebar = FlexContainer().apply {
            setDirection(FlexDirection.COLUMN)
            setBackgroundColor(Color(0.5f, 0.5f, 0.6f, 1f)) // Blueish background
            setPadding(10f)
            setGap(10f)
            setWidthPercent(0.2f) // 20% ширины экрана
            setAlign(FlexAlign.CENTER) // Центрирование по вертикали
            setHeightPercent(1f) // Sidebar занимает всю высоту экрана
            setJustify(FlexJustify.CENTER) // Центрирование itemRow по вертикали
        }

        // === Горизонтальный контейнер для элементов с изображением и описанием ===
        val itemRow = FlexContainer().apply {
            setDirection(FlexDirection.ROW)
            setGap(10f)
            setPadding(5f)
            setJustify(FlexJustify.SPACE_EVENLY) // Равномерное распределение по ширине
            setWidthPercent(1f) // itemRow занимает всю ширину sidebar
        }

        val texture = Texture(Gdx.files.internal("assets/logo.png"))
        val font = BitmapFont()

        val items = listOf(
            "Мечник" to "Ближний бой",
            "Лучник" to "Дальний бой",
            "Кавалерист" to "Быстрый юнит"
        )
        for ((title, desc) in items) {
            val item = object : FlexItem() {
                override fun draw(batch: com.badlogic.gdx.graphics.g2d.Batch, parentAlpha: Float) {
                    super.draw(batch, parentAlpha)
                    // Рисуем текст
                    font.color = Color.BLACK
                    font.data.setScale(1.0f)
                    font.draw(batch, title, x + 50f, y + height - 20f)
                    font.draw(batch, desc, x + 50f, y + height - 40f)
                }
            }.apply {
                setBackgroundColor(Color(0.9f, 0.9f, 1f, 1f))
                setSize(120f, 80f)
                setMargin(5f)
                setPadding(5f)
                // Добавляем изображение
                val image = Image(texture)
                image.setSize(40f, 40f)
                image.setPosition(5f, 20f)
                addActor(image)
            }
            itemRow.addActor(item)
        }
        sidebar.addActor(itemRow)

        // Create main content area
        val mainContent = FlexContainer().apply {
            setBackgroundColor(Color(0.6f, 0.7f, 0.6f, 1f)) // Greenish background
        }

        // Configure root as horizontal row
        rootContainer.apply {
            setDirection(FlexDirection.ROW)
            setGap(20f)
            addActor(sidebar)
            addActor(mainContent)
        }
    }

    override fun render(delta: Float) {
        // Clear screen
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Update camera
        camera.update()
        batch.projectionMatrix = camera.combined

        // Update and draw flex container
        rootContainer.act(delta)
        batch.begin()
        rootContainer.draw(batch, 1f)
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        camera.setToOrtho(false, width.toFloat(), height.toFloat())
        camera.update()
        rootContainer.setSize(width.toFloat(), height.toFloat())
    }

    override fun dispose() {
        batch.dispose()
        // Dispose of any other resources if needed
    }
}
