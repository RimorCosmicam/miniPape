package com.rimor.minipape

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kotlin.math.abs
import kotlin.math.hypot

/**
 * A Mont card: the same 92% black surface, held off the screen edges so the ground it stands on
 * stays visible around it. What onboarding and the editor sit inside.
 */
@Composable
fun MontCard(
    modifier: Modifier = Modifier,
    spacing: Dp = 10.dp,
    top: Dp = 22.dp,
    bottom: Dp = 16.dp,
    scrollable: Boolean = false,
    arrangement: Arrangement.Vertical? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Height is capped by whatever the card is given, and on a screen this small the contents
    // scroll inside that cap rather than deciding for themselves whether they still fit.
    val scroll = rememberScrollState()
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .background(Mont.Surface)
            .then(if (scrollable) Modifier.verticalScroll(scroll) else Modifier)
            .padding(start = 22.dp, top = top, end = 18.dp, bottom = bottom),
        verticalArrangement = arrangement ?: Arrangement.spacedBy(spacing),
        content = content,
    )
}

/**
 * The one thing a screen is for, said large and centred. Still only a word: no box, no border,
 * no fill — it is simply the biggest and brightest thing on the surface.
 */
@Composable
fun MontAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Text(
        label.uppercase(),
        style = Mont.action,
        color = if (enabled) Mont.Selected else Mont.Disabled,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(vertical = Mont.RowPadding.dp),
    )
}

/**
 * A term and what it means, on one line.
 *
 * The term takes the width of the word and no more. Giving it a fixed share of the row instead
 * leaves a short word like DRAG sitting alone in a hundred dp of nothing, with its own
 * explanation stranded on the far side of the gap.
 */
@Composable
fun MontTermLine(term: String, meaning: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()) {
        Text(
            term.uppercase(),
            style = Mont.term,
            color = Mont.Selected,
            maxLines = 1,
            modifier = Modifier.alignByBaseline(),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            meaning,
            style = Mont.meaning,
            color = Mont.Explanatory,
            modifier = Modifier.alignByBaseline().weight(1f),
        )
    }
}

/** A word, full width, tappable. Bright if it does something now, dim if it is secondary. */
@Composable
fun MontRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bright: Boolean = true,
    enabled: Boolean = true,
    explanation: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(vertical = Mont.RowPadding.dp),
    ) {
        Text(
            label.uppercase(),
            style = Mont.row,
            color = when {
                !enabled -> Mont.Disabled
                bright -> Mont.Selected
                else -> Mont.Dim
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (explanation != null) MontExplanation(explanation, Modifier.padding(top = 3.dp))
    }
}

/** The explanatory line under a row. */
@Composable
fun MontExplanation(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = Mont.explanatory, color = Mont.Explanatory, modifier = modifier)
}

/**
 * The slider, full width by 18. No track, no bead, no rounding: the bar is a faint white wash
 * and dragging fills it with white from the left, so the value reads as an area rather than as
 * the position of a dot.
 */
@Composable
fun MontBar(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValue: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val travel = range.endInclusive - range.start
    val span = travel.takeIf { it > 0f } ?: 1f
    val fraction = if (travel > 0f) ((value - range.start) / span).coerceIn(0f, 1f) else 0f
    Canvas(
        modifier
            .fillMaxWidth()
            .height(18.dp)
            .then(if (!enabled || travel <= 0f) Modifier else Modifier
            .pointerInput(range) {
                detectTapGestures { offset ->
                    onValue(range.start + (offset.x / size.width.toFloat()).coerceIn(0f, 1f) * span)
                }
            }
            .pointerInput(range) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        onValue(range.start + (offset.x / size.width.toFloat()).coerceIn(0f, 1f) * span)
                    },
                ) { change, _ ->
                    onValue(range.start + (change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * span)
                }
            }),
    ) {
        drawRect(Mont.Track)
        if (fraction > 0f) drawRect(Color.White, size = Size(size.width * fraction, size.height))
    }
}

/**
 * The toggle, 56 by 18: the slider stopped at two positions. A white block fills one half and
 * the state is written in the half it has left. The word names what the control currently is,
 * not what tapping it would do.
 */
@Composable
fun MontToggle(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .width(56.dp)
            .height(18.dp)
            .background(Mont.Track)
            .clickable(
                interactionSource = interaction,
                indication = null,
            ) { onChange(!checked) },
    ) {
        if (checked) {
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Text("ON", style = Mont.toggle, color = Mont.Selected)
            }
            Box(Modifier.weight(1f).fillMaxHeight().background(Color.White))
        } else {
            Box(Modifier.weight(1f).fillMaxHeight().background(Color.White))
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Text("OFF", style = Mont.toggle, color = Mont.Dim)
            }
        }
    }
}

/** mini in Thin, Pape in Black, at the same size, stacked. That contrast is the logo. */
@Composable
fun MontStackedWordmark(modifier: Modifier = Modifier, size: TextUnit = 34.sp) {
    Column(modifier) {
        Text("mini", style = Mont.wordmark.copy(fontSize = size), color = Mont.Selected)
        Text("Pape", style = Mont.wordmarkBold.copy(fontSize = size), color = Mont.Selected)
    }
}

/**
 * Diagonal stripes: bands at 26.565 degrees — a 1:2 slope — 34dp apart, split evenly between
 * the two colours, scrolling slowly along their own axis. Drawn in a rotated frame so that
 * within it they are plain horizontal rows. The colour pair carries the state.
 */
@Composable
fun MontStripes(accent: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "MontStripes")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9_000, easing = LinearEasing)),
        label = "MontStripePhase",
    )
    Canvas(modifier.background(Color.Black)) {
        val period = 34.dp.toPx()
        val band = period / 2f
        val reach = hypot(size.width, size.height)
        rotate(degrees = 26.565f) {
            val left = center.x - reach
            var y = center.y - reach + phase * period
            while (y < center.y + reach) {
                drawRect(accent, topLeft = Offset(left, y), size = Size(reach * 2f, band))
                y += period
            }
        }
    }
}

/** A caption above a control. Same weight as an explanatory line, same 62% white. */
@Composable
fun MontCaption(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) {
    Text(
        text.uppercase(),
        style = Mont.explanatory,
        color = Mont.Explanatory,
        textAlign = align,
        maxLines = 1,
        modifier = modifier,
    )
}

/** A row that does not want the whole width — the same word, the same rule about brightness. */
@Composable
fun MontWord(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bright: Boolean = true,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Text(
        label.uppercase(),
        style = Mont.row,
        color = when {
            !enabled -> Mont.Disabled
            bright -> Mont.Selected
            else -> Mont.Dim
        },
        maxLines = 1,
        modifier = modifier
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(vertical = Mont.RowPadding.dp),
    )
}

/**
 * The trim: a white line with two mustard selectors over it, dragged to choose where the cut
 * starts and where it ends. The nearer selector is the one that answers a touch.
 */
@Composable
fun MontTrimBar(
    start: Float,
    end: Float,
    onRange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The gesture block outlives the composition that created it, so it has to read the
    // selectors through something that keeps up. Capturing them directly freezes the one you
    // are not dragging at whatever it was when the bar first appeared.
    val from by rememberUpdatedState(start)
    val to by rememberUpdatedState(end)
    val report by rememberUpdatedState(onRange)
    var holding by remember { mutableIntStateOf(0) }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(26.dp)
            .pointerInput(Unit) {
                fun at(x: Float) = (x / size.width.toFloat()).coerceIn(0f, 1f)
                fun move(fraction: Float) {
                    if (holding == 1) {
                        report(fraction.coerceAtMost(to - MIN_TRIM_SPAN), to)
                    } else {
                        report(from, fraction.coerceAtLeast(from + MIN_TRIM_SPAN))
                    }
                }
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val fraction = at(offset.x)
                        holding = if (abs(fraction - from) <= abs(fraction - to)) 1 else 2
                        move(fraction)
                    },
                ) { change, _ -> move(at(change.position.x)) }
            },
    ) {
        val line = 2.dp.toPx()
        val handleWidth = 8.dp.toPx()
        drawRect(
            Color.White,
            topLeft = Offset(0f, (size.height - line) / 2f),
            size = Size(size.width, line),
        )
        listOf(start, end).forEach { fraction ->
            val centre = fraction.coerceIn(0f, 1f) * size.width
            drawRect(
                Mont.Mustard,
                topLeft = Offset(
                    (centre - handleWidth / 2f).coerceIn(0f, size.width - handleWidth),
                    0f,
                ),
                size = Size(handleWidth, size.height),
            )
        }
    }
}

const val MIN_TRIM_SPAN = 0.05f

/** Two words stacked, acting as one control. Bright when it is on. */
@Composable
fun MontPiledWord(
    first: String,
    second: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bright: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick,
        ),
    ) {
        val colour = if (bright) Mont.Selected else Mont.Dim
        Text(first.uppercase(), style = Mont.row, color = colour, maxLines = 1)
        Text(second.uppercase(), style = Mont.row, color = colour, maxLines = 1)
    }
}
