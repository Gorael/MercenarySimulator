package ru.flexbox

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Pool
import com.badlogic.gdx.Gdx

open class FlexItem : Group() {
    private val props: FlexItemProps
    private var backgroundColor: Color? = null
    private var backgroundTexture: TextureRegion? = null

    init {
        this.props = propsPool.obtain()
        setTouchable(Touchable.enabled)
        createDefaultBackground()

        // Default styles
        setBackgroundColor(Color(0.9f, 0.9f, 0.9f, 1f))

        // Invalidate parent layout on changes
        addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                invalidateParentLayout()
            }
        })
    }

    private fun createDefaultBackground() {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.fill()
        backgroundTexture = TextureRegion(Texture(pixmap))
        pixmap.dispose()
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        // Draw background
        if (backgroundColor != null) {
            batch.setColor(
                backgroundColor!!.r,
                backgroundColor!!.g,
                backgroundColor!!.b,
                backgroundColor!!.a * parentAlpha
            )
            batch.draw(backgroundTexture, getX(), getY(), getWidth(), getHeight())
        }

        // Draw children
        super.draw(batch, parentAlpha)
    }

    private fun invalidateParentLayout() {
        if (getParent() is FlexContainer) {
            (getParent() as FlexContainer).invalidateLayout()
        }
    }

    // ======== STYLE METHODS ========
    fun setBackgroundColor(color: Color?): FlexItem {
        this.backgroundColor = color
        return this
    }

    fun setMargin(margin: Float): FlexItem {
        props.marginTop = margin
        props.marginRight = margin
        props.marginBottom = margin
        props.marginLeft = margin
        invalidateParentLayout()
        return this
    }

    fun setMargin(top: Float, right: Float, bottom: Float, left: Float): FlexItem {
        props.marginTop = top
        props.marginRight = right
        props.marginBottom = bottom
        props.marginLeft = left
        invalidateParentLayout()
        return this
    }

    fun setPadding(padding: Float): FlexItem {
        props.paddingTop = padding
        props.paddingRight = padding
        props.paddingBottom = padding
        props.paddingLeft = padding
        invalidateParentLayout()
        return this
    }

    fun setPadding(top: Float, right: Float, bottom: Float, left: Float): FlexItem {
        props.paddingTop = top
        props.paddingRight = right
        props.paddingBottom = bottom
        props.paddingLeft = left
        invalidateParentLayout()
        return this
    }

    fun setFlexGrow(flexGrow: Float): FlexItem {
        props.flexGrow = flexGrow
        invalidateParentLayout()
        return this
    }

    fun setFlexShrink(flexShrink: Float): FlexItem {
        props.flexShrink = flexShrink
        invalidateParentLayout()
        return this
    }

    fun setFlexBasis(flexBasis: Float): FlexItem {
        props.flexBasis = flexBasis
        invalidateParentLayout()
        return this
    }

    fun setAlignSelf(flexAlignSelf: FlexAlign?): FlexItem {
        props.flexAlignSelf = flexAlignSelf
        invalidateParentLayout()
        return this
    }

    // ======== EVENT HANDLERS ========
    fun onClick(action: Runnable): FlexItem {
        addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                action.run()
            }
        })
        return this
    }

    fun onHover(enterAction: Runnable?, exitAction: Runnable?): FlexItem {
        addListener(object : InputListener() {
            override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                if (enterAction != null) enterAction.run()
            }

            override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                if (exitAction != null) exitAction.run()
            }
        })
        return this
    }

    override fun remove(): Boolean {
        propsPool.free(props)
        return super.remove()
    }

    override fun setWidth(width: Float) {
        props.widthValue = width
        props.widthUnit = FlexItemProps.SizeUnit.PX
        super.setWidth(width)
        invalidateParentLayout()
    }

    override fun setHeight(height: Float) {
        props.heightValue = height
        props.heightUnit = FlexItemProps.SizeUnit.PX
        super.setHeight(height)
        invalidateParentLayout()
    }

    override fun setSize(width: Float, height: Float) {
        props.widthValue = width
        props.widthUnit = FlexItemProps.SizeUnit.PX
        props.heightValue = height
        props.heightUnit = FlexItemProps.SizeUnit.PX
        super.setSize(width, height)
        invalidateParentLayout()
    }

    fun setWidthPercent(percent: Float): FlexItem {
        props.widthValue = percent
        props.widthUnit = FlexItemProps.SizeUnit.PERCENT
        invalidateParentLayout()
        return this
    }

    fun setHeightPercent(percent: Float): FlexItem {
        props.heightValue = percent
        props.heightUnit = FlexItemProps.SizeUnit.PERCENT
        invalidateParentLayout()
        return this
    }

    fun setSizePercent(widthPercent: Float, heightPercent: Float): FlexItem {
        props.widthValue = widthPercent
        props.widthUnit = FlexItemProps.SizeUnit.PERCENT
        props.heightValue = heightPercent
        props.heightUnit = FlexItemProps.SizeUnit.PERCENT
        invalidateParentLayout()
        return this
    }

    override fun act(delta: Float) {
        // === Calculate percent-based size if needed ===
        val parentWidth = (parent as? Group)?.width ?: Gdx.graphics.width.toFloat()
        val parentHeight = (parent as? Group)?.height ?: Gdx.graphics.height.toFloat()
        val resolvedWidth = if (props.widthUnit == FlexItemProps.SizeUnit.PERCENT && props.widthValue >= 0f) props.widthValue * parentWidth else if (props.widthValue >= 0f) props.widthValue else width
        val resolvedHeight = if (props.heightUnit == FlexItemProps.SizeUnit.PERCENT && props.heightValue >= 0f) props.heightValue * parentHeight else if (props.heightValue >= 0f) props.heightValue else height
        if (props.widthUnit == FlexItemProps.SizeUnit.PERCENT || props.heightUnit == FlexItemProps.SizeUnit.PERCENT) {
            super.setSize(resolvedWidth, resolvedHeight)
        }
        super.act(delta)
    }

    companion object {
        private val propsPool: Pool<FlexItemProps> = object : Pool<FlexItemProps>() {
            override fun newObject(): FlexItemProps {
                return FlexItemProps()
            }
        }

        @JvmStatic
        fun getProps(actor: Actor?): FlexItemProps? {
            if (actor is FlexItem) {
                return actor.props
            }
            return FlexItemProps()
        }
    }
}
