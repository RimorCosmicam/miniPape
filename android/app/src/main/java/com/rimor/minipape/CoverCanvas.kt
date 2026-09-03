package com.rimor.minipape

import kotlin.math.max

/**
 * The cover canvas, and where framed media is allowed to sit on it.
 *
 * Pure arithmetic on purpose: the editor and the exporter both frame from here, so they cannot
 * drift apart, and the whole thing can be checked on the JVM without a device.
 */
object CoverCanvas {
    const val WIDTH = 948
    const val HEIGHT = 1048
    const val RATIO = WIDTH.toFloat() / HEIGHT

    const val MIN_ZOOM = 1f
    const val MAX_ZOOM = 4f

    /** Where the source lands on the canvas, in canvas pixels. */
    data class Placement(val left: Float, val top: Float, val right: Float, val bottom: Float)

    /**
     * How much wider than the canvas the source becomes once it has been scaled to cover it, and
     * likewise how much taller. Covering leaves slack on one axis and none on the other.
     */
    fun coverX(aspect: Float): Float = if (aspect > 0f) max(1f, aspect / RATIO) else 1f

    fun coverY(aspect: Float): Float = if (aspect > 0f) max(1f, RATIO / aspect) else 1f

    /**
     * How far the media may travel, as a fraction of half the canvas, before its own edge would
     * come inside the frame. Zooming adds slack to both axes; an unmeasured source falls back to
     * the tighter of the two, which is the zoom alone.
     */
    fun horizontalLimit(aspect: Float, zoom: Float): Float =
        (coverX(aspect) * zoom - 1f).coerceAtLeast(0f)

    fun verticalLimit(aspect: Float, zoom: Float): Float =
        (coverY(aspect) * zoom - 1f).coerceAtLeast(0f)

    /** Where a source of this shape ends up, given the framing. */
    fun place(aspect: Float, zoom: Float, offsetX: Float, offsetY: Float): Placement {
        val shape = if (aspect > 0f) aspect else RATIO
        // A source one unit tall and [shape] wide, scaled until it covers the canvas.
        val cover = max(WIDTH / shape, HEIGHT.toFloat())
        val drawnWidth = shape * cover * zoom
        val drawnHeight = cover * zoom
        val left = WIDTH / 2f - drawnWidth / 2f + offsetX * WIDTH * 0.5f
        val top = HEIGHT / 2f - drawnHeight / 2f + offsetY * HEIGHT * 0.5f
        return Placement(left, top, left + drawnWidth, top + drawnHeight)
    }
}
