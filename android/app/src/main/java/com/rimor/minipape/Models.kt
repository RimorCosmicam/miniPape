package com.rimor.minipape

import java.io.File

enum class ThemeFilter(val label: String, val description: String, val mode: Float) {
    CHROMATIC("Chromatic", "RGB lens separation and subtle edge distortion", 1f),
    CRT("CRT", "Scanlines, phosphor shimmer, and curved-screen vignette", 2f),
    VHS("VHS", "Tape jitter, tracking lines, and soft color drift", 3f),
    PIXELATE("Pixelate", "Chunky display pixels", 4f),
    DREAM_BLOOM("Dream Bloom", "Soft luminous highlights and a hazy lens", 5f),
    MONO_INK("Mono Ink", "High-contrast monochrome editorial treatment", 6f),
    KALEIDOSCOPE("Kaleidoscope", "Mirrored radial glass sectors", 7f),
    FISHEYE("Fisheye", "Optical barrel curvature with edge compression", 8f),
    HALFTONE("Halftone", "Printed-dot screening driven by luminance", 9f),
    THERMAL("Thermal", "False-color infrared mapping", 10f),
    NEGATIVE("Negative", "Photographic color inversion", 11f),
    POSTERIZE("Posterize", "Hard tonal screen-print bands", 12f),
    FILM_GRAIN("35mm Film", "Grain, vignette, and warm highlights", 13f),
    MIRROR_PRISM("Mirror Prism", "Angular mirrored facets", 14f),
    LIQUID_GLASS("Liquid Glass", "Animated thick-glass refraction", 15f),
    NIGHT_VISION("Night Vision", "Green phosphor, bloom, noise, and edge falloff", 16f),
}

data class CropRecipe(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f,
    val muted: Boolean = true,
    val loop: Boolean = true,
    val filters: List<ThemeFilter> = emptyList(),
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
