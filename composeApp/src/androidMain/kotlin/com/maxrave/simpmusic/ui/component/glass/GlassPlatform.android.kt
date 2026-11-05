package com.maxrave.simpmusic.ui.component.glass

import android.os.Build

// kyant backdrop's lens is an AGSL RuntimeShader, added in API 33 (Android 13, TIRAMISU).
actual fun isGlassRefractionSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
