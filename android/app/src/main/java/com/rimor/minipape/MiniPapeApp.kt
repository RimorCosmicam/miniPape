package com.rimor.minipape

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.net.Inet4Address
import java.net.NetworkInterface

private enum class Destination(val label: String) {
    Gallery("Gallery"), Create("Create"), Connect("Connect")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPapeApp(viewModel: MiniPapeViewModel = viewModel()) {
    var destination by remember { mutableStateOf(Destination.Gallery) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("miniPape", fontWeight = FontWeight.SemiBold) }) },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { item ->
                    val icon = when (item) {
                        Destination.Gallery -> Icons.Default.Collections
                        Destination.Create -> Icons.Default.Edit
                        Destination.Connect -> Icons.Default.Devices
                    }
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        AnimatedContent(destination, modifier = Modifier.padding(padding), label = "destination") { screen ->
            when (screen) {
                Destination.Gallery -> GalleryScreen(viewModel)
                Destination.Create -> CreateScreen(viewModel)
                Destination.Connect -> ConnectScreen(viewModel)
            }
        }
    }
}

@Composable
private fun GalleryScreen(viewModel: MiniPapeViewModel) {
    val wallpapers by viewModel.wallpapers.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(156.dp),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (preview.source != null) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Live from Mac", style = MaterialTheme.typography.titleLarge)
                    WallpaperSurface(preview, Modifier.fillMaxWidth().aspectRatio(1048f / 948f))
                }
            }
        }
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Text("Wallpapers", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        }
        items(wallpapers, key = { it.file.absolutePath }) { item ->
            Card(shape = RoundedCornerShape(28.dp)) {
                Column {
                    WallpaperSurface(
                        PreviewSession(source = item.file, mediaKind = item.mediaKind, recipe = item.recipe),
                        Modifier.fillMaxWidth().aspectRatio(1048f / 948f),
                    )
                    Text(item.displayName, modifier = Modifier.padding(14.dp), maxLines = 1)
                }
            }
        }
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
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { selected = it }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected == null) {
                LargeFloatingActionButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                    shape = CircleShape,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Choose media")
                    }
                }
            } else {
                ThemeFilterStack(filters, Modifier.fillMaxSize()) {
                    coil3.compose.AsyncImage(
                        model = selected,
                        contentDescription = "Crop preview",
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = horizontal * size.width * 0.5f
                            translationY = vertical * size.height * 0.5f
                        },
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                }
            }
        }
        Text("Scale")
        Slider(value = scale, onValueChange = { scale = it }, valueRange = 1f..4f)
        Text("Position")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Slider(value = horizontal, onValueChange = { horizontal = it }, valueRange = -1f..1f, modifier = Modifier.weight(1f))
            Slider(value = vertical, onValueChange = { vertical = it }, valueRange = -1f..1f, modifier = Modifier.weight(1f))
        }
        Text("Filters · stack in selection order")
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeFilter.entries.forEach { filter ->
                FilterChip(
                    selected = filter in filters,
                    onClick = {
                        filters = if (filter in filters) filters - filter else filters + filter
                    },
                    label = { Text(filter.label) },
                )
            }
        }
        FilledTonalButton(
            onClick = {
                selected?.let {
                    viewModel.importFromPhone(
                        context,
                        it,
                        CropRecipe(scale = scale, offsetX = horizontal, offsetY = vertical, filters = filters),
                    )
                }
            },
            enabled = selected != null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(20.dp),
        ) { Text("Save to gallery") }
    }
}

@Composable
private fun ConnectScreen(viewModel: MiniPapeViewModel) {
    val address = remember { localAddress() }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(52.dp))
        Text("Preview from your Mac", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Text("Keep both devices on the same Wi‑Fi network, then enter this address and pair code in miniPape for Mac.")
        HorizontalDivider()
        Text("Address", style = MaterialTheme.typography.labelLarge)
        Text("$address:${ReceiverServer.PORT}", style = MaterialTheme.typography.headlineSmall)
        Text("Pair code", style = MaterialTheme.typography.labelLarge)
        Text(viewModel.pairCode.chunked(3).joinToString(" "), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text("Receiver stays local. The code changes when miniPape restarts.", style = MaterialTheme.typography.bodySmall)
    }
}

private fun localAddress(): String = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .flatMap { it.inetAddresses.toList() }
        .firstOrNull { it is Inet4Address && !it.isLoopbackAddress && it.isSiteLocalAddress }
        ?.hostAddress ?: "Unavailable"
}.getOrDefault("Unavailable")
