package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Extreme AMOLED Black & Red Identity Palette
val AmoledBlack = Color(0xFF000000)
val ObsidianGlassDeep = Color(0xCC060608)
val ObsidianGlassSurface = Color(0x990A0A0D)
val ObsidianGlassElevated = Color(0xB3101014)

val LiveRed = Color(0xFFFF1744)
val LiveRedSoft = Color(0x99FF1744)
val LiveRedSubtle = Color(0x33FF1744)
val Crimson = Color(0xFFB00020)
val CrimsonGlow = Color(0x55B00020)
val DeepRed = Color(0xFF650014)
val DeepRedAura = Color(0x40650014)

val HighlightWhite = Color(0xFFFFFFFF)
val SecondaryText = Color(0xFFA1A1AA)
val MutedText = Color(0xFF71717A)

// Optical Glass Material Layers (iOS 26 inspired)
val GlassSpecularBright = Color(0xE6FFFFFF)      // Crisp top Fresnel edge
val GlassSpecularSoft = Color(0x59FFFFFF)        // Soft sheen reflection
val GlassBodyTop = Color(0x22FFFFFF)             // Upper glass transmission
val GlassBodyMid = Color(0x10FFFFFF)             // Mid glass body
val GlassBodyBottom = Color(0x07FFFFFF)          // Lower glass fade
val GlassInnerShadow = Color(0xB3000000)         // Recessed depth shadow
val GlassRecessedWell = Color(0xCC050507)        // Smoked dark lens well
val GlassHairlineWhite = Color(0x2EFFFFFF)       // Delicate neutral glass rim
val GlassHairlineRed = Color(0x99FF1744)         // Illuminated red edge reflection
val GlassHairlineCrimson = Color(0x66B00020)     // Resting crimson rim

// Subtle Controlled Chromatic Dispersion Accents (low opacity edge optics)
val ChromaticRedEdge = Color(0x38FF1744)
val ChromaticCyanEdge = Color(0x1A7DD3FC)

// Semantic Status Colors (controlled, high-contrast)
val StatusReadyGreen = Color(0xFF34D399)
val StatusReadyBg = Color(0x2434D399)
val StatusErrorRed = LiveRed
val StatusErrorBg = Color(0x2EFF1744)

// Compatibility aliases for existing component references
val DarkBackground = AmoledBlack
val DarkSurface = ObsidianGlassSurface
val DarkSurfaceVariant = ObsidianGlassElevated
val NeonRed = LiveRed
val NeonRedBright = LiveRed
val NeonRedDim = Crimson
val NeonRedGlow = LiveRedSoft
val AccentOrange = Color(0xFFE4E4E7) // Refined platinum accent for secondary metrics
val LaserCyan = HighlightWhite
val LiquidGlassBorder = GlassHairlineWhite
val LiquidGlassBorderRed = GlassHairlineRed
val GlassBorderRed = GlassHairlineRed
val GlassBorderWhite = GlassHairlineWhite
val TextWhitePrimary = HighlightWhite
val TextWhiteSecondary = Color(0xFFE4E4E7)
val TextGraySecondary = SecondaryText
val TextGrayMuted = MutedText

// Hardware Matrix LED Display Colors (Authentic Minecraft OSC terminal & punch-card parity)
val LedRedOn = Color(0xFFFF1744)
val LedRedOff = Color(0xFF170B0E)
val LedBlueOn = Color(0xFF38BDF8)
val LedOrangeOn = Color(0xFFFF8A00)

val ColBlueDark = Color(0xFF1E3A8A)
val ColBlueLight = Color(0xFF38BDF8)
val ColGreen = Color(0xFF16A34A)
val ColYellow = Color(0xFFEAB308)
val ColOrange = Color(0xFFEA580C)
val ColRed = Color(0xFFDC2626)
val ColBrown = Color(0xFF854D0E)

val Row1 = Color(0xFF27272A)
val Row2 = Color(0xFF3F3F46)
val Row3 = Color(0xFF52525B)
val Row4 = Color(0xFF71717A)
val Row5 = Color(0xFFA1A1AA)
