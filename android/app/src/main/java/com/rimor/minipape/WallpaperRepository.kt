package com.rimor.minipape

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

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

    fun addWallpaper(file: File, kind: String) {
        _wallpapers.value = listOf(WallpaperItem(file, file.nameWithoutExtension, kind)) + _wallpapers.value
    }

    private fun loadWallpapers(): List<WallpaperItem> = wallpaperDirectory.listFiles()
        ?.sortedByDescending(File::lastModified)
        ?.map { WallpaperItem(it, it.nameWithoutExtension, mediaKind(it.extension)) }
        .orEmpty()

    private fun mediaKind(extension: String): String = when (extension.lowercase()) {
        "mp4", "mov", "m4v", "webm" -> "video"
        "gif", "apng" -> "animatedImage"
        else -> "image"
    }
}

private fun String.safeExtension(): String = lowercase().filter(Char::isLetterOrDigit).take(8).ifBlank { "bin" }

