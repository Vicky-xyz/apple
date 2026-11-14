package com.sonara.player

import android.content.Context
import android.content.ContentUris
import android.graphics.Bitmap
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

    // ----- custom wallpaper -----
    fun saveWallpaper(ctx: Context, uri: Uri): Boolean {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1800) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return false
            File(ctx.filesDir, "wallpaper.jpg").outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            return true
        } catch (e: Throwable) {
            return false
        }
    }

    fun loadWallpaper(ctx: Context): ImageBitmap? {
        val f = File(ctx.filesDir, "wallpaper.jpg")
        if (!f.exists()) return null
        return try { BitmapFactory.decodeFile(f.path)?.asImageBitmap() } catch (e: Throwable) { null }
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
