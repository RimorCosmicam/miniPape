package com.rimor.minipape

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MiniPapeViewModel : ViewModel() {
    private val repository = MiniPapeRuntime.repository
    val preview = repository.preview.stateIn(viewModelScope, SharingStarted.Eagerly, PreviewSession())
    val wallpapers = repository.wallpapers.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val pairCode: String get() = MiniPapeRuntime.server.pairCode

    fun importFromPhone(context: Context, uri: Uri, displayName: String = "phone-wallpaper") {
        viewModelScope.launch(Dispatchers.IO) {
            val extension = context.contentResolver.getType(uri)?.substringAfterLast('/') ?: "bin"
            val file = repository.wallpaperFile("$displayName.$extension")
            context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use(input::copyTo) }
            repository.addWallpaper(file, if (extension.contains("video")) "video" else "image")
        }
    }
}

