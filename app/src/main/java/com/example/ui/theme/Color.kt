package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =============================================================================
// Local Chat Brand Palette
// =============================================================================
val BrandTeal = Color(0xFF00A884)
val BrandTealDark = Color(0xFF008069)
val BrandTealLight = Color(0xFF25D366)
val BrandCyan = Color(0xFF06CF9C)

// =============================================================================
// Material 3 Light Mode Palette
// =============================================================================
val md_theme_light_primary = Color(0xFF006B56)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFF7EF8D5)
val md_theme_light_onPrimaryContainer = Color(0xFF002019)
val md_theme_light_inversePrimary = Color(0xFF5FDBBA)

val md_theme_light_secondary = Color(0xFF4C635B)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFCEE9DE)
val md_theme_light_onSecondaryContainer = Color(0xFF082019)

val md_theme_light_tertiary = Color(0xFF416375)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFC5E8FE)
val md_theme_light_onTertiaryContainer = Color(0xFF001F2A)

val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onErrorContainer = Color(0xFF410002)

val md_theme_light_background = Color(0xFFF4FBF7)
val md_theme_light_onBackground = Color(0xFF191C1B)
val md_theme_light_surface = Color(0xFFFBFDFA)
val md_theme_light_onSurface = Color(0xFF191C1B)
val md_theme_light_surfaceVariant = Color(0xFFDBE5E0)
val md_theme_light_onSurfaceVariant = Color(0xFF3F4945)
val md_theme_light_surfaceTint = Color(0xFF006B56)
val md_theme_light_inverseSurface = Color(0xFF2E3130)
val md_theme_light_inverseOnSurface = Color(0xFFEFF1EE)

val md_theme_light_surfaceDim = Color(0xFFD8DBD8)
val md_theme_light_surfaceBright = Color(0xFFF8FAF7)
val md_theme_light_surfaceContainerLowest = Color(0xFFFFFFFF)
val md_theme_light_surfaceContainerLow = Color(0xFFF2F5F2)
val md_theme_light_surfaceContainer = Color(0xFFECEFEC)
val md_theme_light_surfaceContainerHigh = Color(0xFFE6E9E6)
val md_theme_light_surfaceContainerHighest = Color(0xFFE1E4E1)

val md_theme_light_outline = Color(0xFF6F7975)
val md_theme_light_outlineVariant = Color(0xFFBFC9C4)
val md_theme_light_scrim = Color(0xFF000000)

// =============================================================================
// Material 3 Dark Mode Palette
// =============================================================================
val md_theme_dark_primary = Color(0xFF5FDBBA)
val md_theme_dark_onPrimary = Color(0xFF00382C)
val md_theme_dark_primaryContainer = Color(0xFF005140)
val md_theme_dark_onPrimaryContainer = Color(0xFF7EF8D5)
val md_theme_dark_inversePrimary = Color(0xFF006B56)

val md_theme_dark_secondary = Color(0xFFB2CCC2)
val md_theme_dark_onSecondary = Color(0xFF1E352D)
val md_theme_dark_secondaryContainer = Color(0xFF354C43)
val md_theme_dark_onSecondaryContainer = Color(0xFFCEE9DE)

val md_theme_dark_tertiary = Color(0xFFA9CCE1)
val md_theme_dark_onTertiary = Color(0xFF0E3445)
val md_theme_dark_tertiaryContainer = Color(0xFF284B5D)
val md_theme_dark_onTertiaryContainer = Color(0xFFC5E8FE)

val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)

val md_theme_dark_background = Color(0xFF0B141B)
val md_theme_dark_onBackground = Color(0xFFE1E3E0)
val md_theme_dark_surface = Color(0xFF111B21)
val md_theme_dark_onSurface = Color(0xFFE1E3E0)
val md_theme_dark_surfaceVariant = Color(0xFF202C33)
val md_theme_dark_onSurfaceVariant = Color(0xFFBFC9C4)
val md_theme_dark_surfaceTint = Color(0xFF5FDBBA)
val md_theme_dark_inverseSurface = Color(0xFFE1E3E0)
val md_theme_dark_inverseOnSurface = Color(0xFF191C1B)

val md_theme_dark_surfaceDim = Color(0xFF0B141B)
val md_theme_dark_surfaceBright = Color(0xFF353D43)
val md_theme_dark_surfaceContainerLowest = Color(0xFF070E13)
val md_theme_dark_surfaceContainerLow = Color(0xFF0F181F)
val md_theme_dark_surfaceContainer = Color(0xFF142028)
val md_theme_dark_surfaceContainerHigh = Color(0xFF1E2B34)
val md_theme_dark_surfaceContainerHighest = Color(0xFF283640)

val md_theme_dark_outline = Color(0xFF89938F)
val md_theme_dark_outlineVariant = Color(0xFF3F4945)
val md_theme_dark_scrim = Color(0xFF000000)

// =============================================================================
// Direct / Legacy Tokens (Maintained for full backward compatibility)
// =============================================================================
val LightBg = Color(0xFFF0F2F5)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEFEAE2)
val LightTextPrimary = Color(0xFF111B21)
val LightTextSecondary = Color(0xFF667781)
val LightOutgoingBubble = Color(0xFFD9FDD3)
val LightIncomingBubble = Color(0xFFFFFFFF)
val LightDivider = Color(0xFFE9EDEF)

val DarkBg = Color(0xFF0B141B)
val DarkSurface = Color(0xFF111B21)
val DarkSurfaceVariant = Color(0xFF202C33)
val DarkTextPrimary = Color(0xFFE9EDEF)
val DarkTextSecondary = Color(0xFF8696A0)
val DarkOutgoingBubble = Color(0xFF005C4B)
val DarkIncomingBubble = Color(0xFF202C33)
val DarkDivider = Color(0xFF222D34)

// Indicators & Accents
val TickBlue = Color(0xFF53BDEB)
val TickGrey = Color(0xFF8696A0)
val CallMissedRed = Color(0xFFF15C6D)
val CallSuccessGreen = Color(0xFF00A884)
val StarYellow = Color(0xFFFFB800)

// =============================================================================
// Extended Semantic Chat Colors
// =============================================================================
@Immutable
data class ChatColors(
    val outgoingBubble: Color,
    val incomingBubble: Color,
    val onOutgoingBubble: Color,
    val onIncomingBubble: Color,
    val outgoingTimestamp: Color,
    val incomingTimestamp: Color,
    val tickBlue: Color,
    val tickGrey: Color,
    val callMissed: Color,
    val callSuccess: Color,
    val starYellow: Color,
    val onlineGreen: Color,
    val chatWallpaperBackground: Color,
    val chatWallpaperPattern: Color,
    val unreadBadge: Color,
    val onUnreadBadge: Color,
    val messageSelectionOverlay: Color
)

val LightChatColors = ChatColors(
    outgoingBubble = LightOutgoingBubble,
    incomingBubble = LightIncomingBubble,
    onOutgoingBubble = Color(0xFF111B21),
    onIncomingBubble = Color(0xFF111B21),
    outgoingTimestamp = Color(0xFF667781),
    incomingTimestamp = Color(0xFF667781),
    tickBlue = TickBlue,
    tickGrey = TickGrey,
    callMissed = CallMissedRed,
    callSuccess = CallSuccessGreen,
    starYellow = StarYellow,
    onlineGreen = Color(0xFF25D366),
    chatWallpaperBackground = Color(0xFFEFEAE2),
    chatWallpaperPattern = Color(0x0C000000),
    unreadBadge = BrandTeal,
    onUnreadBadge = Color.White,
    messageSelectionOverlay = Color(0x3300A884)
)

val DarkChatColors = ChatColors(
    outgoingBubble = DarkOutgoingBubble,
    incomingBubble = DarkIncomingBubble,
    onOutgoingBubble = Color(0xFFE9EDEF),
    onIncomingBubble = Color(0xFFE9EDEF),
    outgoingTimestamp = Color(0xFF8696A0),
    incomingTimestamp = Color(0xFF8696A0),
    tickBlue = TickBlue,
    tickGrey = TickGrey,
    callMissed = CallMissedRed,
    callSuccess = CallSuccessGreen,
    starYellow = StarYellow,
    onlineGreen = Color(0xFF25D366),
    chatWallpaperBackground = Color(0xFF0B141B),
    chatWallpaperPattern = Color(0x14FFFFFF),
    unreadBadge = BrandTeal,
    onUnreadBadge = Color.White,
    messageSelectionOverlay = Color(0x4000A884)
)

val LocalChatColors = staticCompositionLocalOf { LightChatColors }
