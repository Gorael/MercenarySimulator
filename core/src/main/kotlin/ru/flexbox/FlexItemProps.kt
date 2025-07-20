package ru.flexbox

class FlexItemProps {
    // Flex properties
    @JvmField
    var flexGrow: Float = 0f
    @JvmField
    var flexShrink: Float = 1f
    @JvmField
    var flexBasis: Float = -1f // -1 = auto
    @JvmField
    var flexAlignSelf: FlexAlign? = null

    // ======== SIZE UNIT SUPPORT ========
    enum class SizeUnit { PX, PERCENT }
    @JvmField
    var widthValue: Float = -1f
    @JvmField
    var widthUnit: SizeUnit = SizeUnit.PX
    @JvmField
    var heightValue: Float = -1f
    @JvmField
    var heightUnit: SizeUnit = SizeUnit.PX

    // Spacing
    @JvmField
    var marginTop: Float = 0f
    @JvmField
    var marginRight: Float = 0f
    @JvmField
    var marginBottom: Float = 0f
    @JvmField
    var marginLeft: Float = 0f

    @JvmField
    var paddingTop: Float = 0f
    @JvmField
    var paddingRight: Float = 0f
    @JvmField
    var paddingBottom: Float = 0f
    @JvmField
    var paddingLeft: Float = 0f

    fun marginMainStart(isRow: Boolean): Float {
        return if (isRow) marginLeft else marginBottom
    }

    fun marginMainEnd(isRow: Boolean): Float {
        return if (isRow) marginRight else marginTop
    }

    fun marginCrossStart(isRow: Boolean): Float {
        return if (isRow) marginBottom else marginLeft
    }

    fun marginCrossEnd(isRow: Boolean): Float {
        return if (isRow) marginTop else marginRight
    }
}
