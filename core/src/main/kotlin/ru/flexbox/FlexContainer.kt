package ru.flexbox

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import ru.flexbox.FlexItem.Companion.getProps
import com.badlogic.gdx.Gdx

class FlexContainer : Group() {
    // Flex properties
    private var flexDirection: FlexDirection? = FlexDirection.ROW
    private var flexJustify = FlexJustify.FLEX_START
    private var flexAlign = FlexAlign.STRETCH
    private var flexWrap: FlexWrap? = FlexWrap.NO_WRAP
    private var gap = 0f

    // Spacing
    private var paddingTop = 0f
    private var paddingRight = 0f
    private var paddingBottom = 0f
    private var paddingLeft = 0f

    // Background
    private var backgroundColor: Color? = null
    private var backgroundTexture: TextureRegion? = null
    private var layoutInvalidated = true

    // ======== SIZE UNIT SUPPORT ========
    enum class SizeUnit { PX, PERCENT }
    private var widthValue: Float = 0f
    private var widthUnit: SizeUnit = SizeUnit.PX
    private var heightValue: Float = 0f
    private var heightUnit: SizeUnit = SizeUnit.PX

    init {
        setTouchable(Touchable.childrenOnly)
        createDefaultBackground()

        // Listen for changes that require layout recalculation
        addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                invalidateLayout()
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

    fun invalidateLayout() {
        layoutInvalidated = true
    }

    override fun setWidth(width: Float) {
        widthValue = width
        widthUnit = SizeUnit.PX
        super.setWidth(width)
        invalidateLayout()
    }

    override fun setHeight(height: Float) {
        heightValue = height
        heightUnit = SizeUnit.PX
        super.setHeight(height)
        invalidateLayout()
    }

    override fun setSize(width: Float, height: Float) {
        widthValue = width
        widthUnit = SizeUnit.PX
        heightValue = height
        heightUnit = SizeUnit.PX
        super.setSize(width, height)
        invalidateLayout()
    }

    fun setWidthPercent(percent: Float) {
        widthValue = percent
        widthUnit = SizeUnit.PERCENT
        invalidateLayout()
    }

    fun setHeightPercent(percent: Float) {
        heightValue = percent
        heightUnit = SizeUnit.PERCENT
        invalidateLayout()
    }

    fun setSizePercent(widthPercent: Float, heightPercent: Float) {
        widthValue = widthPercent
        widthUnit = SizeUnit.PERCENT
        heightValue = heightPercent
        heightUnit = SizeUnit.PERCENT
        invalidateLayout()
    }

    override fun act(delta: Float) {
        // === Calculate percent-based size if needed ===
        val parentWidth = (parent as? Group)?.width ?: Gdx.graphics.width.toFloat()
        val parentHeight = (parent as? Group)?.height ?: Gdx.graphics.height.toFloat()
        val resolvedWidth = if (widthUnit == SizeUnit.PERCENT) widthValue * parentWidth else widthValue
        val resolvedHeight = if (heightUnit == SizeUnit.PERCENT) heightValue * parentHeight else heightValue
        if (widthUnit == SizeUnit.PERCENT || heightUnit == SizeUnit.PERCENT) {
            super.setSize(resolvedWidth, resolvedHeight)
        }
        super.act(delta)
        if (layoutInvalidated) {
            calculateLayout()
            layoutInvalidated = false
        }
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

    private fun calculateLayout() {
        val width = getWidth() - paddingLeft - paddingRight
        val height = getHeight() - paddingTop - paddingBottom

        val visibleChildren = this.visibleChildren
        if (visibleChildren.size == 0) return

        if (flexWrap == FlexWrap.NO_WRAP) {
            layoutSingleLine(visibleChildren, width, height)
        } else {
            layoutMultiLine(visibleChildren, width, height)
        }
    }

    private fun layoutSingleLine(children: Array<Actor>, width: Float, height: Float) {
        // Calculate total main size and free space
        var totalMainSize = 0f
        var totalFlexGrow = 0f
        var maxCrossSize = 0f

        for (child in children) {
            val props = getProps(child)
            val childMainSize = getMainAxisSize(child, props!!)
            val childCrossSize = getCrossAxisSize(child, props, width, height)

            totalMainSize += childMainSize + props.marginMainStart(this.isRow) + props.marginMainEnd(this.isRow)
            totalFlexGrow += props.flexGrow

            val effectiveCrossSize = childCrossSize + props.marginCrossStart(this.isRow) + props.marginCrossEnd(
                this.isRow
            )
            if (effectiveCrossSize > maxCrossSize) maxCrossSize = effectiveCrossSize
        }

        // Add gaps between items
        totalMainSize += gap * (children.size - 1)
        val freeSpace = getMainAxisSize(width, height) - totalMainSize

        // Apply justify content to free space
        val mainOffset = if (flexJustify == FlexJustify.CENTER) {
            (getMainAxisSize(width, height) - totalMainSize) / 2f
        } else {
            calculateMainOffset(freeSpace, children.size)
        }

        // Position children
        var mainPos = mainOffset
        for (child in children) {
            val props = getProps(child)

            // Calculate child size
            var childMainSize = getMainAxisSize(child, props!!)
            val childCrossSize = getCrossAxisSize(child, props, width, height)

            // Apply flex grow to free space
            if (freeSpace > 0 && props.flexGrow > 0) {
                childMainSize += freeSpace * (props.flexGrow / totalFlexGrow)
            }

            // Calculate cross axis position
            val crossPos = calculateCrossPosition(child, props, maxCrossSize, height, width)

            // Set child bounds
            if (this.isRow) {
                child.setBounds(
                    getX() + paddingLeft + mainPos + props.marginLeft,
                    getY() + paddingBottom + crossPos + props.marginBottom,
                    childMainSize,
                    childCrossSize
                )
            } else {
                child.setBounds(
                    getX() + paddingLeft + crossPos + props.marginLeft,
                    getY() + paddingBottom + mainPos + props.marginBottom,
                    childCrossSize,
                    childMainSize
                )
            }

            // Update position for next child
            mainPos += childMainSize + props.marginMainStart(this.isRow) + props.marginMainEnd(this.isRow) + gap
        }
    }

    private fun layoutMultiLine(children: Array<Actor>, width: Float, height: Float) {
        // Simplified multi-line implementation
        val maxLineSize = if (this.isRow) width else height
        var currentLineSize = 0f
        var lineStartIndex = 0
        var lineCrossSize = 0f
        var totalCrossSize = 0f

        for (i in 0..children.size) {
            val isLast = i == children.size
            val child: Actor = (if (isLast) null else children.get(i))!!
            val props = if (isLast) null else getProps(child)

            var childMainSize = 0f
            var childCrossSize = 0f

            if (!isLast) {
                childMainSize = getMainAxisSize(child, props!!) +
                        props.marginMainStart(this.isRow) +
                        props.marginMainEnd(this.isRow)
                childCrossSize = getCrossAxisSize(child, props, width, height) +
                        props.marginCrossStart(this.isRow) +
                        props.marginCrossEnd(this.isRow)
            }

            // Check if we need to wrap
            if (isLast || (currentLineSize > 0 &&
                        currentLineSize + childMainSize + (if (i - lineStartIndex > 0) gap else 0f) > maxLineSize)
            ) {
                // Layout current line

                val line = com.badlogic.gdx.utils.Array<Actor>()
                for (j in lineStartIndex..<i) {
                    line.add(children.get(j))
                }

                if (line.size > 0) {
                    // Position line
                    var linePos = 0f
                    for (lineChild in line) {
                        val childProps = getProps(lineChild)

                        if (this.isRow) {
                            lineChild.setPosition(
                                getX() + paddingLeft + linePos + childProps!!.marginLeft,
                                getY() + paddingBottom + totalCrossSize + childProps.marginBottom
                            )
                        } else {
                            lineChild.setPosition(
                                getX() + paddingLeft + totalCrossSize + childProps!!.marginLeft,
                                getY() + paddingBottom + linePos + childProps.marginBottom
                            )
                        }

                        linePos += getMainAxisSize(lineChild, childProps) +
                                childProps.marginMainStart(this.isRow) +
                                childProps.marginMainEnd(this.isRow) +
                                gap
                    }

                    // Update cross size
                    totalCrossSize += lineCrossSize + gap
                    lineCrossSize = 0f
                }

                // Reset for next line
                lineStartIndex = i
                currentLineSize = 0f
            }

            if (!isLast) {
                // Add child to current line
                currentLineSize += childMainSize + (if (i > lineStartIndex) gap else 0f)
                if (childCrossSize > lineCrossSize) lineCrossSize = childCrossSize
            }
        }
    }

    private fun calculateMainOffset(freeSpace: Float, childCount: Int): Float {
        when (flexJustify) {
            FlexJustify.FLEX_START -> return 0f
            FlexJustify.FLEX_END -> return freeSpace
            FlexJustify.CENTER -> return freeSpace / 2
            FlexJustify.SPACE_BETWEEN -> return 0f // Handled by distributing space
            FlexJustify.SPACE_AROUND -> return freeSpace / (childCount * 2)
            FlexJustify.SPACE_EVENLY -> return freeSpace / (childCount + 1)
            else -> return 0f
        }
    }

    private fun calculateCrossPosition(
        child: Actor,
        props: FlexItemProps,
        maxCrossSize: Float,
        height: Float,
        width: Float
    ): Float {
        val crossSize = getCrossAxisSize(child, props, width, height)
        val availableSpace = if (this.isRow) height else width

        when (flexAlign) {
            FlexAlign.FLEX_START -> return props.marginCrossStart(this.isRow)
            FlexAlign.FLEX_END -> return availableSpace - crossSize - props.marginCrossEnd(this.isRow)
            FlexAlign.CENTER -> return (availableSpace - crossSize) / 2
            FlexAlign.STRETCH -> return props.marginCrossStart(this.isRow)
            FlexAlign.BASELINE ->                 // Simplified baseline alignment
                return props.marginCrossStart(this.isRow) + (maxCrossSize - crossSize) / 2

        }
    }

    private val visibleChildren: Array<Actor>
        // ======== HELPER METHODS ========
        get() {
            val visible : Array<Actor> = emptyArray()
            for (child in getChildren()) {
                if (child.isVisible) visible.fill(child)
            }
            return visible
        }

    private fun getMainAxisSize(child: Actor, props: FlexItemProps): Float {
        if (props.flexBasis > 0) return props.flexBasis
        return if (this.isRow) child.getWidth() else child.getHeight()
    }

    private fun getCrossAxisSize(child: Actor, props: FlexItemProps, width: Float, height: Float): Float {
        if (flexAlign == FlexAlign.STRETCH) {
            return if (this.isRow) height - props.marginTop - props.marginBottom else width - props.marginLeft - props.marginRight
        }
        return if (this.isRow) child.getHeight() else child.getWidth()
    }

    private fun getMainAxisSize(width: Float, height: Float): Float {
        return if (this.isRow) width else height
    }

    private val isRow: Boolean
        get() = flexDirection == FlexDirection.ROW || flexDirection == FlexDirection.ROW_REVERSE

    // ======== PROPERTY SETTERS ========
    fun setDirection(flexDirection: FlexDirection?) {
        this.flexDirection = flexDirection
        invalidateLayout()
    }

    fun setJustify(flexJustify: FlexJustify) {
        this.flexJustify = flexJustify
        invalidateLayout()
    }

    fun setAlign(flexAlign: FlexAlign) {
        this.flexAlign = flexAlign
        invalidateLayout()
    }

    fun setWrap(flexWrap: FlexWrap?) {
        this.flexWrap = flexWrap
        invalidateLayout()
    }

    fun setGap(gap: Float) {
        this.gap = gap
        invalidateLayout()
    }

    fun setPadding(padding: Float) {
        setPadding(padding, padding, padding, padding)
    }

    fun setPadding(top: Float, right: Float, bottom: Float, left: Float) {
        this.paddingTop = top
        this.paddingRight = right
        this.paddingBottom = bottom
        this.paddingLeft = left
        invalidateLayout()
    }

    fun setBackgroundColor(color: Color?) {
        this.backgroundColor = color
    }

    override fun addActor(actor: Actor?) {
        super.addActor(actor)
        invalidateLayout()
    }

    override fun removeActor(actor: Actor?): Boolean {
        val result = super.removeActor(actor)
        if (result) invalidateLayout()
        return result
    }
}
