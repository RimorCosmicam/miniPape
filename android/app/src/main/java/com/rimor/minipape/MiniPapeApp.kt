package com.rimor.minipape

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.net.Inet4Address
import java.net.NetworkInterface

private enum class EditPanel { Crop, Filters }

@Composable
fun MiniPapeApp(viewModel: MiniPapeViewModel = viewModel()) {
    val pager = rememberPagerState(initialPage = 0, pageCount = { 2 })
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
    ) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> GalleryScreen(viewModel)
                else -> CreateScreen(viewModel)
            }
        }
    }
}

@Composable
private fun ImageIcon(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        painter = painterResource(R.drawable.minipape_brand),
        contentDescription = "miniPape",
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun GalleryScreen(viewModel: MiniPapeViewModel) {
    val wallpapers by viewModel.wallpapers.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val sessions = buildList {
        if (preview.source != null) add("Live from Mac" to preview)
        wallpapers.forEach { item ->
            add(item.displayName to PreviewSession(source = item.file, mediaKind = item.mediaKind, recipe = item.recipe))
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cardHeight = maxHeight - 8.dp
        if (sessions.isEmpty()) {
            EmptyGallery()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sessions, key = { it.first + (it.second.source?.absolutePath ?: "live") }) { (label, session) ->
                    Card(
                        modifier = Modifier.fillMaxWidth().height(cardHeight),
                        shape = RoundedCornerShape(26.dp),
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            WallpaperSurface(session, Modifier.fillMaxSize())
                            Surface(
                                modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.66f),
                                shape = RoundedCornerShape(15.dp),
                            ) {
                                Text(
                                    label,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGallery() {
    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ImageIcon(Modifier.size(76.dp))
        Text("No wallpapers yet", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp), color = Color.White)
        Text("Swipe left to create one", color = Color.White.copy(alpha = 0.72f))
    }
}

@Composable
private fun CreateScreen(viewModel: MiniPapeViewModel) {
    val context = LocalContext.current
    val address = remember { localAddress() }
    var pairingOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Uri?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var horizontal by remember { mutableFloatStateOf(0f) }
    var vertical by remember { mutableFloatStateOf(0f) }
    var filters by remember { mutableStateOf<List<ThemeFilter>>(emptyList()) }
    var panel by remember { mutableStateOf(EditPanel.Crop) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) selected = result.data?.data
    }
    val chooseMedia = {
        picker.launch(mediaSourceChooser())
    }
    val save: () -> Unit = {
        selected?.let {
            viewModel.importFromPhone(
                context,
                it,
                CropRecipe(scale = scale, offsetX = horizontal, offsetY = vertical, filters = filters),
            )
        }
        Unit
    }

    if (pairingOpen) {
        Column(Modifier.fillMaxSize()) {
            ConnectionDetails(
                address,
                viewModel.pairCode,
                Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp, vertical = 6.dp),
            )
            MacButton(
                pairingOpen = true,
                onClick = { pairingOpen = false },
                modifier = Modifier.fillMaxWidth().height(54.dp),
            )
        }
    } else {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val coverMode = maxHeight < 600.dp
            if (coverMode) {
                CoverEditor(
                        selected, scale, horizontal, vertical, filters, panel, chooseMedia,
                        { scale = it }, { horizontal = it }, { vertical = it },
                        { filter -> filters = if (filter in filters) filters - filter else filters + filter },
                        { panel = it }, save, { pairingOpen = true },
                )
            } else {
                Column(Modifier.fillMaxSize()) {
                    TallEditor(
                        selected, scale, horizontal, vertical, filters, panel, chooseMedia,
                        { scale = it }, { horizontal = it }, { vertical = it },
                        { filter -> filters = if (filter in filters) filters - filter else filters + filter },
                        { panel = it }, save,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                    MacButton(
                        pairingOpen = false,
                        onClick = { pairingOpen = true },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MacButton(pairingOpen: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(start = 12.dp, top = 4.dp, bottom = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.height(40.dp),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = Color.White,
            ),
        ) {
            Icon(Icons.Default.Devices, contentDescription = null, Modifier.size(18.dp), tint = Color.White)
            Text(if (pairingOpen) "Done" else "Mac", modifier = Modifier.padding(start = 7.dp), color = Color.White)
        }
    }
}

private fun mediaSourceChooser(): Intent {
    return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        putExtra(Intent.EXTRA_LOCAL_ONLY, false)
    }
}

@Composable
private fun CoverEditor(
    selected: Uri?, scale: Float, horizontal: Float, vertical: Float,
    filters: List<ThemeFilter>, panel: EditPanel,
    onChoose: () -> Unit, onScale: (Float) -> Unit, onHorizontal: (Float) -> Unit,
    onVertical: (Float) -> Unit, onToggleFilter: (ThemeFilter) -> Unit,
    onPanel: (EditPanel) -> Unit, onSave: () -> Unit, onMac: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Column(
            modifier = Modifier.width(64.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.width(52.dp).height(196.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = Color.White,
            ) {
                val plain = IconButtonDefaults.iconButtonColors(
                    contentColor = Color.White,
                    disabledContentColor = Color.White.copy(alpha = 0.45f),
                )
                val filled = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = Color.White.copy(alpha = 0.45f),
                )
                Column(
                    modifier = Modifier.fillMaxSize().padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = onChoose, modifier = Modifier.size(42.dp), colors = plain) {
                        Icon(Icons.Default.Add, contentDescription = "Choose image or video", tint = Color.White)
                    }
                    if (panel == EditPanel.Crop) {
                        FilledIconButton(onClick = { onPanel(EditPanel.Crop) }, modifier = Modifier.size(42.dp), colors = filled) {
                            Icon(Icons.Default.Crop, contentDescription = "Crop controls", tint = Color.White)
                        }
                    } else {
                        IconButton(onClick = { onPanel(EditPanel.Crop) }, modifier = Modifier.size(42.dp), colors = plain) {
                            Icon(Icons.Default.Crop, contentDescription = "Crop controls", tint = Color.White)
                        }
                    }
                    if (panel == EditPanel.Filters) {
                        FilledIconButton(onClick = { onPanel(EditPanel.Filters) }, modifier = Modifier.size(42.dp), colors = filled) {
                            Icon(Icons.Default.Palette, contentDescription = "Filters, ${filters.size} selected", tint = Color.White)
                        }
                    } else {
                        IconButton(onClick = { onPanel(EditPanel.Filters) }, modifier = Modifier.size(42.dp), colors = plain) {
                            Icon(Icons.Default.Palette, contentDescription = "Filters, ${filters.size} selected", tint = Color.White)
                        }
                    }
                    FilledIconButton(
                        onClick = onSave,
                        enabled = selected != null,
                        modifier = Modifier.size(42.dp),
                        colors = filled,
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save wallpaper", tint = Color.White)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            FilledTonalButton(
                onClick = onMac,
                modifier = Modifier.width(64.dp).height(40.dp),
                shape = RoundedCornerShape(15.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = Color.White,
                ),
            ) {
                Text("Mac", color = Color.White, maxLines = 1)
            }
        }

        Column(Modifier.weight(1f).fillMaxHeight()) {
            if (selected != null && panel == EditPanel.Crop) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = Color.White,
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                        CoverSlider("Scale", scale, 1f..4f, onScale)
                        CoverSlider("X", horizontal, -1f..1f, onHorizontal)
                        CoverSlider("Y", vertical, -1f..1f, onVertical)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            if (selected != null && panel == EditPanel.Filters) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = Color.White,
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items(ThemeFilter.entries) { filter ->
                            FilterChip(
                                selected = filter in filters,
                                onClick = { onToggleFilter(filter) },
                                label = { Text(filter.label, maxLines = 1, color = Color.White) },
                                modifier = Modifier.height(44.dp),
                                colors = brightFilterChipColors(),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            EditorCanvas(
                selected, scale, horizontal, vertical, filters, onChoose,
                Modifier.fillMaxWidth().weight(1f),
            )
        }
    }
}

@Composable
private fun CoverSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth().height(29.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(42.dp), color = Color.White)
        Slider(
            value = value,
            onValueChange = onValue,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = brightSliderColors(),
        )
        Text(
            String.format("%.1f", value),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(28.dp),
            color = Color.White,
        )
    }
}

@Composable
private fun brightSliderColors() = SliderDefaults.colors(
    thumbColor = MaterialTheme.colorScheme.secondary,
    activeTrackColor = MaterialTheme.colorScheme.secondary,
    inactiveTrackColor = Color.White.copy(alpha = 0.28f),
    activeTickColor = Color.White,
    inactiveTickColor = Color.White.copy(alpha = 0.5f),
)

@Composable
private fun brightFilterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    labelColor = Color.White,
    iconColor = Color.White,
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = Color.White,
    selectedLeadingIconColor = Color.White,
    selectedTrailingIconColor = Color.White,
)

@Composable
private fun TallEditor(
    selected: Uri?, scale: Float, horizontal: Float, vertical: Float,
    filters: List<ThemeFilter>, panel: EditPanel,
    onChoose: () -> Unit, onScale: (Float) -> Unit, onHorizontal: (Float) -> Unit,
    onVertical: (Float) -> Unit, onToggleFilter: (ThemeFilter) -> Unit,
    onPanel: (EditPanel) -> Unit, onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        EditorCanvas(
            selected, scale, horizontal, vertical, filters, onChoose,
            Modifier.fillMaxWidth().weight(1f),
        )
        EditorControls(
            selected != null, scale, horizontal, vertical, filters, panel,
            onScale, onHorizontal, onVertical, onToggleFilter, onPanel, onSave,
            Modifier.fillMaxWidth().height(300.dp),
        )
    }
}

@Composable
private fun EditorCanvas(
    selected: Uri?, scale: Float, horizontal: Float, vertical: Float,
    filters: List<ThemeFilter>, onChoose: () -> Unit, modifier: Modifier,
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (selected == null) {
                FilledTonalButton(
                    onClick = onChoose,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Text("Choose", modifier = Modifier.padding(start = 8.dp), color = Color.White)
                }
            } else {
                ThemeFilterStack(filters, Modifier.fillMaxSize()) {
                    coil3.compose.AsyncImage(
                        model = selected,
                        contentDescription = "Cover crop preview",
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = horizontal * size.width * 0.5f
                            translationY = vertical * size.height * 0.5f
                        },
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorControls(
    hasMedia: Boolean, scale: Float, horizontal: Float, vertical: Float,
    filters: List<ThemeFilter>, panel: EditPanel,
    onScale: (Float) -> Unit, onHorizontal: (Float) -> Unit, onVertical: (Float) -> Unit,
    onToggleFilter: (ThemeFilter) -> Unit, onPanel: (EditPanel) -> Unit,
    onSave: () -> Unit, modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = Color.White,
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FilterChip(
                    selected = panel == EditPanel.Crop,
                    onClick = { onPanel(EditPanel.Crop) },
                    label = { Text("Crop", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, Modifier.size(18.dp), tint = Color.White) },
                    modifier = Modifier.weight(1f),
                    colors = brightFilterChipColors(),
                )
                FilterChip(
                    selected = panel == EditPanel.Filters,
                    onClick = { onPanel(EditPanel.Filters) },
                    label = { Text("FX ${filters.size}", color = Color.White) },
                    modifier = Modifier.weight(1f),
                    colors = brightFilterChipColors(),
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (panel == EditPanel.Crop) {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        CompactSlider("Scale", scale, 1f..4f, onScale)
                        CompactSlider("Left / right", horizontal, -1f..1f, onHorizontal)
                        CompactSlider("Up / down", vertical, -1f..1f, onVertical)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        items(ThemeFilter.entries) { filter ->
                            FilterChip(
                                selected = filter in filters,
                                onClick = { onToggleFilter(filter) },
                                label = { Text(filter.label, maxLines = 1, color = Color.White) },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                colors = brightFilterChipColors(),
                            )
                        }
                    }
                }
            }
            Button(
                onClick = onSave,
                enabled = hasMedia,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = Color.White.copy(alpha = 0.45f),
                ),
            ) { Text("Save", color = Color.White) }
        }
    }
}

@Composable
private fun CompactSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White)
            Spacer(Modifier.weight(1f))
            Text(String.format("%.2f", value), style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
        Slider(
            value = value,
            onValueChange = onValue,
            valueRange = range,
            modifier = Modifier.fillMaxWidth().height(34.dp),
            colors = brightSliderColors(),
        )
    }
}

@Composable
private fun ConnectionDetails(address: String, pairCode: String, modifier: Modifier = Modifier) {
    Surface(
        modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = Color.White,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, contentDescription = null, Modifier.size(20.dp), tint = Color.White)
                Text("Connect", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp), color = Color.White)
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.24f))
            Text("ADDRESS", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f))
            Text("$address:${ReceiverServer.PORT}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text("PAIR CODE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f))
            Text(pairCode.chunked(3).joinToString(" "), fontSize = 31.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

private fun localAddress(): String = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .flatMap { it.inetAddresses.toList() }
        .firstOrNull { it is Inet4Address && !it.isLoopbackAddress && it.isSiteLocalAddress }
        ?.hostAddress ?: "Unavailable"
}.getOrDefault("Unavailable")
