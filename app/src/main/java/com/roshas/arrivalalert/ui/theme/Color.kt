package com.roshas.arrivalalert.ui.theme

import androidx.compose.ui.graphics.Color

// Near-black canvas. The design tokens and the design prose disagree here
// (#121414 vs #050505); this is what the reference screens actually render.
val Background = Color(0xFF121414)

// Electric violet, the one accent. Active states only: switch thumb and track,
// slider track and thumb, focused field underline, the distance readout.
val Accent = Color(0xFF8F00FF)
val OnAccent = Color(0xFFFFFFFF)

val Foreground = Color(0xFFE3E2E2)
val Muted = Color(0xFF757575)
val Divider = Color(0xFF2A2A2A)

// The sheet lifts very slightly off the canvas rather than using elevation.
val SheetBackground = Color(0xFF0D0E0F)

val Destructive = Color(0xFFFFB4AB)
