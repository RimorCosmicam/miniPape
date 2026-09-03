package com.rimor.minipape

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Movie
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Cuts the chosen media down to the cover canvas exactly as the editor framed it.
 *
 * The framing maths here is the inverse of what the preview draws, and both sides take it from
 * [CoverCanvas] so they cannot disagree. Stills come out as PNG; motion comes out as a GIF,
 * trimmed to the chosen span and either looping or stopping on its last frame.
 */
object MediaExport {
    const val CANVAS_WIDTH = CoverCanvas.WIDTH
    const val CANVAS_HEIGHT = CoverCanvas.HEIGHT

    /** 8 hundredths of a second a frame — 12.5fps, which most GIF viewers honour exactly. */
    private const val FRAME_DELAY_CENTISECONDS = 8
    private const val FRAME_STEP_MS = FRAME_DELAY_CENTISECONDS * 10
    private const val MAX_FRAMES = 40
    private const val PALETTE_FRAMES = 8
    private const val PALETTE_SAMPLE_BUDGET = 180_000

    data class Cut(val file: File, val mediaKind: String, val mimeType: String)

    fun cut(context: Context, uri: Uri, recipe: CropRecipe, directory: File): Cut {
        val mimeType = context.contentResolver.getType(uri).orEmpty().lowercase()
        val stamp = System.currentTimeMillis()
        val frames = when {
            mimeType.startsWith("video/") -> videoFrames(context, uri, recipe)
            mimeType == "image/gif" -> gifFrames(context, uri, recipe)
            else -> null
        }
        if (frames != null && frames.count > 1) {
            val target = File(directory, "minipape-$stamp.gif")
            try {
                writeGif(frames, recipe, target)
            } finally {
                frames.close()
            }
            return Cut(target, "animatedImage", "image/gif")
        }
        frames?.close()

        val source = decodeStill(context, uri) ?: error("That file could not be decoded")
        val canvas = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        drawCut(source, recipe, canvas)
        if (recipe.chromatic) aberrate(canvas, 0)
        val target = File(directory, "minipape-$stamp.png")
        BufferedOutputStream(target.outputStream()).use { canvas.compress(Bitmap.CompressFormat.PNG, 100, it) }
        source.recycle()
        canvas.recycle()
        return Cut(target, "image", "image/png")
    }

    /** Copies a finished cut into Pictures/miniPape and returns what a gallery can open. */
    fun publish(context: Context, cut: Cut): Uri? = runCatching {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, cut.file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, cut.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/miniPape")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { output -> cut.file.inputStream().use { it.copyTo(output) } }
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        uri
    }.getOrNull()

    // --- framing -------------------------------------------------------------------------

    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    private fun drawCut(source: Bitmap, recipe: CropRecipe, into: Bitmap) {
        val canvas = Canvas(into)
        canvas.drawColor(Color.BLACK)
        val cover = max(
            CANVAS_WIDTH.toFloat() / source.width,
            CANVAS_HEIGHT.toFloat() / source.height,
        )
        val zoom = cover * recipe.scale
        val matrix = Matrix()
        matrix.setScale(zoom, zoom)
        matrix.postTranslate(
            CANVAS_WIDTH / 2f - source.width * zoom / 2f,
            CANVAS_HEIGHT / 2f - source.height * zoom / 2f,
        )
        if (recipe.rotation != 0f) {
            matrix.postRotate(recipe.rotation, CANVAS_WIDTH / 2f, CANVAS_HEIGHT / 2f)
        }
        matrix.postTranslate(
            recipe.offsetX * CANVAS_WIDTH * 0.5f,
            recipe.offsetY * CANVAS_HEIGHT * 0.5f,
        )
        canvas.drawBitmap(source, matrix, paint)
    }

    private fun aberrate(bitmap: Bitmap, frame: Int) {
        val source = IntArray(CANVAS_WIDTH)
        val filtered = IntArray(CANVAS_WIDTH)
        val phase = ChromaticAberration.phase(frame)
        for (y in 0 until CANVAS_HEIGHT) {
            bitmap.getPixels(source, 0, CANVAS_WIDTH, 0, y, CANVAS_WIDTH, 1)
            ChromaticAberration.filterRow(source, filtered, y, phase)
            bitmap.setPixels(filtered, 0, CANVAS_WIDTH, 0, y, CANVAS_WIDTH, 1)
        }
    }

    // --- animation -----------------------------------------------------------------------

    private fun writeGif(frames: Frames, recipe: CropRecipe, target: File) {
        val cutFrame = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val row = IntArray(CANVAS_WIDTH)
        val filtered = IntArray(CANVAS_WIDTH)
        val palette = buildPalette(frames, recipe, cutFrame, row, filtered)
        val indices = ByteArray(CANVAS_WIDTH * CANVAS_HEIGHT)

        BufferedOutputStream(target.outputStream()).use { output ->
            val writer = GifWriter(output, CANVAS_WIDTH, CANVAS_HEIGHT, palette.colours, recipe.loop)
            for (index in 0 until frames.count) {
                val source = frames.frame(index) ?: continue
                drawCut(source, recipe, cutFrame)
                frames.release(source)
                var cursor = 0
                val phase = ChromaticAberration.phase(index)
                for (y in 0 until CANVAS_HEIGHT) {
                    cutFrame.getPixels(row, 0, CANVAS_WIDTH, 0, y, CANVAS_WIDTH, 1)
                    val pixels = if (recipe.chromatic) {
                        ChromaticAberration.filterRow(row, filtered, y, phase)
                        filtered
                    } else {
                        row
                    }
                    for (x in 0 until CANVAS_WIDTH) {
                        indices[cursor++] = palette.index(pixels[x] and 0xFFFFFF).toByte()
                    }
                }
                writer.writeFrame(indices, frames.delayCentiseconds)
            }
            writer.finish()
        }
        cutFrame.recycle()
    }

    private fun buildPalette(
        frames: Frames,
        recipe: CropRecipe,
        cutFrame: Bitmap,
        row: IntArray,
        filtered: IntArray,
    ): GifPalette {
        val passes = min(PALETTE_FRAMES, frames.count)
        val stride = max(1, CANVAS_WIDTH * CANVAS_HEIGHT * passes / PALETTE_SAMPLE_BUDGET)
        val samples = LongArray(CANVAS_WIDTH * CANVAS_HEIGHT * passes / stride + passes * CANVAS_HEIGHT)
        var count = 0
        for (pass in 0 until passes) {
            val index = if (passes == 1) 0 else pass * (frames.count - 1) / (passes - 1)
            val source = frames.frame(index) ?: continue
            drawCut(source, recipe, cutFrame)
            frames.release(source)
            val phase = ChromaticAberration.phase(index)
            for (y in 0 until CANVAS_HEIGHT) {
                cutFrame.getPixels(row, 0, CANVAS_WIDTH, 0, y, CANVAS_WIDTH, 1)
                val pixels = if (recipe.chromatic) {
                    ChromaticAberration.filterRow(row, filtered, y, phase)
                    filtered
                } else {
                    row
                }
                var x = (y * 7) % stride
                while (x < CANVAS_WIDTH && count < samples.size) {
                    samples[count++] = (pixels[x] and 0xFFFFFF).toLong()
                    x += stride
                }
            }
        }
        return GifPalette.build(samples, count)
    }

    private interface Frames {
        val count: Int
        val delayCentiseconds: Int
        fun frame(index: Int): Bitmap?
        fun release(bitmap: Bitmap) = Unit
        fun close()
    }

    /**
     * How the trimmed span is cut into frames. A short selection runs at 12.5fps; a long one
     * slows down rather than being silently truncated, so what you trimmed is what you get.
     */
    private class Pacing(durationMs: Long, recipe: CropRecipe) {
        val startMs: Long
        val stepMs: Long
        val count: Int
        val delayCentiseconds: Int

        init {
            val start = recipe.trimStart.coerceIn(0f, 1f)
            val end = recipe.trimEnd.coerceIn(start, 1f)
            startMs = (durationMs * start).toLong()
            val span = max(1L, (durationMs * (end - start)).toLong())
            stepMs = max(FRAME_STEP_MS.toLong(), span / MAX_FRAMES)
            count = min(MAX_FRAMES, max(1, ceil(span.toDouble() / stepMs).toInt()))
            delayCentiseconds = max(1, (stepMs / 10.0).roundToInt())
        }
    }

    private fun videoFrames(context: Context, uri: Uri, recipe: CropRecipe): Frames? = runCatching {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L
        if (duration <= 0L) {
            retriever.release()
            return@runCatching null
        }
        val pacing = Pacing(duration, recipe)
        object : Frames {
            override val count = pacing.count
            override val delayCentiseconds = pacing.delayCentiseconds
            override fun frame(index: Int): Bitmap? = runCatching {
                retriever.getFrameAtTime(
                    (pacing.startMs + index * pacing.stepMs) * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST,
                )
            }.getOrNull()

            override fun release(bitmap: Bitmap) = bitmap.recycle()
            override fun close() {
                runCatching { retriever.release() }
            }
        }
    }.getOrNull()

    @Suppress("DEPRECATION")
    private fun gifFrames(context: Context, uri: Uri, recipe: CropRecipe): Frames? = runCatching {
        val movie = context.contentResolver.openInputStream(uri)?.use { stream ->
            Movie.decodeStream(BufferedInputStream(stream))
        } ?: return@runCatching null
        val duration = movie.duration()
        if (duration <= 0 || movie.width() <= 0 || movie.height() <= 0) return@runCatching null
        val pacing = Pacing(duration.toLong(), recipe)
        val canvasBitmap = Bitmap.createBitmap(movie.width(), movie.height(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBitmap)
        object : Frames {
            override val count = pacing.count
            override val delayCentiseconds = pacing.delayCentiseconds
            override fun frame(index: Int): Bitmap {
                canvas.drawColor(Color.BLACK)
                movie.setTime(min(duration - 1, (pacing.startMs + index * pacing.stepMs).toInt()))
                movie.draw(canvas, 0f, 0f)
                return canvasBitmap
            }

            override fun close() = canvasBitmap.recycle()
        }
    }.getOrNull()

    private fun decodeStill(context: Context, uri: Uri): Bitmap? = runCatching {
        ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(context.contentResolver, uri),
        ) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
            val longest = max(info.size.width, info.size.height)
            if (longest > 4096) decoder.setTargetSampleSize(longest / 4096 + 1)
        }
    }.getOrNull()
}

/** Chromatic aberration is the one theme filter the exporter can bake into a file. */
val CropRecipe.chromatic: Boolean get() = ThemeFilter.CHROMATIC in filters
