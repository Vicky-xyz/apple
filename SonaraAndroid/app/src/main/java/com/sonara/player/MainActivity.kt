package com.sonara.player

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.filled.Palette as PaletteIcon
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
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
        if (settings.reactive == 1 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        ) PlayerHolder.setVisualizer(true)
        setContent { SonaraApp(settings, player) }
    }

    override fun onStart() {
        super.onStart()
        PlayerHolder.vizVisible = true
    }

    override fun onStop() {
        PlayerHolder.vizVisible = false
        super.onStop()
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

private fun folderOf(s: Song): String = s.path.substringBeforeLast('/', "")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun SonaraApp(settings: AppSettings, player: ExoPlayer) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var view by remember { mutableStateOf("all") }
    var songColor by remember { mutableStateOf<Color?>(null) }
    val page = when {
        view == "fav" -> 1
        view.startsWith("pl:") -> 2
        view.startsWith("dir:") -> 3
        else -> 0
    }
    val pal = animatedPalette(buildPalette(settings, songColor, page))
    val act = ctx as ComponentActivity

    LaunchedEffect(pal.light) {
        val t = android.graphics.Color.TRANSPARENT
        val style = if (pal.light) SystemBarStyle.light(t, t) else SystemBarStyle.dark(t)
        act.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    var songs by remember { mutableStateOf(Library.load(ctx)) }
    var query by remember { mutableStateOf("") }
    var playlists by remember { mutableStateOf(Library.loadPlaylists(ctx)) }
    var nameDialog by remember { mutableStateOf<String?>(null) }
    var nameText by remember { mutableStateOf("") }
    var addTo by remember { mutableStateOf<String?>(null) }
    var scanStatus by remember { mutableStateOf("") }
    var wallImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var customizing by remember { mutableStateOf(false) }
    var draftPhoto by remember { mutableStateOf<ImageBitmap?>(null) }
    var photoChanged by remember { mutableStateOf(false) }
    val draft = remember(customizing) { AppSettings(ctx, false) }
    var folders by remember { mutableStateOf(Library.loadFolders(ctx)) }
    var plays by remember { mutableStateOf(Library.loadPlays(ctx)) }
    var sortMenu by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(Sheet.None) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var drag by remember { mutableStateOf<Float?>(null) }
    var art by remember { mutableStateOf<ImageBitmap?>(null) }
    var lyrics by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var menuFor by remember { mutableStateOf<String?>(null) }

    val ui = remember { PlayerUi(player) }
    DisposableEffect(Unit) { onDispose { ui.dispose() } }
    LaunchedEffect(ui.mediaId) {
        plays = Library.loadPlays(ctx)
        delay(17_000)
        plays = Library.loadPlays(ctx)
    }

    val current = songs.firstOrNull { it.uri == ui.mediaId }

    val currentPlaylist = playlists.firstOrNull { "pl:" + it.id == view }
    val visible = remember(songs, query, view, playlists, settings.sort, settings.sortDesc, plays) {
        val byUri = songs.associateBy { it.uri }
        val base = when {
            view == "fav" -> songs.filter { it.fav }
            view.startsWith("dir:") -> songs.filter { folderOf(it) == view.removePrefix("dir:") }
            view.startsWith("pl:") ->
                (playlists.firstOrNull { "pl:" + it.id == view }?.uris ?: emptyList()).mapNotNull { byUri[it] }
            else -> songs
        }
        // every word typed must match the title, artist, album or folder
        val terms = query.trim().lowercase().split(' ').filter { it.isNotEmpty() }
        val found = if (terms.isEmpty()) base else base.filter { sg ->
            val hay = (sg.title + " " + sg.artist + " " + sg.album + " " + sg.path).lowercase()
            terms.all { hay.contains(it) }
        }
        sortSongs(found, settings.sort, settings.sortDesc == 1, plays, view.startsWith("dir:"))
    }

    val folderList = remember(songs) {
        val groups = songs.filter { it.path.isNotEmpty() }.groupBy { folderOf(it) }
        val names = groups.keys.associateWith { it.substringAfterLast('/') }
        val dupes = names.values.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        groups.map { (dir, list) ->
            val n = names.getValue(dir)
            val label = if (n in dupes) dir.substringBeforeLast('/', "").substringAfterLast('/') + "/" + n else n
            Triple(dir, label.ifBlank { "Storage" }, list.size)
        }.sortedBy { it.second.lowercase() }
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
        val s = current ?: run { songColor = null; return@LaunchedEffect }
        lyrics = withContext(Dispatchers.IO) { Library.loadLyrics(ctx, s.uri) }
        val bytes = withContext(Dispatchers.IO) { Library.art(ctx, s.uri) } ?: run { songColor = null; return@LaunchedEffect }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        art = decoded?.asImageBitmap()
        songColor = if (decoded != null) Color(withContext(Dispatchers.Default) { Library.dominantColor(decoded) }) else null
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

    fun persistPl(list: List<Playlist>) {
        playlists = list
        Library.savePlaylists(ctx, list)
    }

    fun scanFolders(list: List<Uri>) {
        if (list.isEmpty()) return
        scope.launch {
            scanStatus = "Scanning folders…"
            val found = withContext(Dispatchers.IO) { list.flatMap { Library.scanTree(ctx, it) } }
            val known = songs.map { it.uri }.toSet()
            val fresh = found.filter { it.toString() !in known }.distinctBy { it.toString() }
            val added = withContext(Dispatchers.IO) { fresh.map { Library.read(ctx, it) } }
            persist(songs + added)
            scanStatus = "Found ${found.size} audio files, added ${added.size} new"
        }
    }

    fun ensureService() {
        try { ctx.startService(Intent(ctx, PlaybackService::class.java)) } catch (_: Exception) {}
    }

    fun startQueue(queue: List<Song>, index: Int) {
        if (queue.isEmpty()) return
        player.setMediaItems(queue.map { it.toMediaItem() }, index.coerceIn(0, queue.lastIndex), 0L)
        player.prepare()
        player.play()
        ensureService()
    }

    fun play(index: Int) {
        val item = visible.getOrNull(index) ?: return
        if (item.uri == ui.mediaId) {
            if (player.isPlaying) player.pause() else { player.play(); ensureService() }
            return
        }
        // Each folder is its own playlist: next / previous / shuffle stay inside the song's folder.
        val queue = if (view == "all" && settings.folderSep == 1 && item.path.isNotEmpty()) {
            val same = visible.filter { folderOf(it) == folderOf(item) }
            if (settings.sort == 0) sortSongs(same, 0, settings.sortDesc == 1, plays, true) else same
        } else visible
        startQueue(queue, queue.indexOfFirst { it.uri == item.uri })
    }

    fun remove(s: Song) {
        for (i in player.mediaItemCount - 1 downTo 0) {
            if (player.getMediaItemAt(i).mediaId == s.uri) player.removeMediaItem(i)
        }
        Library.deleteLyrics(ctx, s.uri)
        if (s.uri.startsWith("content://media/")) Library.saveHidden(ctx, Library.loadHidden(ctx) + s.uri)
        persistPl(playlists.map { it.copy(uris = it.uris - s.uri) })
        persist(songs.filter { it.uri != s.uri })
    }

    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
            val list = (folders + uri.toString()).distinct()
            folders = list
            Library.saveFolders(ctx, list)
            scanFolders(listOf(uri))
        }
    }

    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        settings.update("reactive", if (granted) 1 else 0)
        PlayerHolder.setVisualizer(granted)
    }

    fun setReactive(on: Boolean) {
        if (!on) {
            settings.update("reactive", 0)
            PlayerHolder.setVisualizer(false)
        } else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            settings.update("reactive", 1)
            PlayerHolder.setVisualizer(true)
        } else {
            micPerm.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    var cropSource by remember { mutableStateOf<Bitmap?>(null) }

    val wallPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val bmp = withContext(Dispatchers.IO) {
                Library.decodeSampled(ctx, uri, 2400)?.also { Library.saveBitmap(ctx, it, "wallpaper_src_draft.jpg") }
            }
            if (bmp != null) cropSource = bmp
        }
    }

    fun recrop() {
        scope.launch {
            val bmp = withContext(Dispatchers.IO) {
                Library.loadBitmap(ctx, "wallpaper_src_draft.jpg") ?: Library.loadBitmap(ctx, "wallpaper_src.jpg")
            }
            if (bmp != null) cropSource = bmp else wallPicker.launch(arrayOf("image/*"))
        }
    }

    fun finishCrop(x: Int, y: Int, w: Int, h: Int) {
        val src = cropSource ?: return
        scope.launch {
            val ok = withContext(Dispatchers.IO) { Library.cropAndSave(ctx, src, x, y, w, h, "wallpaper_draft.jpg") }
            if (ok) {
                draftPhoto = withContext(Dispatchers.IO) { Library.loadWallpaper(ctx, "wallpaper_draft.jpg") }
                photoChanged = true
                draft.update("walltype", 1)
            }
            cropSource = null
        }
    }

    LaunchedEffect(customizing) {
        if (customizing) {
            photoChanged = false
            withContext(Dispatchers.IO) { Library.clearDraftWallpaper(ctx) }
            draftPhoto = wallImage ?: withContext(Dispatchers.IO) { Library.loadWallpaper(ctx) }
        }
    }

    fun applyCustomize() {
        scope.launch {
            if (photoChanged) withContext(Dispatchers.IO) { Library.promoteDraftWallpaper(ctx) }
            if (draftPhoto != null) wallImage = draftPhoto
            settings.applyFrom(draft)
            customizing = false
        }
    }

    LaunchedEffect(settings.wallType) {
        if (settings.wallType == 1 && wallImage == null) {
            wallImage = withContext(Dispatchers.IO) { Library.loadWallpaper(ctx) }
        }
    }

    val audioPermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

    fun hasAudioPerm(): Boolean =
        ContextCompat.checkSelfPermission(ctx, audioPermission) == PackageManager.PERMISSION_GRANTED

    fun scanDevice() {
        scope.launch {
            scanStatus = "Scanning your device…"
            val found = withContext(Dispatchers.IO) {
                Library.scanDevice(ctx, settings.minLenSec * 1000L, Library.loadHidden(ctx))
            }
            persist(Library.merge(songs, found))
            scanStatus = "Found ${found.size} songs on this device"
        }
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        if (hasAudioPerm()) {
            scanDevice()
        } else {
            scanStatus = "Allow access to your music so Sonara can find songs automatically."
        }
    }

    fun requestScan() {
        if (hasAudioPerm()) scanDevice() else permLauncher.launch(arrayOf(audioPermission))
    }

    LaunchedEffect(Unit) {
        val need = mutableListOf<String>()
        if (!hasAudioPerm()) need.add(audioPermission)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) need.add(Manifest.permission.POST_NOTIFICATIONS)
        if (need.isEmpty()) scanDevice() else permLauncher.launch(need.toTypedArray())
    }

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
    val listState = rememberLazyListState()
    val scrollShade = remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 500f).coerceIn(0f, 1f)
        }
    }
    val spin = rememberInfiniteTransition(label = "spin").animateFloat(
        0f, 360f, infiniteRepeatable(tween(3600, easing = LinearEasing)), label = "spinAngle"
    )
    val activeLine = remember(pos, lyrics) { lyrics.indexOfLast { it.timeMs in 0..pos } }

    MaterialTheme(colorScheme = if (pal.light) lightColorScheme() else darkColorScheme()) {
        Box(
            Modifier.fillMaxSize().drawBehind {
                drawRect(Brush.verticalGradient(listOf(lerp(pal.top, pal.bg, scrollShade.value), pal.bg, pal.bg)))
            }
        ) {
            WallpaperLayer(settings, pal, wallImage, art)
            val transition = rememberInfiniteTransition(label = "ambient")
            val glow by transition.animateFloat(
                initialValue = 0.78f, targetValue = 1.12f,
                animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "glow"
            )
            Box(
                Modifier.offset(150.dp, (-120).dp).size(340.dp).scale(glow)
                    .background(Brush.radialGradient(listOf(pal.glow.copy(alpha = 0.38f), Color.Transparent)), CircleShape)
            )
            Box(
                Modifier.align(Alignment.BottomStart).offset((-120).dp, 120.dp).size(320.dp).scale(glow)
                    .background(Brush.radialGradient(listOf(pal.glow2.copy(alpha = 0.32f), Color.Transparent)), CircleShape)
            )

            LazyColumn(
                Modifier.fillMaxSize().statusBarsPadding(),
                state = listState,
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 24.dp + navBottom),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("SONARA", color = pal.accent, fontSize = 12.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
                            Text("Your sound, your space.", color = pal.text, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        }
                        RoundIcon(Icons.Default.PaletteIcon, pal) {
                            settings.update("palette", (settings.palette + 1) % THEMES.size)
                        }
                        Spacer(Modifier.width(8.dp))
                        RoundIcon(Icons.Default.Settings, pal) { sheet = Sheet.Settings }
                    }
                }

                item {
                    GlassCard(pal, Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    VinylIcon(
                                        pal.accent, pal.text,
                                        Modifier.size(16.dp).graphicsLayer { rotationZ = if (ui.playing && settings.spin == 1) spin.value else 0f }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("NOW PLAYING", color = pal.muted, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                                }
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
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    current?.title ?: "Nothing playing", color = pal.text, fontSize = 21.sp, fontWeight = FontWeight.Bold,
                                    maxLines = 1, softWrap = false, textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE)
                                )
                                Text(
                                    current?.artist ?: "Add music to begin", color = pal.muted, fontSize = 14.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (lyrics.isNotEmpty()) {
                                    val line = lyrics.getOrNull(activeLine)?.takeIf { it.timeMs >= 0 }?.text ?: ""
                                    Box(Modifier.fillMaxWidth().height(24.dp).padding(top = 4.dp), contentAlignment = Alignment.Center) {
                                        Text(line, color = pal.accent, fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            VizView(settings, ui.playing, pal.accent, pal.text, pal.comp1, pal.comp2, Modifier.fillMaxWidth().height(listOf(28, 40, 64, 96)[settings.vizHeight.coerceIn(0, 3)].dp))
                        }
                    }
                }

                item {
                    GlassCard(pal, Modifier.fillMaxWidth()) {
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
                                    Icon(if (ui.playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = pal.onAccent, modifier = Modifier.padding(17.dp).size(30.dp))
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { Chip("All", view == "all", pal) { view = "all" } }
                        item { Chip("Favorites", view == "fav", pal) { view = "fav" } }
                        items(playlists, key = { it.id }) { pl ->
                            Chip(pl.name, view == "pl:" + pl.id, pal) { view = "pl:" + pl.id }
                        }
                        item { Chip("+ Playlist", false, pal) { nameText = ""; nameDialog = "new" } }
                    }
                }
                if (folderList.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("FOLDERS  ·  each folder plays on its own", color = pal.muted, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(folderList, key = { it.first }) { f ->
                                    Chip(f.second + " (" + f.third + ")", view == "dir:" + f.first, pal) { view = "dir:" + f.first }
                                }
                            }
                        }
                    }
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box {
                            Chip("Sort: " + sortLabel(settings.sort, settings.sortDesc == 1) + "  ▾", false, pal) { sortMenu = true }
                            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                                SORT_NAMES.forEachIndexed { i, n ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(if (settings.sort == i) n + (if (settings.sortDesc == 1) "   ↓ reversed" else "   ↑") else n)
                                        },
                                        onClick = {
                                            if (settings.sort == i) {
                                                settings.update("sortdesc", 1 - settings.sortDesc)
                                            } else {
                                                settings.update("sort", i)
                                                settings.update("sortdesc", 0)
                                            }
                                            sortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        Chip("Scan device", false, pal) { requestScan() }
                        if (view.startsWith("dir:") || currentPlaylist != null) {
                            Chip("Play all", false, pal) { startQueue(visible, 0) }
                            Chip("Shuffle", false, pal) {
                                player.shuffleModeEnabled = true
                                settings.savePlayback(true, player.repeatMode, ui.speed)
                                startQueue(visible, 0)
                            }
                        }
                        if (currentPlaylist != null) {
                            Chip("Rename", false, pal) { nameText = currentPlaylist.name; nameDialog = "rename" }
                            Chip("Delete", false, pal) {
                                persistPl(playlists.filter { it.id != currentPlaylist.id })
                                view = "all"
                            }
                        }
                    }
                }

                if (visible.isEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                if (songs.isEmpty()) "No songs yet. Sonara finds the music on your phone automatically once you allow access."
                                else "No matching songs.",
                                color = pal.muted
                            )
                            if (songs.isEmpty()) Chip("Scan device", false, pal) { requestScan() }
                        }
                    }
                }

                itemsIndexed(visible, key = { _, s -> s.uri }) { index, song ->
                    val isCur = song.uri == ui.mediaId
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
                            .background(if (isCur) pal.accent.copy(alpha = .18f) else pal.panel)
                            .border(0.5.dp, Color.White.copy(alpha = if (pal.light) 0.6f else 0.1f), RoundedCornerShape(17.dp))
                            .clickable { play(index) }
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(pal.accent.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                            if (isCur) {
                                VinylIcon(
                                    pal.accent, pal.text,
                                    Modifier.size(30.dp).graphicsLayer { rotationZ = if (ui.playing && settings.spin == 1) spin.value else 0f }
                                )
                            } else {
                                Icon(Icons.Default.MusicNote, null, tint = pal.accent)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, color = pal.text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val folder = song.path.substringBeforeLast('/', "").substringAfterLast('/')
                            val extra = (if (folder.isNotEmpty()) " · " + folder else "") +
                                (if (song.durationMs > 0) " · " + clock(song.durationMs) else "")
                            Text(song.artist + extra, color = pal.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { persist(songs.map { if (it.uri == song.uri) it.copy(fav = !it.fav) else it }) }) {
                            Icon(if (song.fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (song.fav) pal.comp2 else pal.muted)
                        }
                        Box {
                            IconButton(onClick = { menuFor = song.uri }) { Icon(Icons.Default.MoreVert, null, tint = pal.muted) }
                            DropdownMenu(expanded = menuFor == song.uri, onDismissRequest = { menuFor = null }) {
                                DropdownMenuItem(text = { Text("Add to playlist") }, onClick = { menuFor = null; addTo = song.uri })
                                if (currentPlaylist != null) {
                                    DropdownMenuItem(text = { Text("Remove from this playlist") }, onClick = {
                                        menuFor = null
                                        persistPl(playlists.map { if (it.id == currentPlaylist.id) it.copy(uris = it.uris - song.uri) else it })
                                    })
                                }
                                DropdownMenuItem(text = { Text("Remove from library") }, onClick = { menuFor = null; remove(song) })
                            }
                        }
                    }
                }
            }

            Box(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().height(80.dp)
                    .graphicsLayer { alpha = scrollShade.value }
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = if (pal.light) 0.16f else 0.5f), Color.Transparent)))
            )

            if (customizing) {
                BackHandler { customizing = false }
                CustomizeWindow(
                    d = draft, pal = pal, songColor = songColor, art = art, photo = draftPhoto,
                    onPickPhoto = { wallPicker.launch(arrayOf("image/*")) },
                    onRecrop = { recrop() },
                    onApply = { applyCustomize() },
                    onCancel = { customizing = false }
                )
            }

            cropSource?.let { bmp ->
                BackHandler { cropSource = null }
                val dm = ctx.resources.displayMetrics
                CropScreen(
                    bmp = bmp,
                    screenAspect = dm.heightPixels.toFloat() / dm.widthPixels.toFloat(),
                    onCancel = { cropSource = null },
                    onDone = { x, y, w, h -> finishCrop(x, y, w, h) }
                )
            }

            val nd = nameDialog
            if (nd != null) {
                AlertDialog(
                    onDismissRequest = { nameDialog = null },
                    containerColor = pal.bg, titleContentColor = pal.text, textContentColor = pal.text,
                    title = { Text(if (nd == "rename") "Rename playlist" else "New playlist") },
                    text = {
                        OutlinedTextField(
                            value = nameText, onValueChange = { nameText = it }, singleLine = true,
                            placeholder = { Text("Playlist name", color = pal.muted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = pal.accent, unfocusedBorderColor = pal.muted.copy(alpha = .3f),
                                focusedTextColor = pal.text, unfocusedTextColor = pal.text, cursorColor = pal.accent
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val name = nameText.trim()
                            if (name.isNotEmpty()) {
                                if (nd == "rename") {
                                    val cp = currentPlaylist
                                    if (cp != null) persistPl(playlists.map { if (it.id == cp.id) it.copy(name = name) else it })
                                } else {
                                    val first = if (nd.startsWith("newfor:")) listOf(nd.removePrefix("newfor:")) else emptyList()
                                    val pl = Playlist(java.util.UUID.randomUUID().toString(), name, first)
                                    persistPl(playlists + pl)
                                    view = "pl:" + pl.id
                                }
                            }
                            nameDialog = null
                        }) { Text("Save", color = pal.accent) }
                    },
                    dismissButton = { TextButton(onClick = { nameDialog = null }) { Text("Cancel", color = pal.muted) } }
                )
            }

            val at = addTo
            if (at != null) {
                AlertDialog(
                    onDismissRequest = { addTo = null },
                    containerColor = pal.bg, titleContentColor = pal.text, textContentColor = pal.text,
                    title = { Text("Add to playlist") },
                    text = {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            playlists.forEach { pl ->
                                TextButton(onClick = {
                                    persistPl(playlists.map { if (it.id == pl.id && at !in it.uris) it.copy(uris = it.uris + at) else it })
                                    addTo = null
                                }) { Text(pl.name, color = pal.text) }
                            }
                            TextButton(onClick = { addTo = null; nameText = ""; nameDialog = "newfor:" + at }) {
                                Text("+ New playlist", color = pal.accent)
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = { TextButton(onClick = { addTo = null }) { Text("Close", color = pal.muted) } }
                )
            }

            if (sheet != Sheet.None) {
                ModalBottomSheet(
                    onDismissRequest = { sheet = Sheet.None },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = pal.bg,
                    contentColor = pal.text
                ) {
                    when (sheet) {
                        Sheet.Settings -> SettingsSheet(
                            settings, pal, folders, scanStatus,
                            onAddFolder = { folderPicker.launch(null) },
                            onRescan = { scanFolders(folders.map { Uri.parse(it) }) },
                            onRemoveFolder = { f ->
                                val l = folders - f
                                folders = l
                                Library.saveFolders(ctx, l)
                            },
                            onReactive = { setReactive(it) },
                            onScanDevice = { requestScan() },
                            onRestoreHidden = { Library.saveHidden(ctx, emptySet()); requestScan() },
                            onCustomize = { sheet = Sheet.None; customizing = true }
                        )
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
private fun GlassCard(pal: Palette, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier.shadow(
            14.dp, RoundedCornerShape(28.dp),
            ambientColor = pal.glow.copy(alpha = 0.5f), spotColor = pal.glow.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(28.dp),
        color = pal.panel,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = if (pal.light) 0.9f else 0.28f),
                    Color.White.copy(alpha = if (pal.light) 0.25f else 0.04f)
                )
            )
        )
    ) {
        Box(Modifier.padding(18.dp)) { content() }
    }
}

@Composable
private fun VinylIcon(color: Color, highlight: Color, modifier: Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        drawCircle(color, radius = r * 0.9f, center = c, style = Stroke(r * 0.16f))
        drawCircle(color, radius = r * 0.58f, center = c, alpha = 0.5f, style = Stroke(r * 0.06f))
        drawCircle(color, radius = r * 0.2f, center = c)
        drawArc(
            highlight.copy(alpha = 0.95f), startAngle = -60f, sweepAngle = 45f, useCenter = false,
            topLeft = Offset(c.x - r * 0.74f, c.y - r * 0.74f), size = Size(r * 1.48f, r * 1.48f),
            style = Stroke(r * 0.12f, cap = StrokeCap.Round)
        )
        drawCircle(highlight, radius = r * 0.07f, center = Offset(c.x + r * 0.32f, c.y + r * 0.32f))
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
        modifier = Modifier.widthIn(max = 220.dp),
        shape = RoundedCornerShape(50),
        color = if (selected) pal.accent else pal.panel,
        border = BorderStroke(1.dp, if (selected) pal.accent else pal.muted.copy(alpha = .3f))
    ) {
        Text(
            label,
            color = if (selected) pal.onAccent else pal.text,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroup(title: String, options: List<String>, selected: Int, pal: Palette, onSelect: (Int) -> Unit) {
    Label(title, pal)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, n -> Chip(n, selected == i, pal) { onSelect(i) } }
    }
}

@Composable
private fun ThemeChip(t: AppTheme, selected: Boolean, pal: Palette, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = t.bg,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) pal.accent else pal.muted.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(t.surface, t.accent, t.accent2, t.comp1, t.comp2).forEach { c ->
                    Box(Modifier.size(18.dp).clip(CircleShape).background(c).border(0.5.dp, t.text.copy(alpha = 0.25f), CircleShape))
                }
            }
            Text(t.name, color = t.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SettingSlider(title: String, value: Int, max: Int, pal: Palette, min: Int = 0, onChange: (Int) -> Unit) {
    Label(title, pal)
    Slider(
        value = value.toFloat(), onValueChange = { onChange(it.toInt()) }, valueRange = min.toFloat()..max.toFloat(),
        colors = SliderDefaults.colors(
            thumbColor = pal.accent, activeTrackColor = pal.accent, inactiveTrackColor = pal.muted.copy(alpha = .3f)
        )
    )
}

/** Wallpaper image (photo or album art) with blur and dimming. */
@Composable
private fun WallpaperLayer(s: AppSettings, pal: Palette, photo: ImageBitmap?, art: ImageBitmap?) {
    val wp = when (s.wallType) { 1 -> photo; 2 -> art; else -> null }
    if (wp != null) {
        Box(Modifier.fillMaxSize().clipToBounds()) {
            Image(
                wp, null,
                Modifier.fillMaxSize().then(if (s.wallBlur > 0) Modifier.blur(s.wallBlur.dp) else Modifier),
                contentScale = ContentScale.Crop
            )
            Box(Modifier.fillMaxSize().background((if (pal.light) Color.White else Color.Black).copy(alpha = s.wallDim / 100f)))
        }
    }
}

@Composable
private fun WallTile(label: String, selected: Boolean, pal: Palette, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)
    ) {
        Box(
            Modifier.size(72.dp, 100.dp).clip(RoundedCornerShape(14.dp))
                .border(if (selected) 2.dp else 1.dp, if (selected) pal.accent else pal.muted.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            content = content
        )
        Text(label, color = if (selected) pal.accent else pal.muted, fontSize = 11.sp, maxLines = 1)
    }
}

/** A miniature copy of the main screen (designed at 300 x 465 dp) used for live previews. */
@Composable
private fun PreviewScreen(d: AppSettings, p: Palette, photo: ImageBitmap?, art: ImageBitmap?) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(p.top, p.bg, p.bg)))) {
        WallpaperLayer(d, p, photo, art)
        Box(
            Modifier.offset(110.dp, (-100).dp).size(300.dp)
                .background(Brush.radialGradient(listOf(p.glow.copy(alpha = 0.38f), Color.Transparent)), CircleShape)
        )
        Box(
            Modifier.align(Alignment.BottomStart).offset((-100).dp, 100.dp).size(280.dp)
                .background(Brush.radialGradient(listOf(p.glow2.copy(alpha = 0.32f), Color.Transparent)), CircleShape)
        )
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text("SONARA", color = p.accent, fontSize = 11.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                Text("Your sound, your space.", color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            GlassCard(p, Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    VinylIcon(p.accent, p.text, Modifier.size(48.dp))
                    Text("Midnight Drive", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Local audio", color = p.muted, fontSize = 12.sp)
                    VizView(d, true, p.accent, p.text, p.comp1, p.comp2, Modifier.fillMaxWidth().height(listOf(28, 40, 64, 96)[d.vizHeight.coerceIn(0, 3)].dp))
                }
            }
            GlassCard(p, Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SkipPrevious, null, tint = p.text)
                    Box(Modifier.size(40.dp).clip(CircleShape).background(p.accent), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, null, tint = p.onAccent)
                    }
                    Icon(Icons.Default.SkipNext, null, tint = p.text)
                    Icon(Icons.Default.Favorite, null, tint = p.comp2)
                }
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.panel).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VinylIcon(p.accent, p.text, Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Neon Rain", color = p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Local audio · Music", color = p.muted, fontSize = 11.sp)
                }
            }
        }
    }
}

/**
 * Full-screen appearance window. The preview stays pinned at the top so every change is visible
 * without scrolling; the controls live in sub-windows (tabs) underneath. Nothing is saved until Apply.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomizeWindow(
    d: AppSettings,
    pal: Palette,
    songColor: Color?,
    art: ImageBitmap?,
    photo: ImageBitmap?,
    onPickPhoto: () -> Unit,
    onRecrop: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
) {
    val dpal = buildPalette(d, songColor)
    var tab by remember { mutableIntStateOf(0) }
    val panelScroll = rememberScrollState()
    LaunchedEffect(tab) { panelScroll.scrollTo(0) }
    val panelH = LocalConfiguration.current.screenHeightDp.dp * 0.36f
    val theme = THEMES[d.palette.coerceIn(0, THEMES.lastIndex)]

    Column(
        Modifier.fillMaxSize().background(pal.bg)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            .statusBarsPadding().navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel) { Text("Cancel", color = pal.muted) }
            Text("Customize", color = pal.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Button(onClick = onApply, colors = ButtonDefaults.buttonColors(containerColor = pal.accent, contentColor = pal.onAccent)) {
                Text("Apply")
            }
        }

        // pinned live preview: as large as the space allows
        BoxWithConstraints(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val k = minOf(maxWidth.value / 300f, maxHeight.value / 465f)
            val baseDensity = LocalDensity.current
            Box(
                Modifier.size((300f * k).dp, (465f * k).dp).clip(RoundedCornerShape(24.dp))
                    .border(1.dp, pal.muted.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            ) {
                // the preview is designed at 300 x 465 dp; scaling the density shrinks it exactly to fit
                CompositionLocalProvider(LocalDensity provides Density(baseDensity.density * k, baseDensity.fontScale)) {
                    Box(Modifier.size(300.dp, 465.dp)) { PreviewScreen(d, dpal, photo, art) }
                }
            }
        }

        // sub-window tabs
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Theme", "Wallpaper", "Adjust", "Effects", "Visualizer").forEachIndexed { i, n ->
                Chip(n, tab == i, pal) { tab = i }
            }
        }

        Column(
            Modifier.fillMaxWidth().height(panelH).verticalScroll(panelScroll)
                .padding(horizontal = 20.dp).padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (tab) {
                0 -> {
                    Label("LIGHT", pal)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        THEMES.forEachIndexed { i, t -> if (t.light) ThemeChip(t, d.palette == i, pal) { d.update("palette", i) } }
                    }
                    Label("DARK", pal)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        THEMES.forEachIndexed { i, t -> if (!t.light) ThemeChip(t, d.palette == i, pal) { d.update("palette", i) } }
                    }
                }
                1 -> {
                    Label("SOURCE", pal)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        WallTile("Gradient", d.wallType == 0, pal, { d.update("walltype", 0) }) {
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(lerp(theme.bg, wallTint(theme, d.wallpaper), 0.65f), theme.bg))))
                        }
                        WallTile("My photo", d.wallType == 1, pal, { if (photo == null) onPickPhoto() else d.update("walltype", 1) }) {
                            if (photo != null) Image(photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Text("+", color = pal.muted, fontSize = 28.sp, modifier = Modifier.align(Alignment.Center))
                        }
                        WallTile("Album art", d.wallType == 2, pal, { d.update("walltype", 2) }) {
                            if (art != null) Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Text("play a song first", color = pal.muted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(4.dp))
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("Choose and crop photo…", false, pal, onPickPhoto)
                        if (photo != null) Chip("Crop again", false, pal, onRecrop)
                    }
                    Label("GRADIENT STYLE", pal)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        WALLS.forEachIndexed { i, w ->
                            WallTile(w.name, d.wallpaper == i, pal, { d.update("wallpaper", i); d.update("walltype", 0) }) {
                                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(lerp(theme.bg, wallTint(theme, i), 0.65f), theme.bg))))
                            }
                        }
                    }
                }
                2 -> {
                    SettingSlider("WALLPAPER BLUR  ${d.wallBlur}", d.wallBlur, 30, pal) { d.update("wallblur", it) }
                    Text("Blur needs Android 12 or newer.", color = pal.muted, fontSize = 12.sp)
                    SettingSlider("WALLPAPER DIMMING  ${d.wallDim}%", d.wallDim, 80, pal) { d.update("walldim", it) }
                    SettingSlider("GLASS TRANSPARENCY  ${d.glass}%", d.glass, 100, pal) { d.update("glass", it) }
                }
                3 -> {
                    ChipGroup("SONG-COLORED SHADOW AND GLOW", listOf("Off", "On"), d.songGlow, pal) { d.update("songglow", it) }
                    ChipGroup("ACCENT FOLLOWS THE SONG", listOf("Off", "On"), d.dynAccent, pal) { d.update("dynaccent", it) }
                    ChipGroup("PAGE COLOR SHIFT (favorites, playlists, folders)", listOf("Off", "On"), d.pageTint, pal) { d.update("pagetint", it) }
                    ChipGroup("SPINNING DISC ON THE PLAYING SONG", listOf("Off", "On"), d.spin, pal) { d.update("spin", it) }
                }
                else -> {
                    ChipGroup("STYLE", VIZ_NAMES, d.visualizer, pal) { d.update("visualizer", it) }
                    ChipGroup("BARS", listOf("16", "24", "32", "48"), listOf(16, 24, 32, 48).indexOf(d.vizBars).coerceAtLeast(0), pal) {
                        d.update("vizbars", listOf(16, 24, 32, 48)[it])
                    }
                    ChipGroup("COLOR", listOf("Accent", "Palette mix", "Neutral", "Gradient"), d.vizColor, pal) { d.update("vizcolor", it) }
                    ChipGroup("SENSITIVITY", listOf("Low", "Medium", "High"), d.vizSens, pal) { d.update("vizsens", it) }
                    ChipGroup("HEIGHT", listOf("Compact", "Normal", "Tall", "Big"), d.vizHeight, pal) { d.update("vizheight", it) }
                    ChipGroup("BAR WIDTH", listOf("Thin", "Normal", "Thick"), d.vizThick, pal) { d.update("vizthick", it) }
                    ChipGroup("BAR SHAPE", listOf("Rounded", "Square"), d.vizShape, pal) { d.update("vizshape", it) }
                    ChipGroup("MIRROR (bass in the middle)", listOf("Off", "On"), d.vizMirror, pal) { d.update("vizmirror", it) }
                }
            }
        }
    }
}

/** Manual crop: drag and pinch the photo inside a frame shaped like the phone screen. */
@Composable
private fun CropScreen(bmp: Bitmap, screenAspect: Float, onCancel: () -> Unit, onDone: (Int, Int, Int, Int) -> Unit) {
    val density = LocalDensity.current
    val image = remember(bmp) { bmp.asImageBitmap() }
    var scale by remember(bmp) { mutableFloatStateOf(1f) }
    var offX by remember(bmp) { mutableFloatStateOf(0f) }
    var offY by remember(bmp) { mutableFloatStateOf(0f) }

    Column(
        Modifier.fillMaxSize().background(Color.Black)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            .statusBarsPadding().navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel) { Text("Cancel", color = Color.White) }
            Text("Crop wallpaper", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(64.dp))
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val maxWpx = with(density) { maxWidth.toPx() }
            val maxHpx = with(density) { maxHeight.toPx() }
            val fitsWidth = maxWpx * 0.9f * screenAspect <= maxHpx * 0.74f
            val fw = if (fitsWidth) maxWpx * 0.9f else maxHpx * 0.74f / screenAspect
            val fh = fw * screenAspect
            val s0 = maxOf(fw / bmp.width, fh / bmp.height)
            val dispW = bmp.width * s0 * scale
            val dispH = bmp.height * s0 * scale

            Box(
                Modifier.fillMaxSize().pointerInput(fw, fh, s0) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val ns = (scale * zoom).coerceIn(1f, 6f)
                        val mx = ((bmp.width * s0 * ns - fw) / 2f).coerceAtLeast(0f)
                        val my = ((bmp.height * s0 * ns - fh) / 2f).coerceAtLeast(0f)
                        scale = ns
                        offX = (offX + pan.x).coerceIn(-mx, mx)
                        offY = (offY + pan.y).coerceIn(-my, my)
                    }
                },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(with(density) { fw.toDp() }, with(density) { fh.toDp() })
                        .clipToBounds().border(2.dp, Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        image, null,
                        Modifier.requiredSize(with(density) { dispW.toDp() }, with(density) { dispH.toDp() })
                            .graphicsLayer {
                                translationX = offX
                                translationY = offY
                            },
                        contentScale = ContentScale.FillBounds
                    )
                }
            }
            Text(
                "Drag to move  ·  pinch to zoom", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp)
            )
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { scale = 1f; offX = 0f; offY = 0f }) { Text("Reset", color = Color.White) }
                Button(onClick = {
                    val f = 1f / (s0 * scale)
                    val x0 = (dispW / 2f - fw / 2f - offX) * f
                    val y0 = (dispH / 2f - fh / 2f - offY) * f
                    onDone(x0.toInt(), y0.toInt(), (fw * f).toInt(), (fh * f).toInt())
                }) { Text("Done") }
            }
        }
    }
}

@Composable
private fun VizView(s: AppSettings, playing: Boolean, color: Color, alt: Color, c1: Color, c2: Color, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "viz")
    val phase by t.animateFloat(
        0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "phase"
    )
    val amp by animateFloatAsState(if (playing) 1f else 0.1f, tween(500), label = "amp")
    val style = s.visualizer
    val reactive = s.reactive == 1
    val sens = listOf(0.7f, 1f, 1.4f)[s.vizSens.coerceIn(0, 2)]
    val n = s.vizBars
    val mirror = s.vizMirror == 1
    Canvas(modifier) {
        val levels = if (reactive && playing) PlayerHolder.vizLevels else null
        val w = size.width
        val h = size.height
        // strength 0..1 for item i of cnt: real audio when reactive, otherwise a smooth animation
        fun a(i: Int, cnt: Int, speed: Float): Float {
            val raw = if (levels != null) levels[(i * levels.size / cnt).coerceIn(0, levels.size - 1)]
            else (0.1f + 0.9f * (0.5f + 0.5f * sin(phase * speed + i * 0.8f))) * amp
            return (raw * sens).coerceIn(0f, 1f)
        }
        fun colorAt(i: Int, cnt: Int): Color = when (s.vizColor) {
            1 -> {
                val stops = listOf(color, c1, c2, color)
                val pos = (i.toFloat() / cnt) * 3f
                val k = pos.toInt().coerceIn(0, 2)
                lerp(stops[k], stops[k + 1], pos - k)
            }
            2 -> alt
            3 -> lerp(color, c1, i.toFloat() / cnt)
            else -> color
        }
        when (style) {
            1 -> {
                val path = Path()
                val steps = 60
                for (i in 0..steps) {
                    val x = w * i / steps
                    val src = if (mirror) kotlin.math.abs(i - steps / 2) * 2 else i
                    val v = a(src, steps + 1, 1f)
                    val y = h / 2 + sin(phase * 2f + i * 0.28f) * h * 0.45f * v
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                val brush = Brush.horizontalGradient(listOf(colorAt(0, 3), colorAt(1, 3), colorAt(2, 3)))
                drawPath(path, brush, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
            }
            2 -> {
                val cnt = (n / 2).coerceAtLeast(8)
                val gap = w / cnt
                for (i in 0 until cnt) {
                    val src = if (mirror) kotlin.math.abs(i - cnt / 2) * 2 else i
                    val v = a(src, cnt, 1f + (i % 4) * 0.5f)
                    drawCircle(
                        colorAt(i, cnt).copy(alpha = (0.35f + 0.65f * v).coerceIn(0f, 1f)),
                        radius = (2f + 7f * v).dp.toPx(),
                        center = Offset(gap * i + gap / 2f, h / 2f)
                    )
                }
            }
            3 -> {
                val bass = if (levels != null) (levels.take(6).average().toFloat() * sens).coerceIn(0f, 1f) else amp * 0.5f
                for (k in 0 until 3) {
                    val f = ((phase / (2f * Math.PI.toFloat())) + k / 3f) % 1f
                    drawCircle(
                        colorAt(k, 3).copy(alpha = ((1f - f) * 0.7f * (0.3f + 0.7f * maxOf(bass, amp * 0.4f))).coerceIn(0f, 1f)),
                        radius = h * (0.1f + 0.5f * f) * (0.8f + 0.6f * bass),
                        center = Offset(w / 2f, h / 2f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
            else -> {
                val slot = w / n
                val bw = slot * listOf(0.35f, 0.6f, 0.85f)[s.vizThick.coerceIn(0, 2)]
                val half = n / 2
                for (i in 0 until n) {
                    val v = if (mirror) {
                        val j = if (i < half) half - 1 - i else i - half
                        a(j, half, 1f + (j % 5) * 0.7f)
                    } else {
                        a(i, n, 1f + (i % 5) * 0.7f)
                    }
                    val bh = h * (0.08f + 0.92f * v).coerceIn(0f, 1f)
                    drawRoundRect(
                        colorAt(i, n).copy(alpha = .9f),
                        topLeft = Offset(i * slot + (slot - bw) / 2f, (h - bh) / 2f),
                        size = Size(bw, bh),
                        cornerRadius = if (s.vizShape == 1) CornerRadius(0f, 0f) else CornerRadius(bw / 2f)
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

private fun folderLabel(f: String): String = Uri.parse(f).lastPathSegment ?: f

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsSheet(
    s: AppSettings,
    pal: Palette,
    folders: List<String>,
    scanStatus: String,
    onAddFolder: () -> Unit,
    onRescan: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onReactive: (Boolean) -> Unit,
    onScanDevice: () -> Unit,
    onRestoreHidden: () -> Unit,
    onCustomize: () -> Unit,
) {
    SheetBody {
        Text("Settings", color = pal.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Chip("Customize appearance…", false, pal, onCustomize)
        Text("Themes, wallpaper, glass, shadows and the visualizer have their own window, so a stray touch can't change them.", color = pal.muted, fontSize = 12.sp)
        Label("AUDIO-REACTIVE", pal)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("On", s.reactive == 1, pal) { onReactive(true) }
            Chip("Off", s.reactive == 0, pal) { onReactive(false) }
        }
        Text(
            "Makes the visualizer move with the music. Android asks for the microphone permission for this, but Sonara only reads its own playback and never records or stores audio.",
            color = pal.muted, fontSize = 12.sp
        )
        Label("LIBRARY SORT", pal)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SORT_NAMES.forEachIndexed { i, n -> Chip(n, s.sort == i, pal) { s.update("sort", i); s.update("sortdesc", 0) } }
        }
        ChipGroup("SORT DIRECTION", listOf("Normal", "Reverse"), s.sortDesc, pal) { s.update("sortdesc", it) }
        Label("DEVICE MUSIC", pal)
        Text("Sonara finds the music on your phone automatically every time it opens and plays each file straight from its location.", color = pal.muted, fontSize = 13.sp)
        ChipGroup("PLAY EACH FOLDER SEPARATELY", listOf("Off", "On"), s.folderSep, pal) { s.update("foldersep", it) }
        Text("On: tapping a song plays only the songs from the same folder, and next / previous stay inside it.", color = pal.muted, fontSize = 12.sp)
        Label("IGNORE SONGS SHORTER THAN", pal)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 30, 60).forEach { sec ->
                Chip(if (sec == 0) "Nothing" else "${sec}s", s.minLenSec == sec, pal) { s.update("minlen", sec) }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Scan device now", false, pal, onScanDevice)
            Chip("Restore removed songs", false, pal, onRestoreHidden)
        }
        Label("MUSIC FOLDERS", pal)
        if (folders.isEmpty()) Text("No folders added yet.", color = pal.muted, fontSize = 13.sp)
        folders.forEach { f ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(folderLabel(f), color = pal.text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                TextButton(onClick = { onRemoveFolder(f) }) { Text("Remove", color = pal.muted) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("+ Add folder", false, pal, onAddFolder)
            Chip("Rescan", false, pal, onRescan)
        }
        if (scanStatus.isNotEmpty()) Text(scanStatus, color = pal.accent, fontSize = 12.sp)
        Text("Songs found are added to your library. Removing a folder here doesn't delete its songs from the library.", color = pal.muted, fontSize = 12.sp)
        Label("HOME-SCREEN WIDGET", pal)
        Text("Long-press an empty spot on your home screen, open Widgets and add Sonara.", color = pal.muted, fontSize = 13.sp)
        Text("Sonara 2.7", color = pal.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
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
            colors = ButtonDefaults.buttonColors(containerColor = pal.accent, contentColor = pal.onAccent)
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
            Button(onClick = onLoad, colors = ButtonDefaults.buttonColors(containerColor = pal.accent, contentColor = pal.onAccent)) {
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
