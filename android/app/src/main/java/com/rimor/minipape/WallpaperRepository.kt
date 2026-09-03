package com.rimor.minipape

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import org.json.JSONObject
import org.json.JSONArray

class WallpaperRepository(context: Context) {
    private val wallpaperDirectory = File(context.filesDir, "wallpapers").apply { mkdirs() }
    private val _preview = MutableStateFlow(latestWallpaper())

    /** What the cover activity shows: the most recent cut, still there after a restart. */
    val preview = _preview.asStateFlow()

    /** Where finished cuts are kept. */
    val outputDirectory: File get() = wallpaperDirectory

    /**
     * Puts a finished cut on the cover. The file is already the size and shape of the canvas, so
     * the only part of the recipe still worth carrying is whether it loops.
     */
    fun setCutWallpaper(file: File, kind: String, loop: Boolean) {
        val recipe = CropRecipe(loop = loop)
        recipeFile(file).writeText(recipe.toJson().toString())
        _preview.value = PreviewSession(source = file, mediaKind = kind, recipe = recipe)
    }

    private fun latestWallpaper(): PreviewSession {
        val file = wallpaperDirectory.listFiles()
            ?.filterNot { it.name.endsWith(RECIPE_SUFFIX) }
            ?.maxByOrNull(File::lastModified)
            ?: return PreviewSession()
        val recipe = recipeFile(file).takeIf(File::exists)?.readText()
            ?.let { runCatching { JSONObject(it).toCropRecipe() }.getOrNull() }
            ?: CropRecipe()
        return PreviewSession(source = file, mediaKind = mediaKind(file.extension), recipe = recipe)
    }

    private fun recipeFile(file: File): File = File(file.parentFile, "${file.name}$RECIPE_SUFFIX")

    private fun mediaKind(extension: String): String = when (extension.lowercase()) {
        "mp4", "mov", "m4v", "webm" -> "video"
        "gif", "apng" -> "animatedImage"
        else -> "image"
    }

    private companion object {
        const val RECIPE_SUFFIX = ".minipape.json"
    }
}

fun JSONObject.toCropRecipe(): CropRecipe = CropRecipe(
    scale = optDouble("scale", 1.0).toFloat(),
    offsetX = optDouble("offsetX", 0.0).toFloat(),
    offsetY = optDouble("offsetY", 0.0).toFloat(),
    rotation = optDouble("rotation", 0.0).toFloat(),
    muted = optBoolean("muted", true),
    loop = optBoolean("loop", true),
    trimStart = optDouble("trimStart", 0.0).toFloat(),
    trimEnd = optDouble("trimEnd", 1.0).toFloat(),
    filters = optJSONArray("filters")?.let { values ->
        buildList {
            repeat(values.length()) { index ->
                runCatching { ThemeFilter.valueOf(values.getString(index)) }.getOrNull()?.let(::add)
            }
        }
    }.orEmpty(),
)

private fun CropRecipe.toJson(): JSONObject = JSONObject()
    .put("scale", scale)
    .put("offsetX", offsetX)
    .put("offsetY", offsetY)
    .put("rotation", rotation)
    .put("muted", muted)
    .put("loop", loop)
    .put("trimStart", trimStart)
    .put("trimEnd", trimEnd)
    .put("filters", JSONArray(filters.map(ThemeFilter::name)))
