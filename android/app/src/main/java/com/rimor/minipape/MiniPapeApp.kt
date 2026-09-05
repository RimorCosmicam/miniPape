package com.rimor.minipape

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class Stage { Onboarding, Load, Frame, Finish }

/** The share of the framing screen's width the window takes, leaving the rest for the pile. */
private const val FRAME_SHARE = 0.56f

/**
 * What the editor is holding. Kept in one object so gesture handlers always read live values,
 * and so the travel limits sit next to the numbers they constrain.
 */
private class EditorState {
    var media by mutableStateOf<Uri?>(null)
    var kind by mutableStateOf("")
    var loop by mutableStateOf(true)
    var chromatic by mutableStateOf(false)

    /**
     * Width over height of the source, taken from the painter that draws it — the same number
     * ContentScale.Crop scales by, so the limits cannot disagree with what is on screen.
     */
    var aspect by mutableFloatStateOf(0f)
        private set

    var zoom by mutableFloatStateOf(1f)
        private set
    var horizontal by mutableFloatStateOf(0f)
        private set
    var vertical by mutableFloatStateOf(0f)
        private set
    var trimStart by mutableFloatStateOf(0f)
        private set
    var trimEnd by mutableFloatStateOf(1f)
        private set

    val isMotion: Boolean get() = kind.startsWith("video/") || kind == "image/gif"

    val horizontalLimit: Float get() = CoverCanvas.horizontalLimit(aspect, zoom)
    val verticalLimit: Float get() = CoverCanvas.verticalLimit(aspect, zoom)

    fun load(uri: Uri, mimeType: String) {
        media = uri
        kind = mimeType
        aspect = 0f
        zoom = 1f
        horizontal = 0f
        vertical = 0f
        trimStart = 0f
        trimEnd = 1f
        chromatic = false
    }

    fun measure(value: Float) {
        if (value > 0f && value != aspect) {
            aspect = value
            settle()
        }
    }

    fun zoomTo(value: Float) {
        zoom = value.coerceIn(CoverCanvas.MIN_ZOOM, CoverCanvas.MAX_ZOOM)
        settle()
    }

    fun panHorizontalTo(value: Float) {
        horizontal = value
        settle()
    }

    fun panVerticalTo(value: Float) {
        vertical = value
        settle()
    }

    fun pan(dx: Float, dy: Float) {
        horizontal += dx
        vertical += dy
        settle()
    }

    fun trimTo(start: Float, end: Float) {
        trimStart = start.coerceIn(0f, 1f - MIN_TRIM_SPAN)
        trimEnd = end.coerceIn(trimStart + MIN_TRIM_SPAN, 1f)
    }

    /** Pulls the framing back inside its limits, so no edge of the media enters the window. */
    private fun settle() {
        horizontal = horizontal.coerceIn(-horizontalLimit, horizontalLimit)
        vertical = vertical.coerceIn(-verticalLimit, verticalLimit)
    }

    fun recipe() = CropRecipe(
        scale = zoom,
        offsetX = horizontal,
        offsetY = vertical,
        loop = loop,
        trimStart = trimStart,
        trimEnd = trimEnd,
        filters = if (chromatic) listOf(ThemeFilter.CHROMATIC) else emptyList(),
    )
}

@Composable
fun MiniPapeApp(viewModel: MiniPapeViewModel = viewModel()) {
    val context = LocalContext.current
    val editor = remember { EditorState() }
    var stage by remember { mutableStateOf(if (onboarded(context)) Stage.Load else Stage.Onboarding) }
    val save by viewModel.save.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            editor.load(uri, context.contentResolver.getType(uri).orEmpty())
            viewModel.clearSave()
            stage = Stage.Frame
        }
    }
    // Handing the cut to a gallery ends the job, so coming back from the chooser lands on
    // loading rather than on an editor holding work that has already left.
    val opener = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.clearSave()
        stage = Stage.Load
    }

    BackHandler(enabled = stage == Stage.Frame || stage == Stage.Finish) {
        stage = if (stage == Stage.Finish) Stage.Frame else Stage.Load
    }

    // The ground runs edge to edge, including under the camera. Only the content holds off it.
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (stage) {
            Stage.Onboarding -> OnboardingScreen {
                markOnboarded(context)
                stage = Stage.Load
            }

            Stage.Load -> LoadScreen(
                onLoad = { picker.launch(mediaChooser()) },
                onOnboarding = { stage = Stage.Onboarding },
            )

            Stage.Frame -> FrameScreen(
                editor = editor,
                onEdited = viewModel::clearSave,
                onBack = { stage = Stage.Load },
                onNext = { stage = Stage.Finish },
            )

            Stage.Finish -> FinishScreen(
                editor = editor,
                save = save,
                onEdited = viewModel::clearSave,
                onBack = { stage = Stage.Frame },
                onSave = { editor.media?.let { viewModel.save(context, it, editor.recipe()) } },
                onOpen = { uri, mimeType -> opener.launch(galleryChooser(uri, mimeType)) },
                onDone = {
                    viewModel.clearSave()
                    stage = Stage.Load
                },
            )
        }
    }
}

/**
 * Everything readable holds off the system's unsafe edges — on the cover, the camera.
 *
 * Two things go wrong here and both have to be undone. The Flex Window dispatches no cutout to
 * the window at all, so safeDrawing comes back empty and has to be replaced by asking the display
 * itself. And the window is rendered larger than the panel and scaled down onto it — 1244x1375
 * for a 948x1048 screen — while the cutout is reported in the panel's own pixels. Padding by the
 * raw 220 therefore reserves only 167 real pixels for a 220 pixel camera, which is exactly how
 * far a card ends up sitting over the lens.
 */
@Composable
private fun Modifier.coverSafe(): Modifier {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val view = LocalView.current
    val container = LocalWindowInfo.current.containerSize
    val cutout = remember(view) { view.display?.cutout }
    val mode = remember(view) { view.display?.mode }

    val across = mode?.physicalWidth?.takeIf { it > 0 }
        ?.let { container.width.toFloat() / it } ?: 1f
    val down = mode?.physicalHeight?.takeIf { it > 0 }
        ?.let { container.height.toFloat() / it } ?: 1f

    val safe = WindowInsets.safeDrawing
    val left = maxOf(safe.getLeft(density, direction).toFloat(), (cutout?.safeInsetLeft ?: 0) * across)
    val top = maxOf(safe.getTop(density).toFloat(), (cutout?.safeInsetTop ?: 0) * down)
    val right = maxOf(safe.getRight(density, direction).toFloat(), (cutout?.safeInsetRight ?: 0) * across)
    val bottom = maxOf(safe.getBottom(density).toFloat(), (cutout?.safeInsetBottom ?: 0) * down)
    return with(density) { padding(left.toDp(), top.toDp(), right.toDp(), bottom.toDp()) }
}

/** The one card each screen is built from, filling the display it was given. */
@Composable
private fun FullCard(
    arrangement: Arrangement.Vertical? = Arrangement.SpaceBetween,
    spacing: Int = 10,
    top: Int = 22,
    bottom: Int = 16,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        MontStripes(Mont.Mustard, Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().coverSafe().padding(vertical = 12.dp)) {
            MontCard(
                Modifier.weight(1f),
                spacing = spacing.dp,
                top = top.dp,
                bottom = bottom.dp,
                arrangement = arrangement,
                content = content,
            )
        }
    }
}

// --- onboarding --------------------------------------------------------------------------

private class OnboardingPage(
    val heading: String,
    val lines: List<Pair<String, String>>,
    val advance: String,
)

private val onboardingPages = listOf(
    OnboardingPage(
        heading = "The window",
        lines = listOf(
            "Drag" to "Move the media around",
            "Pinch" to "Or pull the zoom bar",
            "Bars" to "Nudge it left, right, up, down",
        ),
        advance = "Next",
    ),
    OnboardingPage(
        heading = "The cut",
        lines = listOf(
            "Trim" to "Two selectors on a line",
            "Loop off" to "It stops on the last frame",
            "Save" to "Then open it in your gallery",
        ),
        advance = "Load media",
    ),
)

@Composable
private fun OnboardingScreen(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    FullCard {
        // The wordmark and the step belong together; only the way onward sits apart from them.
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            MontStackedWordmark(size = 40.sp)
            // One card throughout: it exchanges what it holds and resizes to fit, rather than
            // one screen vanishing and another taking its place.
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (fadeIn(tween(240, delayMillis = 120)) togetherWith fadeOut(tween(140)))
                        .using(SizeTransform(clip = false) { _, _ -> tween(360, easing = FastOutSlowInEasing) })
                },
                label = "OnboardingStep",
            ) { index ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MontCaption(onboardingPages[index].heading)
                    onboardingPages[index].lines.forEach { (term, meaning) -> MontTermLine(term, meaning) }
                }
            }
        }
        MontRow(
            onboardingPages[step].advance,
            onClick = { if (step == onboardingPages.lastIndex) onDone() else step++ },
        )
    }
}

// --- load --------------------------------------------------------------------------------

@Composable
private fun LoadScreen(onLoad: () -> Unit, onOnboarding: () -> Unit) {
    FullCard(arrangement = null) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            MontStackedWordmark(size = 40.sp)
            Spacer(Modifier.weight(1f))
            MontWord("?", onClick = onOnboarding, bright = false)
        }
        // The screen exists to open one thing, so that is what fills it.
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            MontAction("Load media", onClick = onLoad)
        }
    }
}

// --- framing -----------------------------------------------------------------------------

@Composable
private fun FrameScreen(
    editor: EditorState,
    onEdited: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    FullCard(arrangement = null, spacing = 9, top = 14, bottom = 12) {
        Row(Modifier.fillMaxWidth().weight(1f)) {
            EditorWindow(editor, Modifier.fillMaxWidth(FRAME_SHARE).fillMaxHeight())
            Spacer(Modifier.width(12.dp))
            // Movement and loop piled beside the window, where they are out of its way.
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MontCaption("Horizontal")
                    val limit = editor.horizontalLimit
                    MontBar(
                        editor.horizontal,
                        -limit..limit,
                        { onEdited(); editor.panHorizontalTo(it) },
                        enabled = limit > 0f,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MontCaption("Vertical")
                    val limit = editor.verticalLimit
                    MontBar(
                        editor.vertical,
                        -limit..limit,
                        { onEdited(); editor.panVerticalTo(it) },
                        enabled = limit > 0f,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("LOOP", style = Mont.row, color = Mont.Primary)
                    Spacer(Modifier.width(10.dp))
                    MontToggle(editor.loop, { onEdited(); editor.loop = it })
                }
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(
                Modifier.fillMaxWidth(FRAME_SHARE),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                MontCaption("Zoom")
                MontBar(
                    editor.zoom,
                    CoverCanvas.MIN_ZOOM..CoverCanvas.MAX_ZOOM,
                    { onEdited(); editor.zoomTo(it) },
                )
            }
            Spacer(Modifier.weight(1f))
            MontWord("Back", onClick = onBack, bright = false)
            Spacer(Modifier.width(14.dp))
            MontWord("Next", onClick = onNext, enabled = editor.media != null)
        }
    }
}

// --- finishing ---------------------------------------------------------------------------

@Composable
private fun FinishScreen(
    editor: EditorState,
    save: SaveState,
    onEdited: () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onOpen: (Uri, String) -> Unit,
    onDone: () -> Unit,
) {
    FullCard(arrangement = null, spacing = 12, top = 14, bottom = 12) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            EditorWindow(editor, Modifier.fillMaxHeight())
        }

        if (editor.isMotion) {
            MontTrimBar(
                editor.trimStart,
                editor.trimEnd,
                { start, end -> onEdited(); editor.trimTo(start, end) },
            )
        }

        val saved = save as? SaveState.Saved
        if (saved == null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                MontPiledWord(
                    "Chromatic",
                    "Aberration",
                    onClick = { onEdited(); editor.chromatic = !editor.chromatic },
                    bright = editor.chromatic,
                )
                Spacer(Modifier.weight(1f))
                MontWord("Back", onClick = onBack, bright = false)
                Spacer(Modifier.width(14.dp))
                MontWord(
                    when (save) {
                        SaveState.Working -> "Cutting"
                        is SaveState.Failed -> "Retry"
                        else -> "Save"
                    },
                    onClick = onSave,
                    enabled = editor.media != null && save !is SaveState.Working,
                )
            }
            if (save is SaveState.Failed) MontExplanation(save.message)
        } else {
            // Cut and filed. The list ends the way every Mont list ends: the way out first,
            // then the thing you came for.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MontWord("Later", onClick = onDone, bright = false)
                Spacer(Modifier.weight(1f))
                MontWord(
                    "Open",
                    onClick = { saved.shared?.let { onOpen(it, saved.mimeType) } ?: onDone() },
                )
            }
            MontExplanation("Saved to Pictures/miniPape")
        }
    }
}

// --- the window ---------------------------------------------------------------------------

/** The cover's shape, as large as the space it is given allows. */
@Composable
private fun EditorWindow(editor: EditorState, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        BoxWithConstraints {
            val width = minOf(maxWidth, maxHeight * CoverCanvas.RATIO)
            EditorFrame(editor, Modifier.width(width).height(width / CoverCanvas.RATIO))
        }
    }
}

@Composable
private fun EditorFrame(editor: EditorState, modifier: Modifier) {
    Box(
        modifier
            .background(Color.Black)
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    editor.zoomTo(editor.zoom * zoom)
                    editor.pan(pan.x * 2f / size.width, pan.y * 2f / size.height)
                }
            },
    ) {
        val media = editor.media ?: return@Box
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // The media is laid out at the size that covers the window and centred over it, so
            // the parts hanging outside are really there and can be brought in. Cropping it to
            // the window instead throws them away, and then moving it only slides the crop and
            // leaves the ground showing behind — which is precisely what it used to do.
            val density = LocalDensity.current
            val frameWidth = with(density) { maxWidth.toPx() }
            val frameHeight = with(density) { maxHeight.toPx() }
            // requiredSize, not size: size is coerced back into the parent's constraints, which
            // would clamp the media to the window and crop away the very overflow being laid out.
            val transform = Modifier
                .requiredSize(
                    maxWidth * CoverCanvas.coverX(editor.aspect),
                    maxHeight * CoverCanvas.coverY(editor.aspect),
                )
                .graphicsLayer {
                    scaleX = editor.zoom
                    scaleY = editor.zoom
                    // Offsets are a fraction of the window, not of the media laid over it.
                    translationX = editor.horizontal * frameWidth * 0.5f
                    translationY = editor.vertical * frameHeight * 0.5f
                }
            ChromaticSurface(editor.chromatic, Modifier.fillMaxSize()) {
                if (editor.kind.startsWith("video/")) {
                    VideoFrame(
                        uri = media,
                        loop = true,
                        muted = true,
                        playing = true,
                        playheadSeconds = 0.0,
                        modifier = transform,
                        onAspect = editor::measure,
                    )
                } else {
                    val painter = coil3.compose.rememberAsyncImagePainter(model = media)
                    val intrinsic = painter.intrinsicSize
                    LaunchedEffect(intrinsic) {
                        if (intrinsic.isSpecified && intrinsic.height > 0f) {
                            editor.measure(intrinsic.width / intrinsic.height)
                        }
                    }
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = transform,
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    }
}

// --- plumbing ----------------------------------------------------------------------------

private fun mediaChooser(): Intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = "*/*"
    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
    putExtra(Intent.EXTRA_LOCAL_ONLY, false)
}

/** Whatever the phone has that can show a picture. */
private fun galleryChooser(uri: Uri, mimeType: String): Intent {
    val view = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return Intent.createChooser(view, "Open in")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}

private const val PREFERENCES = "minipape"
private const val ONBOARDED = "onboarded"

private fun onboarded(context: Context): Boolean =
    context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getBoolean(ONBOARDED, false)

private fun markOnboarded(context: Context) {
    context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(ONBOARDED, true)
        .apply()
}
