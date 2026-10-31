package io.github.gamesbyian.glimpse.domain

/**
 * Pure platform motion preference. Invalid animator scales are treated
 * conservatively: no animated settling until a valid preference is available.
 */
internal fun shouldReduceReelMotion(animatorScale: Float, powerSaver: Boolean): Boolean =
    powerSaver || !animatorScale.isFinite() || animatorScale <= 0f
