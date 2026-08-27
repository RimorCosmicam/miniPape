package com.rimor.minipape

import java.io.File

data class CropRecipe(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f,
    val muted: Boolean = true,
    val loop: Boolean = true,
)

data class PreviewSession(
    val source: File? = null,
    val mediaKind: String = "image",
    val recipe: CropRecipe = CropRecipe(),
    val playhead: Double = 0.0,
    val playing: Boolean = true,
)

data class WallpaperItem(
    val file: File,
    val displayName: String,
    val mediaKind: String,
    val recipe: CropRecipe = CropRecipe(),
)
