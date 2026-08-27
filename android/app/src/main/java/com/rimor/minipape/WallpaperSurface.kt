package com.rimor.minipape

import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer

@Composable
fun WallpaperSurface(session: PreviewSession, modifier: Modifier = Modifier) {
    val transform = Modifier.graphicsLayer {
        scaleX = session.recipe.scale
        scaleY = session.recipe.scale
        translationX = session.recipe.offsetX * size.width * 0.5f
        translationY = session.recipe.offsetY * size.height * 0.5f
        rotationZ = session.recipe.rotation
    }
    Box(modifier.background(Color.Black)) {
        val source = session.source ?: return@Box
        ThemeFilterStack(session.recipe.filters, Modifier.fillMaxSize()) {
            if (session.mediaKind == "video") {
                VideoSurface(source.toUri(), session, Modifier.fillMaxSize().then(transform))
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

@Composable
private fun VideoSurface(uri: android.net.Uri, session: PreviewSession, modifier: Modifier) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            repeatMode = if (session.recipe.loop) ExoPlayer.REPEAT_MODE_ALL else ExoPlayer.REPEAT_MODE_OFF
            volume = if (session.recipe.muted) 0f else 1f
            prepare()
        }
    }
    LaunchedEffect(session.playhead, session.playing, session.recipe.muted, session.recipe.loop) {
        player.seekTo((session.playhead * 1000).toLong())
        player.volume = if (session.recipe.muted) 0f else 1f
        player.repeatMode = if (session.recipe.loop) ExoPlayer.REPEAT_MODE_ALL else ExoPlayer.REPEAT_MODE_OFF
        player.playWhenReady = session.playing
    }
    AndroidView(
        factory = {
            TextureView(it).also(player::setVideoTextureView)
        },
        modifier = modifier,
    )
    DisposableEffect(player) { onDispose { player.release() } }
}
