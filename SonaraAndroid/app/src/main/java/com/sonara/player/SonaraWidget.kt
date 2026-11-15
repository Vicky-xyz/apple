package com.sonara.player

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb

/** Home-screen widget: cover art, title, artist and previous / play-pause / next buttons. */
class SonaraWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val v = build(context)
        for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, v)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val act = intent.action ?: return
        if (act != ACTION_PLAY && act != ACTION_NEXT && act != ACTION_PREV) return
        val p = PlayerHolder.get(context)
        if (p.mediaItemCount == 0) return
        when (act) {
            ACTION_PLAY -> {
                if (p.isPlaying) {
                    p.pause()
                } else {
                    p.play()
                    try { context.startService(Intent(context, PlaybackService::class.java)) } catch (_: Exception) {}
                }
            }
            ACTION_NEXT -> p.seekToNext()
            ACTION_PREV -> p.seekToPrevious()
        }
        update(context)
    }

    companion object {
        const val ACTION_PLAY = "com.sonara.player.WIDGET_PLAY"
        const val ACTION_NEXT = "com.sonara.player.WIDGET_NEXT"
        const val ACTION_PREV = "com.sonara.player.WIDGET_PREV"

        fun update(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, SonaraWidget::class.java))
            if (ids.isEmpty()) return
            val v = build(ctx)
            for (id in ids) mgr.updateAppWidget(id, v)
        }

        private fun broadcast(ctx: Context, action: String, code: Int): PendingIntent =
            PendingIntent.getBroadcast(
                ctx, code,
                Intent(ctx, SonaraWidget::class.java).setAction(action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        fun build(ctx: Context): RemoteViews {
            val v = RemoteViews(ctx.packageName, R.layout.widget_player)
            val p = PlayerHolder.peek()
            val item = p?.currentMediaItem
            val md = item?.mediaMetadata
            val hasQueue = item != null

            v.setTextViewText(R.id.w_title, md?.title ?: "Sonara")
            v.setTextViewText(R.id.w_artist, if (hasQueue) (md?.artist ?: "") else "Open Sonara to pick a song")
            v.setImageViewResource(R.id.w_play, if (p?.isPlaying == true) R.drawable.ic_w_pause else R.drawable.ic_w_play)

            val sp = ctx.getSharedPreferences("sonara_settings", Context.MODE_PRIVATE)
            val accent = THEMES[sp.getInt("palette", 3).coerceIn(0, THEMES.lastIndex)].accent.toArgb()
            v.setInt(R.id.w_play, "setColorFilter", accent)

            var artSet = false
            val bytes = md?.artworkData
            if (bytes != null) {
                try {
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        v.setImageViewBitmap(R.id.w_art, Bitmap.createScaledBitmap(bmp, 192, 192, true))
                        artSet = true
                    }
                } catch (_: Throwable) {
                }
            }
            if (!artSet) v.setImageViewResource(R.id.w_art, R.mipmap.ic_launcher)

            val open = PendingIntent.getActivity(
                ctx, 0,
                Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            v.setOnClickPendingIntent(R.id.w_root, open)
            v.setOnClickPendingIntent(R.id.w_prev, broadcast(ctx, ACTION_PREV, 1))
            v.setOnClickPendingIntent(R.id.w_play, broadcast(ctx, ACTION_PLAY, 2))
            v.setOnClickPendingIntent(R.id.w_next, broadcast(ctx, ACTION_NEXT, 3))
            return v
        }
    }
}
