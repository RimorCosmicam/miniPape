package com.rimor.minipape

import android.net.Uri
import android.view.TextureView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer

/**
 * A video filling its box the way ContentScale.Crop fills one, so the editor frame and the cover
 * show the same thing the export cuts.
 *
 * A bare TextureView stretches its surface to the view, which is not a crop. Correcting it takes
 * one extra uniform scale: whichever axis the stretch shortened is scaled back up until both axes
 * agree again, which necessarily leaves the other axis overflowing — that overflow is the crop.
 */
@Composable
fun VideoFrame(
    uri: Uri,
    loop: Boolean,
    muted: Boolean,
    playing: Boolean,
    playheadSeconds: Double,
    modifier: Modifier = Modifier,
    onAspect: (Float) -> Unit = {},
) {
    val context = LocalContext.current
    var videoAspect by remember(uri) { mutableFloatStateOf(0f) }
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(size: VideoSize) {
                val ratio = if (size.height > 0) {
                    size.width.toFloat() / size.height.toFloat() * size.pixelWidthHeightRatio
                } else {
                    0f
                }
                if (ratio > 0f) {
                    videoAspect = ratio
                    onAspect(ratio)
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, loop, muted, playing, playheadSeconds) {
        player.repeatMode = if (loop) ExoPlayer.REPEAT_MODE_ALL else ExoPlayer.REPEAT_MODE_OFF
        player.volume = if (muted) 0f else 1f
        if (playheadSeconds > 0.0) player.seekTo((playheadSeconds * 1000).toLong())
        player.playWhenReady = playing
    }

    Box(modifier) {
        AndroidView(
            factory = { TextureView(it).also(player::setVideoTextureView) },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val box = if (size.height > 0f) size.width / size.height else 0f
                    if (videoAspect > 0f && box > 0f) {
                        scaleX = maxOf(1f, videoAspect / box)
                        scaleY = maxOf(1f, box / videoAspect)
                    }
                },
        )
    }
}
