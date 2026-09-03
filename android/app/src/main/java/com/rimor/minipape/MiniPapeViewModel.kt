package com.rimor.minipape

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MiniPapeViewModel : ViewModel() {
    private val repository = MiniPapeRuntime.repository
    private val _save = MutableStateFlow<SaveState>(SaveState.Idle)

    val save = _save.asStateFlow()

    fun clearSave() {
        if (_save.value !is SaveState.Working) _save.value = SaveState.Idle
    }

    /**
     * Cuts the framed media down to the cover canvas, keeps it as the current cover wallpaper and
     * copies it out to Pictures/miniPape so a gallery can open it.
     */
    fun save(context: Context, uri: Uri, recipe: CropRecipe) {
        if (_save.value is SaveState.Working) return
        _save.value = SaveState.Working
        val application = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            _save.value = runCatching {
                val cut = MediaExport.cut(application, uri, recipe, repository.outputDirectory)
                repository.setCutWallpaper(cut.file, cut.mediaKind, recipe.loop)
                SaveState.Saved(MediaExport.publish(application, cut), cut.mimeType)
            }.getOrElse { failure ->
                SaveState.Failed(failure.message?.takeIf(String::isNotBlank) ?: "That media could not be cut")
            }
        }
    }
}
