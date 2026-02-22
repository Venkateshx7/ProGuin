package com.venkatesh.proguin.voice

import androidx.compose.runtime.staticCompositionLocalOf

val LocalNarrator = staticCompositionLocalOf<Narrator> {
    error("LocalNarrator not provided")
}
