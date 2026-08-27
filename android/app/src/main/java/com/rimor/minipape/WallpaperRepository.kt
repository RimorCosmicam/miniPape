package com.rimor.minipape

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import org.json.JSONObject
import org.json.JSONArray

class WallpaperRepository(context: Context) {
    private val wallpaperDirectory = File(context.filesDir, "wallpapers").apply { mkdirs() }
    private val previewDirectory = File(context.cacheDir, "preview").apply { mkdirs() }
    private val _preview = MutableStateFlow(PreviewSession())
    private val _wallpapers = MutableStateFlow(loadWallpapers())

    val preview = _preview.asStateFlow()
    val wallpapers = _wallpapers.asStateFlow()

    fun previewFile(extension: String): File = File(previewDirectory, "current.${extension.safeExtension()}")

    fun setPreviewSource(file: File, kind: String) {
        previewDirectory.listFiles()?.filter { it != file }?.forEach(File::delete)
        _preview.value = PreviewSession(source = file, mediaKind = kind)
    }

    fun updatePreview(recipe: CropRecipe, playhead: Double, playing: Boolean) {
        _preview.value = _preview.value.copy(recipe = recipe, playhead = playhead, playing = playing)
    }

    fun wallpaperFile(name: String): File {
        val clean = name.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9._ -]"), "_")
            .take(100)
            .ifBlank { "wallpaper-${System.currentTimeMillis()}" }
        return File(wallpaperDirectory, clean)
    }

    fun addWallpaper(file: File, kind: String, recipe: CropRecipe = CropRecipe()) {
        recipeFile(file).writeText(recipe.toJson().toString())
        _wallpapers.value = listOf(WallpaperItem(file, file.nameWithoutExtension, kind, recipe)) + _wallpapers.value
    }

    private fun loadWallpapers(): List<WallpaperItem> = wallpaperDirectory.listFiles()
        ?.filterNot { it.name.endsWith(".minipape.json") }
        ?.sortedByDescending(File::lastModified)
        ?.map { file ->
            val recipe = recipeFile(file).takeIf(File::exists)?.readText()?.let { runCatching { JSONObject(it).toCropRecipe() }.getOrNull() }
                ?: CropRecipe()
            WallpaperItem(file, file.nameWithoutExtension, mediaKind(file.extension), recipe)
        }
        .orEmpty()

    private fun recipeFile(file: File): File = File(file.parentFile, "${file.name}.minipape.json")

    private fun mediaKind(extension: String): String = when (extension.lowercase()) {
        "mp4", "mov", "m4v", "webm" -> "video"
        "gif", "apng" -> "animatedImage"
        else -> "image"
    }
}

fun JSONObject.toCropRecipe(): CropRecipe = CropRecipe(
    scale = optDouble("scale", 1.0).toFloat(),
    offsetX = optDouble("offsetX", 0.0).toFloat(),
    offsetY = optDouble("offsetY", 0.0).toFloat(),
    rotation = optDouble("rotation", 0.0).toFloat(),
    muted = optBoolean("muted", true),
    loop = optBoolean("loop", true),
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
    .put("filters", JSONArray(filters.map(ThemeFilter::name)))

private fun String.safeExtension(): String = lowercase().filter(Char::isLetterOrDigit).take(8).ifBlank { "bin" }
