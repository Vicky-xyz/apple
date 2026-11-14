package com.sonara.player

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val player = PlayerHolder.get(this)
        val settings = AppSettings(this)
        if (player.mediaItemCount == 0) {
            player.shuffleModeEnabled = settings.shuffle
            player.repeatMode = settings.repeat
            player.setPlaybackSpeed(settings.speed)
        }
        PlayerHolder.fadeMs = settings.fadeSec * 1000L
        PlayerHolder.setEffects(settings.eqPreset, settings.bass)
        setContent { SonaraApp(settings, player) }
    }
}

/** Mirrors ExoPlayer state into Compose state. */
class PlayerUi(private val p: ExoPlayer) : Player.Listener {
    var mediaId by mutableStateOf<String?>(p.currentMediaItem?.mediaId)
    var playing by mutableStateOf(p.isPlaying)
    var shuffle by mutableStateOf(p.shuffleModeEnabled)
    var repeat by mutableIntStateOf(p.repeatMode)
    var speed by mutableFloatStateOf(p.playbackParameters.speed)

    init { p.addListener(this) }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { mediaId = mediaItem?.mediaId }
    override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { shuffle = shuffleModeEnabled }
    override fun onRepeatModeChanged(repeatMode: Int) { repeat = repeatMode }
    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) { speed = playbackParameters.speed }
    fun dispose() { p.removeListener(this) }
}

private enum class Sheet { None, Settings, Sound, Sleep, Lyrics }

private val ON_ACCENT = Color(0xFF171426)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SonaraApp(settings: AppSettings, player: ExoPlayer) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val pal = buildPalette(settings, isSystemInDarkTheme())
    val act = ctx as ComponentActivity

    LaunchedEffect(pal.light) {
        val t = android.graphics.Color.TRANSPARENT
        val style = if (pal.light) SystemBarStyle.light(t, t) else SystemBarStyle.dark(t)
        act.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    var songs by remember { mutableStateOf(Library.load(ctx)) }
    var query by remember { mutableStateOf("") }
    var favOnly by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(Sheet.None) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var drag by remember { mutableStateOf<Float?>(null) }
    var art by remember { mutableStateOf<ImageBitmap?>(null) }
    var lyrics by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var menuFor by remember { mutableStateOf<String?>(null) }

    val ui = remember { PlayerUi(player) }
    DisposableEffect(Unit) { onDispose { ui.dispose() } }

    val current = songs.firstOrNull { it.uri == ui.mediaId }

    val visible = remember(songs, query, favOnly, settings.sort) {
        songs
            .filter {
                (!favOnly || it.fav) &&
                    (query.isBlank() || it.title.contains(query, true) || it.artist.contains(query, true))
            }
            .let { l ->
                when (settings.sort) {
                    1 -> l.sortedBy { it.title.lowercase() }
                    2 -> l.sortedBy { it.artist.lowercase() }
                    3 -> l.sortedBy { it.durationMs }
                    else -> l
                }
            }
    }

    LaunchedEffect(player) {
        while (true) {
            pos = player.currentPosition.coerceAtLeast(0)
            dur = player.duration.takeIf { it > 0 } ?: 0L
            delay(250)
        }
    }

    LaunchedEffect(current?.uri) {
        art = null
        lyrics = emptyList()
        val s = current ?: return@LaunchedEffect
        lyrics = withContext(Dispatchers.IO) { Library.loadLyrics(ctx, s.uri) }
        val bytes = withContext(Dispatchers.IO) { Library.art(ctx, s.uri) } ?: return@LaunchedEffect
        art = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        val cur = player.currentMediaItem
        if (cur != null && cur.mediaId == s.uri && cur.mediaMetadata.artworkData == null) {
            val meta = cur.mediaMetadata.buildUpon()
                .setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                .build()
            player.replaceMediaItem(player.currentMediaItemIndex, cur.buildUpon().setMediaMetadata(meta).build())
        }
    }

    fun persist(list: List<Song>) {
        songs = list
        Library.save(ctx, list)
    }

    fun ensureService() {
        try { ctx.startService(Intent(ctx, PlaybackService::class.java)) } catch (_: Exception) {}
    }

    fun play(index: Int) {
        val item = visible.getOrNull(index) ?: return
        if (item.uri == ui.mediaId) {
            if (player.isPlaying) player.pause() else { player.play(); ensureService() }
            return
        }
        player.setMediaItems(visible.map { it.toMediaItem() }, index, 0L)
        player.prepare()
        player.play()
        ensureService()
    }

    fun remove(s: Song) {
        for (i in player.mediaItemCount - 1 downTo 0) {
            if (player.getMediaItemAt(i).mediaId == s.uri) player.removeMediaItem(i)
        }
        Library.deleteLyrics(ctx, s.uri)
        persist(songs.filter { it.uri != s.uri })
    }

    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            val added = withContext(Dispatchers.IO) {
                uris.map { u ->
                    try {
                        ctx.contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (_: Exception) {
                    }
                    Library.read(ctx, u)
                }
            }
            val known = songs.map { it.uri }.toSet()
            persist(songs + added.filter { it.uri !in known }.distinctBy { it.uri })
        }
    }

    val lrcPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val s = current
        if (uri != null && s != null) scope.launch {
            val text = withContext(Dispatchers.IO) {
                try {
                    ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                } catch (e: Exception) {
                    null
                }
            }
            if (text != null) {
                withContext(Dispatchers.IO) { Library.saveLyrics(ctx, s.uri, text) }
                lyrics = parseLrc(text)
            }
        }
    }

    fun addMusic() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        audioPicker.launch(arrayOf("audio/*"))
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val activeLine = remember(pos, lyrics) { lyrics.indexOfLast { it.timeMs in 0..pos } }

    MaterialTheme(colorScheme = if (pal.light) lightColorScheme() else darkColorScheme()) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(pal.top, pal.bg, pal.bg)))) {
            val transition = rememberInfiniteTransition(label = "ambient")
            val glow by transition.animateFloat(
                initialValue = 0.78f, targetValue = 1.12f,
                animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "glow"
            )
            Box(
                Modifier.offset(210.dp, (-95).dp).size(260.dp).scale(glow).blur(70.dp)
                    .background(pal.accent.copy(alpha = .22f), CircleShape)
            )

            LazyColumn(
                Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 24.dp + navBottom),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("SONARA", color = pal.accent, fontSize = 12.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
                            Text("Your sound, your space.", color = pal.text, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        }
                        RoundIcon(if (pal.light) Icons.Default.LightMode else Icons.Default.DarkMode, pal) {
                            settings.update("theme", if (pal.light) 0 else 1)
                        }
                        Spacer(Modifier.width(8.dp))
                        RoundIcon(Icons.Default.Settings, pal) { sheet = Sheet.Settings }
                    }
                }

                item {
                    GlassCard(pal.panel, Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("NOW PLAYING", color = pal.muted, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                                if (ui.speed != 1f) Text("${ui.speed}x", color = pal.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            AnimatedContent(
                                targetState = ui.mediaId,
                                transitionSpec = {
                                    (fadeIn(tween(350)) + scaleIn(initialScale = .92f, animationSpec = tween(350))) togetherWith fadeOut(tween(180))
                                },
                                label = "artwork"
                            ) { _ ->
                                Box(
                                    Modifier.size(190.dp).clip(RoundedCornerShape(32.dp))
                                        .background(Brush.linearGradient(listOf(pal.accent.copy(alpha = .85f), Color(0xFF594D9D), Color(0xFF23263E)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val a = art
                                    if (a != null) {
                                        Image(a, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else {
                                        Icon(Icons.Default.GraphicEq, null, tint = Color.White.copy(alpha = .18f), modifier = Modifier.size(150.dp))
                                        Box(Modifier.size(112.dp).clip(CircleShape).background(Color.White.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.MusicNote, null, tint = Color.White, modifier = Modifier.size(56.dp))
                                        }
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(current?.title ?: "Nothing playing", color = pal.text, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(current?.artist ?: "Add music to begin", color = pal.muted, fontSize = 14.sp)
                                val line = lyrics.getOrNull(activeLine)?.takeIf { it.timeMs >= 0 }?.text
                                if (!line.isNullOrBlank()) {
                                    Text(line, color = pal.accent, fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(top = 6.dp))
                                }
                            }
                            VizView(settings.visualizer, ui.playing, pal.accent, Modifier.fillMaxWidth().height(40.dp))
                        }
                    }
                }

                item {
                    GlassCard(pal.panel, Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Slider(
                                value = drag ?: pos.toFloat().coerceIn(0f, maxOf(dur, 1L).toFloat()),
                                onValueChange = { drag = it },
                                onValueChangeFinished = {
                                    drag?.let { player.seekTo(it.toLong()) }
                                    drag = null
                                },
                                valueRange = 0f..maxOf(dur, 1L).toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = pal.accent, activeTrackColor = pal.accent,
                                    inactiveTrackColor = pal.muted.copy(alpha = .3f)
                                )
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(clock(drag?.toLong() ?: pos), color = pal.muted, fontSize = 11.sp)
                                Text(clock(dur), color = pal.muted, fontSize = 11.sp)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)) }) {
                                    Icon(Icons.Default.Replay10, null, tint = pal.muted)
                                }
                                IconButton(onClick = { if (player.mediaItemCount > 0) player.seekToPrevious() }) {
                                    Icon(Icons.Default.SkipPrevious, null, tint = pal.text, modifier = Modifier.size(32.dp))
                                }
                                Surface(shape = CircleShape, color = pal.accent, onClick = {
                                    if (player.mediaItemCount == 0) {
                                        if (visible.isNotEmpty()) play(0) else addMusic()
                                    } else if (player.isPlaying) player.pause()
                                    else { player.play(); ensureService() }
                                }) {
                                    Icon(if (ui.playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = ON_ACCENT, modifier = Modifier.padding(17.dp).size(30.dp))
                                }
                                IconButton(onClick = { if (player.mediaItemCount > 0) player.seekToNext() }) {
                                    Icon(Icons.Default.SkipNext, null, tint = pal.text, modifier = Modifier.size(32.dp))
                                }
                                IconButton(onClick = { player.seekTo(player.currentPosition + 10_000) }) {
                                    Icon(Icons.Default.Forward10, null, tint = pal.muted)
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    player.shuffleModeEnabled = !player.shuffleModeEnabled
                                    settings.savePlayback(player.shuffleModeEnabled, player.repeatMode, ui.speed)
                                }) { Icon(Icons.Default.Shuffle, null, tint = if (ui.shuffle) pal.accent else pal.muted) }
                                IconButton(onClick = {
                                    player.repeatMode = when (player.repeatMode) {
                                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                        else -> Player.REPEAT_MODE_OFF
                                    }
                                    settings.savePlayback(player.shuffleModeEnabled, player.repeatMode, ui.speed)
                                }) {
                                    Icon(
                                        if (ui.repeat == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                        null, tint = if (ui.repeat == Player.REPEAT_MODE_OFF) pal.muted else pal.accent
                                    )
                                }
                                Box(
                                    Modifier.size(48.dp).clip(CircleShape).clickable {
                                        val steps = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
                                        val next = steps[(steps.indexOfFirst { it == ui.speed } + 1).mod(steps.size)]
                                        player.setPlaybackSpeed(next)
                                        settings.savePlayback(player.shuffleModeEnabled, player.repeatMode, next)
                                    },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${ui.speed}x", color = if (ui.speed != 1f) pal.accent else pal.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                SleepButton(pal) { sheet = Sheet.Sleep }
                                IconButton(onClick = { sheet = Sheet.Lyrics }) {
                                    Icon(Icons.Default.Subject, null, tint = if (lyrics.isNotEmpty()) pal.accent else pal.muted)
                                }
                                IconButton(onClick = { sheet = Sheet.Sound }) {
                                    Icon(Icons.Default.Equalizer, null, tint = pal.muted)
                                }
                            }
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("YOUR LIBRARY", color = pal.text, fontSize = 13.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { addMusic() }) {
                            Icon(Icons.Default.Add, null, tint = pal.accent)
                            Text(" Add music", color = pal.accent)
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        placeholder = { Text("Search songs or artists…", color = pal.muted) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = pal.muted) },
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = pal.accent, unfocusedBorderColor = pal.muted.copy(alpha = .3f),
                            focusedTextColor = pal.text, unfocusedTextColor = pal.text, cursorColor = pal.accent
                        )
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("All", !favOnly, pal) { favOnly = false }
                        Chip("Favorites", favOnly, pal) { favOnly = true }
                        Chip("Sort: ${SORT_NAMES[settings.sort.coerceIn(0, 3)]}", false, pal) {
                            settings.update("sort", (settings.sort + 1) % 4)
                        }
                    }
                }

                if (visible.isEmpty()) {
                    item {
                        Text(
                            if (songs.isEmpty()) "Your songs will appear here. Tap Add music to choose audio files from your device."
                            else "No matching songs.",
                            color = pal.muted, modifier = Modifier.fillMaxWidth().padding(24.dp)
                        )
                    }
                }

                itemsIndexed(visible, key = { _, s -> s.uri }) { index, song ->
                    val isCur = song.uri == ui.mediaId
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
                            .background(if (isCur) pal.accent.copy(alpha = .18f) else pal.panel)
                            .clickable { play(index) }
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(pal.accent.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                            Icon(if (isCur && ui.playing) Icons.Default.GraphicEq else Icons.Default.MusicNote, null, tint = pal.accent)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, color = pal.text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val len = if (song.durationMs > 0) " · " + clock(song.durationMs) else ""
                            Text(song.artist + len, color = pal.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { persist(songs.map { if (it.uri == song.uri) it.copy(fav = !it.fav) else it }) }) {
                            Icon(if (song.fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (song.fav) pal.accent else pal.muted)
                        }
                        Box {
                            IconButton(onClick = { menuFor = song.uri }) { Icon(Icons.Default.MoreVert, null, tint = pal.muted) }
                            DropdownMenu(expanded = menuFor == song.uri, onDismissRequest = { menuFor = null }) {
                                DropdownMenuItem(text = { Text("Remove from library") }, onClick = { menuFor = null; remove(song) })
                            }
                        }
                    }
                }
            }

            if (sheet != Sheet.None) {
                ModalBottomSheet(
                    onDismissRequest = { sheet = Sheet.None },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = pal.bg,
                    contentColor = pal.text
                ) {
                    when (sheet) {
                        Sheet.Settings -> SettingsSheet(settings, pal)
                        Sheet.Sound -> SoundSheet(settings, pal, ui, player)
                        Sheet.Sleep -> SleepSheet(pal)
                        Sheet.Lyrics -> LyricsSheet(
                            hasSong = current != null, lyrics = lyrics, active = activeLine, pal = pal,
                            onLoad = { lrcPicker.launch(arrayOf("*/*")) },
                            onRemove = {
                                current?.let { Library.deleteLyrics(ctx, it.uri) }
                                lyrics = emptyList()
                            }
                        )
                        Sheet.None -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassCard(color: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(28.dp), color = color, tonalElevation = 2.dp, shadowElevation = 12.dp) {
        Box(Modifier.padding(18.dp)) { content() }
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, pal: Palette, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = pal.panel, onClick = onClick) {
        Icon(icon, null, tint = pal.accent, modifier = Modifier.padding(10.dp).size(20.dp))
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, pal: Palette, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) pal.accent else pal.panel,
        border = BorderStroke(1.dp, if (selected) pal.accent else pal.muted.copy(alpha = .3f))
    ) {
        Text(
            label,
            color = if (selected) ON_ACCENT else pal.text,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SleepButton(pal: Palette, onClick: () -> Unit) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val active = PlayerHolder.sleepEndAt > 0
    LaunchedEffect(active) {
        while (active) {
            now = SystemClock.elapsedRealtime()
            delay(1000)
        }
    }
    val on = active || PlayerHolder.sleepAtTrackEnd
    Row(
        Modifier.height(48.dp).clip(CircleShape).clickable(onClick = onClick).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Timer, null, tint = if (on) pal.accent else pal.muted)
        if (active) {
            Text(" " + clock((PlayerHolder.sleepEndAt - now).coerceAtLeast(0)), color = pal.accent, fontSize = 11.sp)
        }
    }
}

@Composable
private fun VizView(style: Int, playing: Boolean, color: Color, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "viz")
    val phase by t.animateFloat(
        0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "phase"
    )
    val amp by animateFloatAsState(if (playing) 1f else 0.1f, tween(500), label = "amp")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        when (style) {
            1 -> {
                val path = Path()
                val steps = 60
                for (i in 0..steps) {
                    val x = w * i / steps
                    val y = h / 2 + sin(phase * 2f + i * 0.28f) * h * 0.32f * amp * (0.6f + 0.4f * sin(phase + i * 0.11f))
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
            }
            2 -> {
                val n = 16
                val gap = w / n
                for (i in 0 until n) {
                    val v = 0.5f + 0.5f * sin(phase * (1f + (i % 4) * 0.5f) + i)
                    drawCircle(
                        color.copy(alpha = 0.35f + 0.65f * v),
                        radius = (2f + 6f * v * amp).dp.toPx(),
                        center = Offset(gap * i + gap / 2f, h / 2f)
                    )
                }
            }
            3 -> {
                for (k in 0 until 3) {
                    val f = ((phase / (2f * Math.PI.toFloat())) + k / 3f) % 1f
                    drawCircle(
                        color.copy(alpha = ((1f - f) * 0.7f * (0.3f + 0.7f * amp)).coerceIn(0f, 1f)),
                        radius = h * (0.1f + 0.5f * f),
                        center = Offset(w / 2f, h / 2f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
            else -> {
                val n = 30
                val bw = w / (n * 1.6f)
                val gap = (w - bw * n) / (n - 1)
                for (i in 0 until n) {
                    val v = 0.5f + 0.5f * sin(phase * (1f + (i % 5) * 0.7f) + i * 0.8f)
                    val bh = h * (0.1f + 0.9f * v * amp).coerceAtMost(1f)
                    drawRoundRect(
                        color.copy(alpha = .85f),
                        topLeft = Offset(i * (bw + gap), (h - bh) / 2f),
                        size = Size(bw, bh),
                        cornerRadius = CornerRadius(bw / 2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun Label(text: String, pal: Palette) {
    Text(text, color = pal.muted, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun SheetBody(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsSheet(s: AppSettings, pal: Palette) {
    SheetBody {
        Text("Appearance", color = pal.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Label("THEME", pal)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            THEME_NAMES.forEachIndexed { i, n -> Chip(n, s.theme == i, pal) { s.update("theme", i) } }
        }
        Label("ACCENT COLOR", pal)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ACCENTS.forEachIndexed { i, a ->
                val c = if (pal.light) a.light else a.dark
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(c)
                        .clickable { s.update("accent", i) },
                    contentAlignment = Alignment.Center
                ) {
                    if (s.accent == i) Box(Modifier.size(12.dp).clip(CircleShape).background(ON_ACCENT))
                }
            }
        }
        Label("WALLPAPER", pal)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WALLS.forEachIndexed { i, w -> Chip(w.name, s.wallpaper == i, pal) { s.update("wallpaper", i) } }
        }
        Label("VISUALIZER STYLE", pal)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            VIZ_NAMES.forEachIndexed { i, n -> Chip(n, s.visualizer == i, pal) { s.update("visualizer", i) } }
        }
        Text("The visualizer is animated for looks and doesn't react to the audio.", color = pal.muted, fontSize = 12.sp)
        Label("LIBRARY SORT", pal)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SORT_NAMES.forEachIndexed { i, n -> Chip(n, s.sort == i, pal) { s.update("sort", i) } }
        }
        Text("Sonara 2.0", color = pal.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SoundSheet(s: AppSettings, pal: Palette, ui: PlayerUi, player: ExoPlayer) {
    var bass by remember { mutableFloatStateOf(s.bass.toFloat()) }
    var fade by remember { mutableFloatStateOf(s.fadeSec.toFloat()) }
    val sliderColors = SliderDefaults.colors(
        thumbColor = pal.accent, activeTrackColor = pal.accent, inactiveTrackColor = pal.muted.copy(alpha = .3f)
    )
    SheetBody {
        Text("Sound", color = pal.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Label("EQUALIZER PRESET", pal)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EQ_PRESETS.forEachIndexed { i, p ->
                Chip(p.name, s.eqPreset == i, pal) {
                    s.update("eq", i)
                    PlayerHolder.setEffects(i, s.bass)
                }
            }
        }
        Label("BASS BOOST  ${(bass / 10).toInt()}%", pal)
        Slider(
            value = bass, onValueChange = { bass = it },
            onValueChangeFinished = {
                s.update("bass", bass.toInt())
                PlayerHolder.setEffects(s.eqPreset, bass.toInt())
            },
            valueRange = 0f..1000f, colors = sliderColors
        )
        Label("FADE BETWEEN SONGS  ${if (fade.toInt() == 0) "Off" else "${fade.toInt()}s"}", pal)
        Slider(
            value = fade, onValueChange = { fade = it },
            onValueChangeFinished = {
                s.update("fade", fade.toInt())
                PlayerHolder.fadeMs = fade.toInt() * 1000L
            },
            valueRange = 0f..10f, steps = 9, colors = sliderColors
        )
        Label("PLAYBACK SPEED", pal)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { sp ->
                Chip("${sp}x", ui.speed == sp, pal) {
                    player.setPlaybackSpeed(sp)
                    s.savePlayback(player.shuffleModeEnabled, player.repeatMode, sp)
                }
            }
        }
        Text("Equalizer and bass boost depend on your phone's audio hardware.", color = pal.muted, fontSize = 12.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SleepSheet(pal: Palette) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            delay(1000)
        }
    }
    SheetBody {
        Text("Sleep timer", color = pal.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        val status = when {
            PlayerHolder.sleepEndAt > 0 -> "Music stops in " + clock((PlayerHolder.sleepEndAt - now).coerceAtLeast(0))
            PlayerHolder.sleepAtTrackEnd -> "Music stops when this song ends"
            else -> "Timer is off"
        }
        Text(status, color = pal.accent, fontSize = 14.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 30, 45, 60, 90).forEach { m -> Chip("$m min", false, pal) { PlayerHolder.startSleep(m) } }
            Chip("End of song", PlayerHolder.sleepAtTrackEnd, pal) { PlayerHolder.sleepAtEndOfTrack() }
        }
        Button(
            onClick = { PlayerHolder.cancelSleep() },
            colors = ButtonDefaults.buttonColors(containerColor = pal.accent, contentColor = ON_ACCENT)
        ) { Text("Turn off timer") }
        Text("Volume fades out gently before playback stops.", color = pal.muted, fontSize = 12.sp)
    }
}

@Composable
private fun LyricsSheet(
    hasSong: Boolean,
    lyrics: List<LyricLine>,
    active: Int,
    pal: Palette,
    onLoad: () -> Unit,
    onRemove: () -> Unit,
) {
    val state = rememberLazyListState()
    LaunchedEffect(active) {
        if (active >= 0) state.animateScrollToItem((active - 2).coerceAtLeast(0))
    }
    Column(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Lyrics", color = pal.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        if (!hasSong) {
            Text("Play a song first, then add its .lrc lyrics file.", color = pal.muted)
        } else if (lyrics.isEmpty()) {
            Text("No lyrics for this song yet. Pick a .lrc file (synced) or a .txt file (plain).", color = pal.muted)
            Button(onClick = onLoad, colors = ButtonDefaults.buttonColors(containerColor = pal.accent, contentColor = ON_ACCENT)) {
                Text("Load lyrics file")
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth().height(380.dp), state = state, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(lyrics) { i, l ->
                    Text(
                        l.text.ifBlank { "♪" },
                        color = if (i == active) pal.accent else pal.text.copy(alpha = if (active < 0) 0.9f else 0.55f),
                        fontSize = if (i == active) 20.sp else 17.sp,
                        fontWeight = if (i == active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Replace", false, pal, onLoad)
                Chip("Remove", false, pal, onRemove)
            }
        }
    }
}
