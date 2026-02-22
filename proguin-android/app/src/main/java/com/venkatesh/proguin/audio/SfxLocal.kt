package com.venkatesh.proguin.audio

import androidx.compose.runtime.staticCompositionLocalOf

val LocalSfx = staticCompositionLocalOf<SfxManager> {
    error("LocalSfx not provided")
}
