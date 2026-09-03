package com.rimor.minipape

import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Red pulled one way, blue the other, by an amount that ripples down the frame.
 *
 * Plain arithmetic on pixels so it can be baked into a saved file. Every sample comes from the
 * same row, so a frame can be filtered a row at a time and a long GIF never needs more than one
 * extra row in memory. [ChromaticSurface] previews the same numbers on the GPU.
 */
object ChromaticAberration {
    /**
     * Separation in canvas pixels, before the ripple.
     *
     * Roughly one and a half percent of the canvas width. The figure the theme shader used —
     * three and a half pixels — was tuned against a full phone screen and is close to invisible
     * across 948, which is the whole reason this looked like it was doing nothing.
     */
    const val SEPARATION = 14f

    /** How much the separation breathes as it travels down the frame. */
    const val RIPPLE = 5.6f

    /** How tightly the ripple coils, per canvas pixel of height. */
    const val WAVE = 0.018f

    /** Advances the ripple frame by frame, so motion shimmers rather than sitting still. */
    fun phase(frame: Int): Float = frame * 0.35f

    fun filterRow(source: IntArray, destination: IntArray, y: Int, phase: Float) {
        val width = source.size
        val shift = SEPARATION + sin(y * WAVE + phase * 1.7f) * RIPPLE
        val step = shift.roundToInt()
        for (x in 0 until width) {
            val red = source[(x + step).coerceIn(0, width - 1)] and 0x00FF0000
            val green = source[x] and 0x0000FF00
            val blue = source[(x - step).coerceIn(0, width - 1)] and 0x000000FF
            destination[x] = (0xFF shl 24) or red or green or blue
        }
    }
}
