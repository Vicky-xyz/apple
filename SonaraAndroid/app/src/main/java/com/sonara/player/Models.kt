package com.sonara.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
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
)

data class LyricLine(val timeMs: Long, val text: String)

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
                    .put("album", it.album).put("dur", it.durationMs).put("fav", it.fav)
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
}
