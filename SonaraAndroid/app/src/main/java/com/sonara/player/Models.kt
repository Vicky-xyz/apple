package com.sonara.player

import android.content.Context
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File

data class Song(
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val fav: Boolean = false,
    val path: String = "",
)

data class LyricLine(val timeMs: Long, val text: String)

data class Playlist(val id: String, val name: String, val uris: List<String>)

fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(uri)
    .setUri(Uri.parse(uri))
    .setMediaMetadata(
        MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album).build()
    )
    .build()

private val collator: java.text.Collator =
    java.text.Collator.getInstance().apply { strength = java.text.Collator.PRIMARY }

private fun sortKey(s: String, skipArticles: Boolean): String {
    var t = s.trim().lowercase()
    if (skipArticles) {
        for (p in listOf("the ", "an ", "a ")) {
            if (t.startsWith(p) && t.length > p.length) {
                t = t.substring(p.length)
                break
            }
        }
    }
    return t
}

/** Case- and accent-insensitive compare where numbers are ordered by value (2 before 10). */
fun naturalCompare(a: String, b: String, skipArticles: Boolean = true): Int {
    val x = sortKey(a, skipArticles)
    val y = sortKey(b, skipArticles)
    var i = 0
    var j = 0
    while (i < x.length && j < y.length) {
        if (x[i].isDigit() && y[j].isDigit()) {
            var ie = i
            while (ie < x.length && x[ie].isDigit()) ie++
            var je = j
            while (je < y.length && y[je].isDigit()) je++
            val nx = x.substring(i, ie).trimStart('0')
            val ny = y.substring(j, je).trimStart('0')
            if (nx.length != ny.length) return nx.length - ny.length
            val c = nx.compareTo(ny)
            if (c != 0) return c
            i = ie
            j = je
        } else {
            val c = collator.compare(x[i].toString(), y[j].toString())
            if (c != 0) return c
            i++
            j++
        }
    }
    return (x.length - i) - (y.length - j)
}

/**
 * mode: 0 recently added, 1 title, 2 artist, 3 length, 4 album, 5 folder, 6 most played, 7 favorites first.
 * In a folder view the default order is by file name, so albums play in track order.
 */
fun sortSongs(list: List<Song>, mode: Int, desc: Boolean, plays: Map<String, Int>, folderView: Boolean): List<Song> {
    val title = Comparator<Song> { a, b -> naturalCompare(a.title, b.title) }
    val artist = Comparator<Song> { a, b -> naturalCompare(a.artist, b.artist) }
    val album = Comparator<Song> { a, b ->
        val ab = a.album.isBlank()
        val bb = b.album.isBlank()
        if (ab != bb) (if (ab) 1 else -1) else naturalCompare(a.album, b.album)
    }
    val file = Comparator<Song> { a, b ->
        naturalCompare(a.path.substringAfterLast('/'), b.path.substringAfterLast('/'), false)
    }
    val folder = Comparator<Song> { a, b ->
        naturalCompare(a.path.substringBeforeLast('/', ""), b.path.substringBeforeLast('/', ""), false)
    }
    val cmp: Comparator<Song>? = when (mode) {
        1 -> title.then(artist)
        2 -> artist.then(album).then(title)
        3 -> Comparator<Song> { a, b -> a.durationMs.compareTo(b.durationMs) }.then(title)
        4 -> album.then(artist).then(title)
        5 -> folder.then(file)
        6 -> Comparator<Song> { a, b -> (plays[b.uri] ?: 0).compareTo(plays[a.uri] ?: 0) }.then(title)
        7 -> Comparator<Song> { a, b -> (if (b.fav) 1 else 0).compareTo(if (a.fav) 1 else 0) }.then(title)
        else -> if (folderView) file else null
    }
    val sorted = if (cmp != null) list.sortedWith(cmp) else list
    return if (desc) sorted.reversed() else sorted
}

private val TS = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

fun parseLrc(raw: String): List<LyricLine> {
    val out = mutableListOf<LyricLine>()
    for (line in raw.lines()) {
        val stamps = TS.findAll(line).toList()
        if (stamps.isEmpty()) {
            val t = line.trim()
            if (t.isNotEmpty() && !t.startsWith("[")) out.add(LyricLine(-1L, t))
            continue
        }
        val text = line.replace(TS, "").trim()
        for (m in stamps) {
            val mm = m.groupValues[1].toLong()
            val ss = m.groupValues[2].toLong()
            val fr = m.groupValues[3]
            val frac = when (fr.length) {
                0 -> 0L
                1 -> fr.toLong() * 100
                2 -> fr.toLong() * 10
                else -> fr.take(3).toLong()
            }
            out.add(LyricLine((mm * 60 + ss) * 1000 + frac, text))
        }
    }
    return if (out.any { it.timeMs >= 0 }) out.filter { it.timeMs >= 0 }.sortedBy { it.timeMs } else out
}

fun clock(ms: Long): String {
    val sec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(sec / 60, sec % 60)
}

object Library {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("sonara_library", Context.MODE_PRIVATE)

    fun load(ctx: Context): List<Song> = try {
        val arr = JSONArray(prefs(ctx).getString("songs", "[]"))
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Song(
                o.getString("uri"),
                o.optString("title", "Unknown track"),
                o.optString("artist", "Unknown artist"),
                o.optString("album", ""),
                o.optLong("dur", 0L),
                o.optBoolean("fav", false),
                o.optString("path", ""),
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun save(ctx: Context, songs: List<Song>) {
        val arr = JSONArray()
        songs.forEach {
            arr.put(
                JSONObject()
                    .put("uri", it.uri).put("title", it.title).put("artist", it.artist)
                    .put("album", it.album).put("dur", it.durationMs).put("fav", it.fav).put("path", it.path)
            )
        }
        prefs(ctx).edit().putString("songs", arr.toString()).apply()
    }

    private fun fileName(ctx: Context, uri: Uri): String {
        var name: String? = null
        try {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) name = c.getString(0)
            }
        } catch (_: Exception) {
        }
        val n = name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Unknown track"
        return n.substringBeforeLast('.').replace('_', ' ')
    }

    fun read(ctx: Context, uri: Uri): Song {
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var dur = 0L
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(ctx, uri)
            title = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            album = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            dur = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
        } finally {
            try { r.release() } catch (_: Exception) {}
        }
        val t: String = title?.takeIf { it.isNotBlank() } ?: fileName(ctx, uri)
        val a: String = artist?.takeIf { it.isNotBlank() } ?: "Unknown artist"
        return Song(uri.toString(), t, a, album ?: "", dur)
    }

    fun art(ctx: Context, uri: String): ByteArray? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(ctx, Uri.parse(uri))
            val raw = r.embeddedPicture ?: return null
            val bmp = BitmapFactory.decodeByteArray(raw, 0, raw.size) ?: return null
            val scale = 512f / maxOf(bmp.width, bmp.height)
            val small = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bmp,
                    (bmp.width * scale).toInt().coerceAtLeast(1),
                    (bmp.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else bmp
            val out = ByteArrayOutputStream()
            small.compress(Bitmap.CompressFormat.JPEG, 88, out)
            out.toByteArray()
        } catch (e: Throwable) {
            null
        } finally {
            try { r.release() } catch (_: Exception) {}
        }
    }

    private fun lyricFile(ctx: Context, uri: String): File {
        val dir = File(ctx.filesDir, "lyrics")
        dir.mkdirs()
        return File(dir, Integer.toHexString(uri.hashCode()) + ".lrc")
    }

    fun loadLyrics(ctx: Context, uri: String): List<LyricLine> {
        val f = lyricFile(ctx, uri)
        return if (f.exists()) parseLrc(f.readText()) else emptyList()
    }

    fun saveLyrics(ctx: Context, uri: String, text: String) = lyricFile(ctx, uri).writeText(text)

    fun deleteLyrics(ctx: Context, uri: String) {
        lyricFile(ctx, uri).delete()
    }

    // ----- automatic device scan (reads the phone's media database; files are played by their path/URI) -----
    @Suppress("DEPRECATION")
    fun scanDevice(ctx: Context, minMs: Long, hidden: Set<String>): List<Song> {
        val out = mutableListOf<Song>()
        val volume = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val proj = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )
        val sel = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " + MediaStore.Audio.Media.DURATION + " >= ?"
        try {
            ctx.contentResolver.query(
                volume, proj, sel, arrayOf(minMs.toString()),
                MediaStore.Audio.Media.DATE_ADDED + " DESC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()
                    if (uri in hidden) continue
                    val name = c.getString(6) ?: ""
                    val title = c.getString(1)?.takeIf { it.isNotBlank() }
                        ?: name.substringBeforeLast('.').ifBlank { "Unknown track" }
                    val artistRaw = c.getString(2)
                    val artist = if (artistRaw.isNullOrBlank() || artistRaw == "<unknown>") "Unknown artist" else artistRaw
                    out.add(Song(uri, title, artist, c.getString(3) ?: "", c.getLong(4), false, c.getString(5) ?: ""))
                }
            }
        } catch (_: Exception) {
        }
        return out
    }

    /** Replaces device songs with the fresh scan, keeping favorites and hand-picked files. */
    fun merge(existing: List<Song>, scanned: List<Song>): List<Song> {
        if (scanned.isEmpty()) return existing
        val favs = existing.filter { it.fav }.map { it.uri }.toSet()
        val keys = scanned.map { it.title.lowercase() to it.durationMs }.toSet()
        val fresh = scanned.map { if (it.uri in favs) it.copy(fav = true) else it }
        val others = existing.filter {
            !it.uri.startsWith("content://media/") && (it.title.lowercase() to it.durationMs) !in keys
        }
        return fresh + others
    }

    fun loadHidden(ctx: Context): Set<String> = try {
        val arr = JSONArray(prefs(ctx).getString("hidden", "[]"))
        List(arr.length()) { arr.getString(it) }.toSet()
    } catch (e: Exception) {
        emptySet()
    }

    fun saveHidden(ctx: Context, hidden: Set<String>) {
        prefs(ctx).edit().putString("hidden", JSONArray(hidden.toList()).toString()).apply()
    }

    /** Average color of an image, weighted toward vivid pixels (used to tint the glow). */
    fun dominantColor(bmp: Bitmap): Int {
        val small = Bitmap.createScaledBitmap(bmp, 24, 24, true)
        var r = 0.0
        var g = 0.0
        var b = 0.0
        var total = 0.0
        val hsv = FloatArray(3)
        for (y in 0 until 24) {
            for (x in 0 until 24) {
                val px = small.getPixel(x, y)
                android.graphics.Color.colorToHSV(px, hsv)
                val w = 0.05 + hsv[1] * hsv[2]
                r += android.graphics.Color.red(px) * w
                g += android.graphics.Color.green(px) * w
                b += android.graphics.Color.blue(px) * w
                total += w
            }
        }
        if (total <= 0.0) return android.graphics.Color.GRAY
        return android.graphics.Color.rgb((r / total).toInt(), (g / total).toInt(), (b / total).toInt())
    }

    // ----- play counts (for "Most played") -----
    fun loadPlays(ctx: Context): Map<String, Int> = try {
        val o = JSONObject(prefs(ctx).getString("plays", "{}"))
        o.keys().asSequence().associateWith { o.getInt(it) }
    } catch (e: Exception) {
        emptyMap()
    }

    fun incPlay(ctx: Context, uri: String) {
        val o = try { JSONObject(prefs(ctx).getString("plays", "{}")) } catch (e: Exception) { JSONObject() }
        o.put(uri, o.optInt(uri, 0) + 1)
        prefs(ctx).edit().putString("plays", o.toString()).apply()
    }

    // ----- custom wallpaper -----
    fun saveWallpaper(ctx: Context, uri: Uri, name: String = "wallpaper.jpg"): Boolean {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1800) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return false
            File(ctx.filesDir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            return true
        } catch (e: Throwable) {
            return false
        }
    }

    fun loadWallpaper(ctx: Context, name: String = "wallpaper.jpg"): ImageBitmap? {
        val f = File(ctx.filesDir, name)
        if (!f.exists()) return null
        return try { BitmapFactory.decodeFile(f.path)?.asImageBitmap() } catch (e: Throwable) { null }
    }

    fun promoteDraftWallpaper(ctx: Context) {
        val pairs = listOf("wallpaper_draft.jpg" to "wallpaper.jpg", "wallpaper_src_draft.jpg" to "wallpaper_src.jpg")
        for ((from, to) in pairs) {
            val f = File(ctx.filesDir, from)
            if (f.exists()) f.copyTo(File(ctx.filesDir, to), overwrite = true)
        }
    }

    fun clearDraftWallpaper(ctx: Context) {
        File(ctx.filesDir, "wallpaper_draft.jpg").delete()
        File(ctx.filesDir, "wallpaper_src_draft.jpg").delete()
    }

    /** Decodes a picked photo (downsampled, rotated upright) so it can be cropped. */
    fun decodeSampled(ctx: Context, uri: Uri, maxSide: Int): Bitmap? {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return null
            val orientation = try {
                ctx.contentResolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (e: Throwable) {
                ExifInterface.ORIENTATION_NORMAL
            }
            val deg = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            return if (deg != 0f) {
                Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(deg) }, true)
            } else bmp
        } catch (e: Throwable) {
            return null
        }
    }

    fun saveBitmap(ctx: Context, bmp: Bitmap, name: String) {
        File(ctx.filesDir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    }

    fun loadBitmap(ctx: Context, name: String): Bitmap? {
        val f = File(ctx.filesDir, name)
        if (!f.exists()) return null
        return try { BitmapFactory.decodeFile(f.path) } catch (e: Throwable) { null }
    }

    /** Saves the rectangle the user framed (in source-image pixels) as the wallpaper picture. */
    fun cropAndSave(ctx: Context, src: Bitmap, x: Int, y: Int, w: Int, h: Int, name: String): Boolean {
        try {
            val cx = x.coerceIn(0, src.width - 1)
            val cy = y.coerceIn(0, src.height - 1)
            val cw = w.coerceIn(1, src.width - cx)
            val ch = h.coerceIn(1, src.height - cy)
            var out = Bitmap.createBitmap(src, cx, cy, cw, ch)
            val longSide = maxOf(out.width, out.height)
            if (longSide > 1800) {
                val sc = 1800f / longSide
                out = Bitmap.createScaledBitmap(
                    out, (out.width * sc).toInt().coerceAtLeast(1), (out.height * sc).toInt().coerceAtLeast(1), true
                )
            }
            saveBitmap(ctx, out, name)
            return true
        } catch (e: Throwable) {
            return false
        }
    }

    // ----- playlists -----
    fun loadPlaylists(ctx: Context): List<Playlist> = try {
        val arr = JSONArray(prefs(ctx).getString("playlists", "[]"))
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val u = o.optJSONArray("uris") ?: JSONArray()
            Playlist(o.getString("id"), o.optString("name", "Playlist"), List(u.length()) { j -> u.getString(j) })
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun savePlaylists(ctx: Context, list: List<Playlist>) {
        val arr = JSONArray()
        list.forEach { pl ->
            val u = JSONArray()
            pl.uris.forEach { u.put(it) }
            arr.put(JSONObject().put("id", pl.id).put("name", pl.name).put("uris", u))
        }
        prefs(ctx).edit().putString("playlists", arr.toString()).apply()
    }

    // ----- music folders -----
    fun loadFolders(ctx: Context): List<String> = try {
        val arr = JSONArray(prefs(ctx).getString("folders", "[]"))
        List(arr.length()) { arr.getString(it) }
    } catch (e: Exception) {
        emptyList()
    }

    fun saveFolders(ctx: Context, list: List<String>) {
        prefs(ctx).edit().putString("folders", JSONArray(list).toString()).apply()
    }

    /** Recursively finds audio files inside a folder the user granted access to. */
    fun scanTree(ctx: Context, treeUri: Uri): List<Uri> {
        val out = mutableListOf<Uri>()
        fun walk(docId: String, depth: Int) {
            if (depth > 10) return
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
            try {
                ctx.contentResolver.query(
                    children,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                    ),
                    null, null, null
                )?.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getString(0)
                        val mime = c.getString(1) ?: ""
                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            walk(id, depth + 1)
                        } else if (mime.startsWith("audio/") || mime == "application/ogg") {
                            out.add(DocumentsContract.buildDocumentUriUsingTree(treeUri, id))
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
        try {
            walk(DocumentsContract.getTreeDocumentId(treeUri), 0)
        } catch (_: Exception) {
        }
        return out
    }
}
