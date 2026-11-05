package com.maxrave.simpmusic.ui.component.glass

import com.maxrave.simpmusic.expect.ui.isLyricsBlurSupported

/**
 * Whether a backdrop blur can be drawn at all: Android 12+ (RenderEffect) and Desktop (skiko).
 *
 * Same answer as [isLyricsBlurSupported], which predates this package and is named after its first
 * user. Below Android 12 [GlassSurface] draws its solid translucent tint instead and never asks haze
 * to blur: haze's own pre-12 path goes through RenderScript, which crashes natively while one
 * blurred screen replaces another (see `barBlurStyle` in UIExt.kt).
 */
internal fun isGlassBlurSupported(): Boolean = isLyricsBlurSupported()

/**
 * Whether kyant backdrop's refraction (its `lens` effect) can run: Android 13+ (RuntimeShader) and
 * Desktop (SkikoRuntimeShader). Where this is false, a surface that asked for refraction falls back
 * to plain blur.
 */
expect fun isGlassRefractionSupported(): Boolean
