package com.rimor.minipape

import android.content.Context

object MiniPapeRuntime {
    lateinit var repository: WallpaperRepository
        private set

    fun initialize(context: Context) {
        if (::repository.isInitialized) return
        repository = WallpaperRepository(context.applicationContext)
    }
}
