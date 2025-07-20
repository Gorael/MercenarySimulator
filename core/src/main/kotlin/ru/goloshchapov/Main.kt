package ru.goloshchapov

import com.badlogic.gdx.graphics.Color
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ru.flexbox.FlexAlign
import ru.flexbox.FlexContainer
import ru.flexbox.FlexDirection
import ru.flexbox.FlexItem
import ru.flexbox.FlexJustify
import ru.goloshchapov.Main.Companion.screenHeight
import ru.goloshchapov.Main.Companion.screenWidth
import ru.flexbox.FlexScreen
import ru.goloshchapov.screen.MainGameScreen
import ru.goloshchapov.terrain.WorldConfig
import ru.goloshchapov.world.GameWorld

/**
 * Главный класс игры, отвечающий за инициализацию и управление экранами.
 * @property screenWidth Ширина экрана
 * @property screenHeight Высота экрана
 */
class Main : KtxGame<KtxScreen>() {
    companion object {
        const val screenWidth = 1280
        const val screenHeight = 720
    }

    override fun create() {
        val resourceManager = ResourceManager()
        val worldConfig = WorldConfig()
        val gameWorld = GameWorld(resourceManager, worldConfig).apply { generateWorld() }
        val taskManager = TaskManager(gameWorld, resourceManager)
        addScreen(MainGameScreen(gameWorld, resourceManager, taskManager))
        setScreen<MainGameScreen>()

//        addScreen(FlexScreen())
//        setScreen<FlexScreen>()
    }
}











