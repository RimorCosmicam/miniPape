package com.rimor.minipape

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged

private const val CHROMATIC_SHADER = """
    uniform shader content;
    uniform float uShift;
    uniform float uRipple;
    uniform float uWave;

    half4 main(float2 p) {
        float shift = uShift + sin(p.y * uWave) * uRipple;
        half r = content.eval(p + float2(shift, 0.0)).r;
        half g = content.eval(p).g;
        half b = content.eval(p - float2(shift, 0.0)).b;
        return half4(r, g, b, 1.0);
    }
"""

/**
 * The aberration, previewed.
 *
 * Its own small shader rather than a branch of the theme filter stack, for two reasons: it has to
 * agree with [ChromaticAberration] exactly, and it scales the separation from canvas pixels down
 * to preview pixels, so a small window shows the strength the full-size file will actually have.
 */
@Composable
fun ChromaticSurface(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ShadedChromaticSurface(modifier, content)
    } else {
        Box(modifier) { content() }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShadedChromaticSurface(modifier: Modifier, content: @Composable () -> Unit) {
    val shader = remember { RuntimeShader(CHROMATIC_SHADER) }
    var width by remember { mutableFloatStateOf(0f) }

    // A RenderEffect copies the shader it is built from, so new uniforms mean a new effect
    // rather than a new value pushed at the old one.
    val effect = remember(width) {
        val scale = if (width > 0f) width / CoverCanvas.WIDTH else 1f
        shader.setFloatUniform("uShift", ChromaticAberration.SEPARATION * scale)
        shader.setFloatUniform("uRipple", ChromaticAberration.RIPPLE * scale)
        shader.setFloatUniform("uWave", ChromaticAberration.WAVE / scale)
        RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
    }

    Box(
        modifier
            .onSizeChanged { width = it.width.toFloat() }
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                renderEffect = effect
            },
    ) { content() }
}
