package com.sonara.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

class Accent(val name: String, val dark: Color, val light: Color)
class Wall(val name: String, val dark: Color, val light: Color)
class EqPreset(val name: String, val gains: FloatArray)

val ACCENTS = listOf(
    Accent("Violet", Color(0xFFB49AFF), Color(0xFF6558E8)),
    Accent("Blue", Color(0xFF82B1FF), Color(0xFF2F6BFF)),
    Accent("Pink", Color(0xFFFF9EC4), Color(0xFFD6336C)),
    Accent("Green", Color(0xFF7EE2A8), Color(0xFF1E9E5A)),
    Accent("Orange", Color(0xFFFFB86B), Color(0xFFE8710A)),
    Accent("Teal", Color(0xFF5EEAD4), Color(0xFF0F9D8F)),
    Accent("Red", Color(0xFFFF8A80), Color(0xFFD32F2F)),
)

val WALLS = listOf(
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
val SORT_NAMES = listOf("Recently added", "Title", "Artist", "Length")
val THEME_NAMES = listOf("Dark", "Light", "AMOLED", "System")

class Palette(
    val light: Boolean,
    val bg: Color,
    val panel: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val top: Color,
)

fun buildPalette(s: AppSettings, systemDark: Boolean): Palette {
    val light = when (s.theme) { 1 -> true; 3 -> !systemDark; else -> false }
    val amoled = s.theme == 2
    val acc = ACCENTS[s.accent.coerceIn(0, ACCENTS.lastIndex)]
    val wall = WALLS[s.wallpaper.coerceIn(0, WALLS.lastIndex)]
    val a = (0.92f - 0.65f * s.glass / 100f).coerceIn(0.2f, 0.95f)
    return if (light) {
        Palette(true, Color(0xFFF2F4FB), Color(0xFFFFFFFF).copy(alpha = a), Color(0xFF17182B), Color(0xFF777B91), acc.light, wall.light)
    } else if (amoled) {
        Palette(false, Color(0xFF000000), Color(0xFF121212).copy(alpha = a), Color(0xFFF5F4FF), Color(0xFF9A9DB5), acc.dark, Color(0xFF000000))
    } else {
        Palette(false, Color(0xFF090B16), Color(0xFF171A2B).copy(alpha = a), Color(0xFFF5F4FF), Color(0xFF9A9DB5), acc.dark, wall.dark)
    }
}

class AppSettings(ctx: Context) {
    private val sp = ctx.getSharedPreferences("sonara_settings", Context.MODE_PRIVATE)

    var theme by mutableIntStateOf(sp.getInt("theme", 0)); private set
    var accent by mutableIntStateOf(sp.getInt("accent", 0)); private set
    var wallpaper by mutableIntStateOf(sp.getInt("wallpaper", 0)); private set
    var visualizer by mutableIntStateOf(sp.getInt("visualizer", 0)); private set
    var eqPreset by mutableIntStateOf(sp.getInt("eq", 0)); private set
    var bass by mutableIntStateOf(sp.getInt("bass", 0)); private set
    var fadeSec by mutableIntStateOf(sp.getInt("fade", 0)); private set
    var sort by mutableIntStateOf(sp.getInt("sort", 0)); private set
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

    var shuffle: Boolean = sp.getBoolean("shuffle", false)
    var repeat: Int = sp.getInt("repeat", 0)
    var speed: Float = sp.getFloat("speed", 1f)

    fun update(key: String, v: Int) {
        when (key) {
            "theme" -> theme = v
            "accent" -> accent = v
            "wallpaper" -> wallpaper = v
            "visualizer" -> visualizer = v
            "eq" -> eqPreset = v
            "bass" -> bass = v
            "fade" -> fadeSec = v
            "sort" -> sort = v
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
        }
        sp.edit().putInt(key, v).apply()
    }

    fun savePlayback(shuffle: Boolean, repeat: Int, speed: Float) {
        this.shuffle = shuffle
        this.repeat = repeat
        this.speed = speed
        sp.edit().putBoolean("shuffle", shuffle).putInt("repeat", repeat).putFloat("speed", speed).apply()
    }
}
