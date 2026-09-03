package com.rimor.minipape

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.core.net.toUri

@Composable
fun WallpaperSurface(session: PreviewSession, modifier: Modifier = Modifier) {
    val transform = Modifier.graphicsLayer {
        scaleX = session.recipe.scale
        scaleY = session.recipe.scale
        translationX = session.recipe.offsetX * size.width * 0.5f
        translationY = session.recipe.offsetY * size.height * 0.5f
        rotationZ = session.recipe.rotation
    }
    Box(modifier.background(Color.Black).clipToBounds()) {
        val source = session.source ?: return@Box
        ThemeFilterStack(session.recipe.filters, Modifier.fillMaxSize()) {
            if (session.mediaKind == "video") {
                VideoFrame(
                    uri = source.toUri(),
                    loop = session.recipe.loop,
                    muted = session.recipe.muted,
                    playing = true,
                    playheadSeconds = 0.0,
                    modifier = Modifier.fillMaxSize().then(transform),
                )
            } else {
                coil3.compose.AsyncImage(
                    model = source,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().then(transform),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}
