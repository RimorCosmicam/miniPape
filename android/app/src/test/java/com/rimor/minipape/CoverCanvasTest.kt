package com.rimor.minipape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The framing limits exist for one reason: at full travel the media must still cover the canvas,
 * on both axes, for any shape of source. So that is what is asserted.
 */
class CoverCanvasTest {
    private val aspects = listOf(
        0.2f, 0.4615f, 0.5f, 0.75f, 0.8f, CoverCanvas.RATIO, 1f, 1.3333f, 1.5f, 1.7778f, 2.35f, 5f,
    )
    private val zooms = listOf(1f, 1.01f, 1.5f, 2f, 3f, 4f)
    private val slack = 0.05f

    @Test
    fun mediaCoversTheCanvasAtEveryCorner() {
        for (aspect in aspects) {
            for (zoom in zooms) {
                val x = CoverCanvas.horizontalLimit(aspect, zoom)
                val y = CoverCanvas.verticalLimit(aspect, zoom)
                for (offsetX in listOf(-x, 0f, x)) {
                    for (offsetY in listOf(-y, 0f, y)) {
                        val placed = CoverCanvas.place(aspect, zoom, offsetX, offsetY)
                        val where = "aspect $aspect zoom $zoom offset $offsetX,$offsetY -> $placed"
                        assertTrue("left edge came inside: $where", placed.left <= slack)
                        assertTrue("top edge came inside: $where", placed.top <= slack)
                        assertTrue("right edge came inside: $where", placed.right >= CoverCanvas.WIDTH - slack)
                        assertTrue("bottom edge came inside: $where", placed.bottom >= CoverCanvas.HEIGHT - slack)
                    }
                }
            }
        }
    }

    @Test
    fun goingOneStepBeyondTheLimitWouldExposeTheGround() {
        // If the limits were merely generous the test above would pass and the bug would remain.
        for (aspect in aspects) {
            for (zoom in zooms) {
                val x = CoverCanvas.horizontalLimit(aspect, zoom)
                if (x > 0f) {
                    val placed = CoverCanvas.place(aspect, zoom, x + 0.02f, 0f)
                    assertTrue("limit was too small for aspect $aspect zoom $zoom", placed.left > 0f)
                }
                val y = CoverCanvas.verticalLimit(aspect, zoom)
                if (y > 0f) {
                    val placed = CoverCanvas.place(aspect, zoom, 0f, y + 0.02f)
                    assertTrue("limit was too small for aspect $aspect zoom $zoom", placed.top > 0f)
                }
            }
        }
    }

    @Test
    fun anUnmeasuredSourceIsPinnedUntilItIsZoomed() {
        assertEquals(0f, CoverCanvas.horizontalLimit(0f, 1f))
        assertEquals(0f, CoverCanvas.verticalLimit(0f, 1f))
        assertEquals(1f, CoverCanvas.horizontalLimit(0f, 2f))
    }

    @Test
    fun aSourceShapedLikeTheCanvasCannotMoveAtAll() {
        assertEquals(0f, CoverCanvas.horizontalLimit(CoverCanvas.RATIO, 1f))
        assertEquals(0f, CoverCanvas.verticalLimit(CoverCanvas.RATIO, 1f))
    }
}
