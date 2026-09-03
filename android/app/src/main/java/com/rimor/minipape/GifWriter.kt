package com.rimor.minipape

import java.io.OutputStream

/**
 * A GIF89a writer with one global palette.
 *
 * Frames are handed over as palette indices and compressed straight to the stream, so a long
 * clip never has to be held in memory all at once. Looping is written as the Netscape
 * application block; leaving it out is what makes a GIF stop on its last frame.
 */
class GifWriter(
    private val out: OutputStream,
    private val width: Int,
    private val height: Int,
    private val palette: IntArray,
    private val loop: Boolean,
) {
    private var started = false

    fun writeFrame(indices: ByteArray, delayCentiseconds: Int) {
        if (!started) {
            header()
            started = true
        }
        graphicControl(delayCentiseconds)
        imageDescriptor()
        LzwEncoder(indices, out).encode()
    }

    fun finish() {
        if (!started) return
        out.write(0x3B)
        out.flush()
    }

    private fun header() {
        out.write("GIF89a".toByteArray(Charsets.US_ASCII))
        writeShort(width)
        writeShort(height)
        out.write(0xF7) // global colour table, 256 entries, 8 bits per channel
        out.write(0) // background index
        out.write(0) // pixel aspect ratio
        for (index in 0 until 256) {
            val colour = palette.getOrElse(index) { 0 }
            out.write((colour shr 16) and 0xFF)
            out.write((colour shr 8) and 0xFF)
            out.write(colour and 0xFF)
        }
        if (loop) {
            out.write(0x21)
            out.write(0xFF)
            out.write(0x0B)
            out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
            out.write(0x03)
            out.write(0x01)
            writeShort(0) // repeat forever
            out.write(0)
        }
    }

    private fun graphicControl(delayCentiseconds: Int) {
        out.write(0x21)
        out.write(0xF9)
        out.write(0x04)
        out.write(0x04) // disposal: leave the frame in place, no transparency
        writeShort(delayCentiseconds)
        out.write(0) // transparent colour index
        out.write(0)
    }

    private fun imageDescriptor() {
        out.write(0x2C)
        writeShort(0)
        writeShort(0)
        writeShort(width)
        writeShort(height)
        out.write(0) // no local colour table, not interlaced
    }

    private fun writeShort(value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
    }
}

/**
 * Variable-code-width LZW as GIF specifies it, with the 5003-slot open-addressed string table
 * the format has been encoded with since it was written. A hash table this size resets cheaply,
 * which matters: on photographic frames the table fills and clears many times per frame.
 */
private class LzwEncoder(private val pixels: ByteArray, private val out: OutputStream) {
    private val bits = 12
    private val maxMaxCode = 1 shl bits
    private val hashSize = 5003
    private val hashTable = IntArray(hashSize)
    private val codeTable = IntArray(hashSize)
    private val accumulator = ByteArray(256)
    private val masks = intArrayOf(
        0x0000, 0x0001, 0x0003, 0x0007, 0x000F, 0x001F, 0x003F, 0x007F,
        0x00FF, 0x01FF, 0x03FF, 0x07FF, 0x0FFF, 0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF,
    )

    private var cursor = 0
    private var codeWidth = 0
    private var maxCode = 0
    private var freeEntry = 0
    private var clearRequested = false
    private var initialWidth = 0
    private var clearCode = 0
    private var endCode = 0
    private var accumulated = 0
    private var accumulatedBits = 0
    private var accumulatorCount = 0

    fun encode() {
        out.write(INITIAL_CODE_SIZE)
        cursor = 0
        compress(INITIAL_CODE_SIZE + 1)
        out.write(0) // block terminator
    }

    private fun compress(initialCodeWidth: Int) {
        initialWidth = initialCodeWidth
        clearRequested = false
        codeWidth = initialWidth
        maxCode = maxCodeFor(codeWidth)
        clearCode = 1 shl (initialCodeWidth - 1)
        endCode = clearCode + 1
        freeEntry = clearCode + 2
        accumulatorCount = 0

        var current = nextPixel()
        var shift = 0
        var probe = hashSize
        while (probe < 65536) {
            shift++
            probe *= 2
        }
        shift = 8 - shift

        clearHash()
        output(clearCode)

        outer@ while (true) {
            val pixel = nextPixel()
            if (pixel == END_OF_PIXELS) break
            val entry = (pixel shl bits) + current
            var slot = (pixel shl shift) xor current
            if (hashTable[slot] == entry) {
                current = codeTable[slot]
                continue
            }
            if (hashTable[slot] >= 0) {
                var step = hashSize - slot
                if (slot == 0) step = 1
                do {
                    slot -= step
                    if (slot < 0) slot += hashSize
                    if (hashTable[slot] == entry) {
                        current = codeTable[slot]
                        continue@outer
                    }
                } while (hashTable[slot] >= 0)
            }
            output(current)
            current = pixel
            if (freeEntry < maxMaxCode) {
                codeTable[slot] = freeEntry++
                hashTable[slot] = entry
            } else {
                clearHash()
                freeEntry = clearCode + 2
                clearRequested = true
                output(clearCode)
            }
        }
        output(current)
        output(endCode)
    }

    private fun output(code: Int) {
        accumulated = accumulated and masks[accumulatedBits]
        accumulated = if (accumulatedBits > 0) accumulated or (code shl accumulatedBits) else code
        accumulatedBits += codeWidth
        while (accumulatedBits >= 8) {
            writeByte((accumulated and 0xFF).toByte())
            accumulated = accumulated shr 8
            accumulatedBits -= 8
        }

        if (freeEntry > maxCode || clearRequested) {
            if (clearRequested) {
                codeWidth = initialWidth
                maxCode = maxCodeFor(codeWidth)
                clearRequested = false
            } else {
                codeWidth++
                maxCode = if (codeWidth == bits) maxMaxCode else maxCodeFor(codeWidth)
            }
        }

        if (code == endCode) {
            while (accumulatedBits > 0) {
                writeByte((accumulated and 0xFF).toByte())
                accumulated = accumulated shr 8
                accumulatedBits -= 8
            }
            flushBlock()
        }
    }

    private fun writeByte(value: Byte) {
        accumulator[accumulatorCount++] = value
        if (accumulatorCount >= 254) flushBlock()
    }

    private fun flushBlock() {
        if (accumulatorCount == 0) return
        out.write(accumulatorCount)
        out.write(accumulator, 0, accumulatorCount)
        accumulatorCount = 0
    }

    private fun clearHash() = hashTable.fill(-1)

    private fun maxCodeFor(width: Int) = (1 shl width) - 1

    private fun nextPixel(): Int =
        if (cursor == pixels.size) END_OF_PIXELS else pixels[cursor++].toInt() and 0xFF

    private companion object {
        const val INITIAL_CODE_SIZE = 8
        const val END_OF_PIXELS = -1
    }
}
