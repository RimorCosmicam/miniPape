package com.rimor.minipape

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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

private enum class Destination(val label: String) {
    Gallery("Gallery"), Create("Create"), Connect("Connect")
}

private enum class EditPanel { Crop, Filters }

@Composable
fun MiniPapeApp(viewModel: MiniPapeViewModel = viewModel()) {
    var destination by remember { mutableStateOf(Destination.Gallery) }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(Modifier.fillMaxSize()) {
            CompactHeader(destination, onDestinationChange = { destination = it })
            AnimatedContent(destination, modifier = Modifier.weight(1f), label = "destination") { screen ->
                when (screen) {
                    Destination.Gallery -> GalleryScreen(viewModel)
                    Destination.Create -> CreateScreen(viewModel)
                    Destination.Connect -> ConnectScreen(viewModel)
                }
            }
        }
    }
}

@Composable
private fun CompactHeader(destination: Destination, onDestinationChange: (Destination) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        ImageIcon(Modifier.size(42.dp))
        Text(
            "miniPape",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 3.dp),
        )
        Spacer(Modifier.weight(1f))
        Destination.entries.forEach { item ->
            val icon = when (item) {
                Destination.Gallery -> Icons.Default.Collections
                Destination.Create -> Icons.Default.Edit
                Destination.Connect -> Icons.Default.Devices
            }
            if (destination == item) {
                FilledIconButton(onClick = { onDestinationChange(item) }, modifier = Modifier.size(44.dp)) {
                    Icon(icon, contentDescription = item.label)
                }
            } else {
                IconButton(onClick = { onDestinationChange(item) }, modifier = Modifier.size(44.dp)) {
                    Icon(icon, contentDescription = item.label)
                }
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
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cardWidth = maxWidth - 20.dp
        val sessions = buildList {
            if (preview.source != null) add("Live from Mac" to preview)
            wallpapers.forEach { item ->
                add(item.displayName to PreviewSession(source = item.file, mediaKind = item.mediaKind, recipe = item.recipe))
            }
        }
        if (sessions.isEmpty()) {
            EmptyGallery()
        } else {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(sessions, key = { it.first + (it.second.source?.absolutePath ?: "live") }) { (label, session) ->
                    Card(
                        modifier = Modifier.width(cardWidth).fillMaxHeight(),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            WallpaperSurface(session, Modifier.fillMaxSize())
                            Surface(
                                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.68f),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Text(
                                    label,
                                    color = MaterialTheme.colorScheme.inverseOnSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
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
        ImageIcon(Modifier.size(92.dp))
        Text("Your cover wallpapers", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
        Text("Create one here or preview from your Mac.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CreateScreen(viewModel: MiniPapeViewModel) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<Uri?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var horizontal by remember { mutableFloatStateOf(0f) }
    var vertical by remember { mutableFloatStateOf(0f) }
    var filters by remember { mutableStateOf<List<ThemeFilter>>(emptyList()) }
    var panel by remember { mutableStateOf(EditPanel.Crop) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { selected = it }
    val chooseMedia = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }
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

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val coverMode = maxHeight < 600.dp
        if (coverMode) {
            CoverEditor(
                selected = selected,
                scale = scale,
                horizontal = horizontal,
                vertical = vertical,
                filters = filters,
                panel = panel,
                onChoose = chooseMedia,
                onScale = { scale = it },
                onHorizontal = { horizontal = it },
                onVertical = { vertical = it },
                onToggleFilter = { filter -> filters = if (filter in filters) filters - filter else filters + filter },
                onPanel = { panel = it },
                onSave = save,
            )
        } else {
            TallEditor(
                selected = selected,
                scale = scale,
                horizontal = horizontal,
                vertical = vertical,
                filters = filters,
                panel = panel,
                onChoose = chooseMedia,
                onScale = { scale = it },
                onHorizontal = { horizontal = it },
                onVertical = { vertical = it },
                onToggleFilter = { filter -> filters = if (filter in filters) filters - filter else filters + filter },
                onPanel = { panel = it },
                onSave = save,
            )
        }
    }
}

@Composable
private fun CoverEditor(
    selected: Uri?, scale: Float, horizontal: Float, vertical: Float,
    filters: List<ThemeFilter>, panel: EditPanel,
    onChoose: () -> Unit, onScale: (Float) -> Unit, onHorizontal: (Float) -> Unit,
    onVertical: (Float) -> Unit, onToggleFilter: (ThemeFilter) -> Unit,
    onPanel: (EditPanel) -> Unit, onSave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        EditorCanvas(
            selected, scale, horizontal, vertical, filters, onChoose,
            Modifier.weight(1.25f).fillMaxHeight(),
        )
        EditorControls(
            selected != null, scale, horizontal, vertical, filters, panel,
            onScale, onHorizontal, onVertical, onToggleFilter, onPanel, onSave,
            Modifier.weight(0.9f).fillMaxHeight(),
        )
    }
}

@Composable
private fun TallEditor(
    selected: Uri?, scale: Float, horizontal: Float, vertical: Float,
    filters: List<ThemeFilter>, panel: EditPanel,
    onChoose: () -> Unit, onScale: (Float) -> Unit, onHorizontal: (Float) -> Unit,
    onVertical: (Float) -> Unit, onToggleFilter: (ThemeFilter) -> Unit,
    onPanel: (EditPanel) -> Unit, onSave: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
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
                FilledTonalButton(onClick = onChoose, shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Choose", modifier = Modifier.padding(start = 8.dp))
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
    Surface(modifier = modifier, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FilterChip(
                    selected = panel == EditPanel.Crop,
                    onClick = { onPanel(EditPanel.Crop) },
                    label = { Text("Crop") },
                    leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = panel == EditPanel.Filters,
                    onClick = { onPanel(EditPanel.Filters) },
                    label = { Text("FX ${filters.size}") },
                    modifier = Modifier.weight(1f),
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
                                label = { Text(filter.label, maxLines = 1) },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
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
            ) { Text("Save") }
        }
    }
}

@Composable
private fun CompactSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            Text(String.format("%.2f", value), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onValue, valueRange = range, modifier = Modifier.fillMaxWidth().height(34.dp))
    }
}

@Composable
private fun ConnectScreen(viewModel: MiniPapeViewModel) {
    val address = remember { localAddress() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val coverMode = maxHeight < 600.dp
        if (coverMode) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(0.85f), horizontalAlignment = Alignment.CenterHorizontally) {
                    ImageIcon(Modifier.size(92.dp))
                    Text("Mac receiver", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Text("Same Wi‑Fi. Local only.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                ConnectionDetails(address, viewModel.pairCode, Modifier.weight(1.15f))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ImageIcon(Modifier.size(104.dp))
                Text("Preview from your Mac", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                ConnectionDetails(address, viewModel.pairCode, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ConnectionDetails(address: String, pairCode: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, contentDescription = null, Modifier.size(20.dp))
                Text("Connect", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
            }
            HorizontalDivider()
            Text("ADDRESS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$address:${ReceiverServer.PORT}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("PAIR CODE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(pairCode.chunked(3).joinToString(" "), fontSize = 31.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun localAddress(): String = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .flatMap { it.inetAddresses.toList() }
        .firstOrNull { it is Inet4Address && !it.isLoopbackAddress && it.isSiteLocalAddress }
        ?.hostAddress ?: "Unavailable"
}.getOrDefault("Unavailable")
