package com.rimor.minipape

import android.app.Application

class MiniPapeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MiniPapeRuntime.initialize(this)
    }
}

