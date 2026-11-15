package com.sonara.player

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

class AppTheme(
    val name: String,
    val light: Boolean,
    val bg: Color,
    val surface: Color,
    val accent: Color,
    val accent2: Color,
    val comp1: Color,
    val comp2: Color,
    val text: Color,
)
class Wall(val name: String, val dark: Color, val light: Color)
class EqPreset(val name: String, val gains: FloatArray)

val THEMES = listOf(
    AppTheme("Pastel Lavender & Cream", true, Color(0xFFFAF8FC), Color(0xFFEFEAF5), Color(0xFF9D8CB8), Color(0xFFD4C7E2), Color(0xFF8CB8A8), Color(0xFFD9A79C), Color(0xFF2F2938)),
    AppTheme("Rosewood Clay & Warm Beige", true, Color(0xFFF9F5F2), Color(0xFFEFE6E0), Color(0xFFB8867B), Color(0xFFD9BDB3), Color(0xFF8AA399), Color(0xFFCCA476), Color(0xFF382B28)),
    AppTheme("Buttercream & Warm Oat", true, Color(0xFFFCFBF7), Color(0xFFF3EFE4), Color(0xFFC4A97E), Color(0xFFDFD3BE), Color(0xFF969E7E), Color(0xFFC28674), Color(0xFF363128)),
    AppTheme("Moody Plum & Charcoal", false, Color(0xFF19171E), Color(0xFF24212C), Color(0xFF9C7CA5), Color(0xFF594961), Color(0xFFC99A9E), Color(0xFF7EA8A4), Color(0xFFECE9F0)),
    AppTheme("Warm Espresso & Taupe", false, Color(0xFF1B1715), Color(0xFF27221F), Color(0xFFB89B84), Color(0xFF6E5B4F), Color(0xFFCFA16D), Color(0xFF8A9E86), Color(0xFFEFECE9)),
    AppTheme("Midnight Teal & Slate", false, Color(0xFF131B1E), Color(0xFF1D282C), Color(0xFF64979E), Color(0xFF3D5B61), Color(0xFFD49488), Color(0xFFC9B98B), Color(0xFFE4ECEE)),
)

val WALLS = listOf(
    Wall("Theme", Color.Unspecified, Color.Unspecified),
    Wall("Aurora", Color(0xFF27204D), Color(0xFFE5E1FF)),
    Wall("Ocean", Color(0xFF0F2B46), Color(0xFFDCEBFF)),
    Wall("Sunset", Color(0xFF4A1F3D), Color(0xFFFFE3D6)),
    Wall("Forest", Color(0xFF123524), Color(0xFFDDF3E4)),
    Wall("Rose", Color(0xFF4A1830), Color(0xFFFFE0EC)),
    Wall("Mono", Color(0xFF22232B), Color(0xFFE9EAEE)),
)

val EQ_PRESETS = listOf(
    EqPreset("Flat", floatArrayOf(0f, 0f, 0f, 0f, 0f)),
    EqPreset("Bass boost", floatArrayOf(6f, 4f, 0f, 0f, 0f)),
    EqPreset("Treble boost", floatArrayOf(0f, 0f, 0f, 4f, 6f)),
    EqPreset("Vocal", floatArrayOf(-2f, 0f, 4f, 3f, 0f)),
    EqPreset("Rock", floatArrayOf(5f, 3f, -1f, 3f, 5f)),
    EqPreset("Pop", floatArrayOf(-1f, 3f, 4f, 2f, -1f)),
    EqPreset("Jazz", floatArrayOf(3f, 2f, -2f, 2f, 4f)),
    EqPreset("Hip-hop", floatArrayOf(6f, 4f, 0f, 1f, 3f)),
    EqPreset("Electronic", floatArrayOf(5f, 3f, 0f, 3f, 5f)),
    EqPreset("Classical", floatArrayOf(3f, 2f, 0f, 2f, 3f)),
)

val VIZ_NAMES = listOf("Bars", "Wave", "Dots", "Pulse")
val SORT_NAMES = listOf("Recently added", "Title", "Artist", "Length", "Album", "Folder", "Most played", "Favorites first")

fun sortLabel(mode: Int, desc: Boolean): String = when (mode) {
    1 -> if (desc) "Title Z–A" else "Title A–Z"
    2 -> if (desc) "Artist Z–A" else "Artist A–Z"
    3 -> if (desc) "Longest first" else "Shortest first"
    4 -> if (desc) "Album Z–A" else "Album A–Z"
    5 -> if (desc) "Folder Z–A" else "Folder A–Z"
    6 -> if (desc) "Least played" else "Most played"
    7 -> if (desc) "Favorites last" else "Favorites first"
    else -> if (desc) "Oldest first" else "Newest first"
}

class Palette(
    val light: Boolean,
    val bg: Color,
    val panel: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val top: Color,
    val accent2: Color,
    val comp1: Color,
    val comp2: Color,
    val glow: Color,
    val glow2: Color,
    val onAccent: Color,
)

fun wallTint(t: AppTheme, wallIdx: Int): Color {
    val w = WALLS[wallIdx.coerceIn(0, WALLS.lastIndex)]
    return if (wallIdx == 0) t.accent2 else if (t.light) w.light else w.dark
}

/**
 * Builds the colors for the chosen palette.
 * songColor tints the main glow (and optionally the accent); page (0 all, 1 favorites,
 * 2 playlist, 3 folder) shifts the second glow through the palette's complementary accents.
 */
fun buildPalette(s: AppSettings, songColor: Color?, page: Int = 0): Palette {
    val t = THEMES[s.palette.coerceIn(0, THEMES.lastIndex)]
    val a = (0.92f - 0.65f * s.glass / 100f).coerceIn(0.2f, 0.95f)
    val accent = if (s.dynAccent == 1 && songColor != null) lerp(t.accent, songColor, 0.7f) else t.accent
    val glow = if (s.songGlow == 1 && songColor != null) songColor else t.accent
    val glow2 = if (s.pageTint == 1) {
        when (page) {
            1 -> t.comp1
            2 -> t.comp2
            3 -> lerp(t.comp1, t.comp2, 0.5f)
            else -> t.accent2
        }
    } else t.accent2
    val onAccent = if (accent.luminance() > 0.4f) Color(0xFF1B1620) else Color.White
    return Palette(
        light = t.light,
        bg = t.bg,
        panel = t.surface.copy(alpha = a),
        text = t.text,
        muted = t.text.copy(alpha = 0.62f),
        accent = accent,
        top = lerp(t.bg, wallTint(t, s.wallpaper), 0.65f),
        accent2 = t.accent2,
        comp1 = t.comp1,
        comp2 = t.comp2,
        glow = glow,
        glow2 = glow2,
        onAccent = onAccent,
    )
}

/** Cross-fades every color so switching themes, pages or songs shifts the whole screen smoothly. */
@Composable
fun animatedPalette(t: Palette): Palette {
    val spec = tween<Color>(650)
    val slow = tween<Color>(900)
    val bg by animateColorAsState(t.bg, spec, label = "pbg")
    val panel by animateColorAsState(t.panel, spec, label = "ppanel")
    val text by animateColorAsState(t.text, spec, label = "ptext")
    val muted by animateColorAsState(t.muted, spec, label = "pmuted")
    val accent by animateColorAsState(t.accent, spec, label = "paccent")
    val top by animateColorAsState(t.top, spec, label = "ptop")
    val accent2 by animateColorAsState(t.accent2, spec, label = "paccent2")
    val comp1 by animateColorAsState(t.comp1, spec, label = "pcomp1")
    val comp2 by animateColorAsState(t.comp2, spec, label = "pcomp2")
    val glow by animateColorAsState(t.glow, slow, label = "pglow")
    val glow2 by animateColorAsState(t.glow2, slow, label = "pglow2")
    val onAccent by animateColorAsState(t.onAccent, spec, label = "pon")
    return Palette(t.light, bg, panel, text, muted, accent, top, accent2, comp1, comp2, glow, glow2, onAccent)
}

class AppSettings(ctx: Context, private val persist: Boolean = true) {
    private val sp = ctx.getSharedPreferences("sonara_settings", Context.MODE_PRIVATE)

    var palette by mutableIntStateOf(sp.getInt("palette", 3)); private set
    var songGlow by mutableIntStateOf(sp.getInt("songglow", 1)); private set
    var dynAccent by mutableIntStateOf(sp.getInt("dynaccent", 0)); private set
    var wallpaper by mutableIntStateOf(sp.getInt("wallpaper", 0)); private set
    var visualizer by mutableIntStateOf(sp.getInt("visualizer", 0)); private set
    var eqPreset by mutableIntStateOf(sp.getInt("eq", 0)); private set
    var bass by mutableIntStateOf(sp.getInt("bass", 0)); private set
    var fadeSec by mutableIntStateOf(sp.getInt("fade", 0)); private set
    var sort by mutableIntStateOf(sp.getInt("sort", 0)); private set
    var sortDesc by mutableIntStateOf(sp.getInt("sortdesc", 0)); private set
    var reactive by mutableIntStateOf(sp.getInt("reactive", 0)); private set
    var minLenSec by mutableIntStateOf(sp.getInt("minlen", 30)); private set
    var vizBars by mutableIntStateOf(sp.getInt("vizbars", 32)); private set
    var vizColor by mutableIntStateOf(sp.getInt("vizcolor", 0)); private set
    var vizSens by mutableIntStateOf(sp.getInt("vizsens", 1)); private set
    var vizHeight by mutableIntStateOf(sp.getInt("vizheight", 1)); private set
    var vizThick by mutableIntStateOf(sp.getInt("vizthick", 1)); private set
    var vizShape by mutableIntStateOf(sp.getInt("vizshape", 0)); private set
    var vizMirror by mutableIntStateOf(sp.getInt("vizmirror", 0)); private set
    var spin by mutableIntStateOf(sp.getInt("spin", 1)); private set
    var folderSep by mutableIntStateOf(sp.getInt("foldersep", 1)); private set
    var glass by mutableIntStateOf(sp.getInt("glass", 25)); private set
    var wallType by mutableIntStateOf(sp.getInt("walltype", 0)); private set
    var wallBlur by mutableIntStateOf(sp.getInt("wallblur", 12)); private set
    var wallDim by mutableIntStateOf(sp.getInt("walldim", 35)); private set
    var wallZoom by mutableIntStateOf(sp.getInt("wallzoom", 100)); private set
    var wallX by mutableIntStateOf(sp.getInt("wallx", 50)); private set
    var wallY by mutableIntStateOf(sp.getInt("wally", 50)); private set
    var pageTint by mutableIntStateOf(sp.getInt("pagetint", 1)); private set

    var shuffle: Boolean = sp.getBoolean("shuffle", false)
    var repeat: Int = sp.getInt("repeat", 0)
    var speed: Float = sp.getFloat("speed", 1f)

    fun update(key: String, v: Int) {
        when (key) {
            "palette" -> palette = v
            "songglow" -> songGlow = v
            "dynaccent" -> dynAccent = v
            "wallpaper" -> wallpaper = v
            "visualizer" -> visualizer = v
            "eq" -> eqPreset = v
            "bass" -> bass = v
            "fade" -> fadeSec = v
            "sort" -> sort = v
            "sortdesc" -> sortDesc = v
            "reactive" -> reactive = v
            "minlen" -> minLenSec = v
            "vizbars" -> vizBars = v
            "vizcolor" -> vizColor = v
            "vizsens" -> vizSens = v
            "vizheight" -> vizHeight = v
            "vizthick" -> vizThick = v
            "vizshape" -> vizShape = v
            "vizmirror" -> vizMirror = v
            "spin" -> spin = v
            "foldersep" -> folderSep = v
            "glass" -> glass = v
            "walltype" -> wallType = v
            "wallblur" -> wallBlur = v
            "walldim" -> wallDim = v
            "wallzoom" -> wallZoom = v
            "wallx" -> wallX = v
            "wally" -> wallY = v
            "pagetint" -> pageTint = v
        }
        if (persist) sp.edit().putInt(key, v).apply()
    }

    /** Copies every appearance value from a draft (the Customize window) into these live settings. */
    fun applyFrom(d: AppSettings) {
        update("palette", d.palette); update("songglow", d.songGlow); update("dynaccent", d.dynAccent)
        update("pagetint", d.pageTint)
        update("wallpaper", d.wallpaper); update("walltype", d.wallType)
        update("wallblur", d.wallBlur); update("walldim", d.wallDim); update("glass", d.glass)
        update("wallzoom", d.wallZoom); update("wallx", d.wallX); update("wally", d.wallY)
        update("spin", d.spin); update("visualizer", d.visualizer)
        update("vizbars", d.vizBars); update("vizcolor", d.vizColor); update("vizsens", d.vizSens)
        update("vizheight", d.vizHeight); update("vizthick", d.vizThick)
        update("vizshape", d.vizShape); update("vizmirror", d.vizMirror)
    }

    fun savePlayback(shuffle: Boolean, repeat: Int, speed: Float) {
        this.shuffle = shuffle
        this.repeat = repeat
        this.speed = speed
        sp.edit().putBoolean("shuffle", shuffle).putInt("repeat", repeat).putFloat("speed", speed).apply()
    }
}
