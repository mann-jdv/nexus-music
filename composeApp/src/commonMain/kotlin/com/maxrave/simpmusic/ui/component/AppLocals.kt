package com.maxrave.simpmusic.ui.component

import androidx.compose.runtime.compositionLocalOf

// App-wide state App.kt provides to its screens. These lived in PromoBanner.kt, which Nexus Music
// deleted along with upstream's remote promo banners.

/** True while App.kt has a dialog of its own on screen. */
val LocalAppDialogOpen = compositionLocalOf { false }

/** True while App.kt's Now Playing sheet is open. */
val LocalNowPlayingOpen = compositionLocalOf { false }

/** True once the user has touched or scrolled anything in the app this launch, or opened Now Playing. */
val LocalUserStarted = compositionLocalOf { false }
