package com.rimor.minipape

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The GIF writer is hand-rolled, so it is checked by decoding what it produces with an
 * independent reader rather than by looking at it.
 */
class GifWriterTest {
    private val width = 48
    private val height = 32
    private val frames = 3

    private fun colourAt(frame: Int, x: Int, y: Int): Int {
        val red = x * 255 / (width - 1)
        val green = y * 255 / (height - 1)
        val blue = frame * 90
        return (red shl 16) or (green shl 8) or blue
    }

    private fun encode(loop: Boolean): ByteArray {
        val samples = LongArray(width * height * frames)
        var count = 0
        for (frame in 0 until frames) {
            for (y in 0 until height) {
                for (x in 0 until width) samples[count++] = colourAt(frame, x, y).toLong()
            }
        }
        val palette = GifPalette.build(samples, count)

        val bytes = ByteArrayOutputStream()
        val writer = GifWriter(bytes, width, height, palette.colours, loop)
        val indices = ByteArray(width * height)
        for (frame in 0 until frames) {
            var cursor = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    indices[cursor++] = palette.index(colourAt(frame, x, y)).toByte()
                }
            }
            writer.writeFrame(indices, 8)
        }
        writer.finish()
        return bytes.toByteArray()
    }

    @Test
    fun writesEveryFrameAtTheRequestedSize() {
        val bytes = encode(loop = true)
        val reader = ImageIO.getImageReadersByFormatName("gif").next()
        reader.input = ImageIO.createImageInputStream(ByteArrayInputStream(bytes))

        assertEquals(frames, reader.getNumImages(true))
        for (frame in 0 until frames) {
            val image = reader.read(frame)
            assertEquals(width, image.width)
            assertEquals(height, image.height)
        }
    }

    @Test
    fun pixelsSurviveTheRoundTrip() {
        val bytes = encode(loop = true)
        val reader = ImageIO.getImageReadersByFormatName("gif").next()
        reader.input = ImageIO.createImageInputStream(ByteArrayInputStream(bytes))

        for (frame in 0 until frames) {
            val image = reader.read(frame)
            for (y in 0 until height step 7) {
                for (x in 0 until width step 5) {
                    val expected = colourAt(frame, x, y)
                    val actual = image.getRGB(x, y) and 0xFFFFFF
                    for (shift in intArrayOf(16, 8, 0)) {
                        val difference = abs(((expected shr shift) and 0xFF) - ((actual shr shift) and 0xFF))
                        assertTrue(
                            "frame $frame at $x,$y channel $shift drifted by $difference",
                            difference <= 24,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun loopingIsWhatDecidesTheNetscapeBlock() {
        val marker = "NETSCAPE2.0".toByteArray(Charsets.US_ASCII)
        assertTrue(contains(encode(loop = true), marker))
        assertFalse(contains(encode(loop = false), marker))
    }

    private fun contains(haystack: ByteArray, needle: ByteArray): Boolean =
        (0..haystack.size - needle.size).any { start ->
            needle.indices.all { haystack[start + it] == needle[it] }
        }
}
