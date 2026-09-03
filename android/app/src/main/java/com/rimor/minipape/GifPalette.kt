package com.rimor.minipape

import java.util.Arrays

/**
 * A 256-colour global palette built by median cut, with a 15-bit nearest-colour cache in front
 * of it. Colours arrive as 0xRRGGBB.
 */
class GifPalette private constructor(val colours: IntArray) {
    private val cache = ByteArray(1 shl 15)
    private val resolved = BooleanArray(1 shl 15)

    fun index(colour: Int): Int {
        val key = (((colour shr 19) and 0x1F) shl 10) or
            (((colour shr 11) and 0x1F) shl 5) or
            ((colour shr 3) and 0x1F)
        if (!resolved[key]) {
            cache[key] = nearest(key).toByte()
            resolved[key] = true
        }
        return cache[key].toInt() and 0xFF
    }

    private fun nearest(key: Int): Int {
        val r = (((key shr 10) and 0x1F) shl 3) or 4
        val g = (((key shr 5) and 0x1F) shl 3) or 4
        val b = ((key and 0x1F) shl 3) or 4
        var best = 0
        var bestDistance = Int.MAX_VALUE
        for (index in colours.indices) {
            val colour = colours[index]
            val dr = r - ((colour shr 16) and 0xFF)
            val dg = g - ((colour shr 8) and 0xFF)
            val db = b - (colour and 0xFF)
            val distance = dr * dr + dg * dg + db * db
            if (distance < bestDistance) {
                bestDistance = distance
                best = index
                if (distance == 0) break
            }
        }
        return best
    }

    companion object {
        const val MAX_COLOURS = 256

        /**
         * Median cut: repeatedly take the box with the widest spread on any one channel, sort it
         * on that channel and halve it at the median. The average of each surviving box is a
         * palette entry.
         *
         * [samples] holds packed 0xRRGGBB values in its low 24 bits; the range [0, [size]) is
         * reordered in place.
         */
        fun build(samples: LongArray, size: Int): GifPalette {
            if (size <= 0) return GifPalette(intArrayOf(0x000000))
            val boxes = ArrayList<Box>()
            boxes += Box(samples, 0, size)
            while (boxes.size < MAX_COLOURS) {
                var target: Box? = null
                for (box in boxes) {
                    if (box.count > 1 && box.extent > 0 && (target == null || box.extent > target.extent)) {
                        target = box
                    }
                }
                val chosen = target ?: break
                sortRange(samples, chosen.from, chosen.to, chosen.widestChannel)
                val middle = chosen.from + chosen.count / 2
                boxes.remove(chosen)
                boxes += Box(samples, chosen.from, middle)
                boxes += Box(samples, middle, chosen.to)
            }
            return GifPalette(IntArray(boxes.size) { boxes[it].average() })
        }

        private fun sortRange(samples: LongArray, from: Int, to: Int, channel: Int) {
            val shift = when (channel) {
                0 -> 16
                1 -> 8
                else -> 0
            }
            for (index in from until to) {
                val colour = samples[index] and 0xFFFFFF
                samples[index] = (((colour shr shift) and 0xFF) shl 32) or colour
            }
            Arrays.sort(samples, from, to)
            for (index in from until to) samples[index] = samples[index] and 0xFFFFFF
        }
    }

    private class Box(samples: LongArray, val from: Int, val to: Int) {
        val count = to - from
        val widestChannel: Int
        val extent: Int
        private val red: Int
        private val green: Int
        private val blue: Int

        init {
            var rMin = 255
            var rMax = 0
            var gMin = 255
            var gMax = 0
            var bMin = 255
            var bMax = 0
            var rSum = 0L
            var gSum = 0L
            var bSum = 0L
            for (index in from until to) {
                val colour = samples[index].toInt()
                val r = (colour shr 16) and 0xFF
                val g = (colour shr 8) and 0xFF
                val b = colour and 0xFF
                if (r < rMin) rMin = r
                if (r > rMax) rMax = r
                if (g < gMin) gMin = g
                if (g > gMax) gMax = g
                if (b < bMin) bMin = b
                if (b > bMax) bMax = b
                rSum += r
                gSum += g
                bSum += b
            }
            val divisor = if (count > 0) count else 1
            red = (rSum / divisor).toInt()
            green = (gSum / divisor).toInt()
            blue = (bSum / divisor).toInt()
            val rSpread = rMax - rMin
            val gSpread = gMax - gMin
            val bSpread = bMax - bMin
            extent = maxOf(rSpread, gSpread, bSpread)
            widestChannel = when (extent) {
                rSpread -> 0
                gSpread -> 1
                else -> 2
            }
        }

        fun average(): Int = (red shl 16) or (green shl 8) or blue
    }
}
