package com.mediaxplayer.app

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

@SuppressLint("UnsafeOptInUsageError")
class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private var everPlayed = false
    private var retries = 0
    private val ui = Handler(Looper.getMainLooper())

    companion object {
        private const val UA =
            "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Mobile Safari/537.36"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 28) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideBars()

        val url = intent.getStringExtra("url").orEmpty()

        val playerView = PlayerView(this).apply {
            useController = true
            controllerShowTimeoutMs = 3000
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
        }
        setContentView(playerView)

        startPlayer(playerView, url)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun hideBars() {
        val c = WindowInsetsControllerCompat(window, window.decorView)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        c.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideBars()
    }

    private fun startPlayer(playerView: PlayerView, url: String) {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(UA)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(30_000, 120_000, 1_500, 4_000)
            .build()

        val renderers = DefaultRenderersFactory(this).setEnableDecoderFallback(true)

        val exo = ExoPlayer.Builder(this, renderers)
            .setMediaSourceFactory(DefaultMediaSourceFactory(http))
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .setHandleAudioBecomingNoisy(true)
            .build()
        exo.setAudioAttributes(AudioAttributes.DEFAULT, true)

        val item = MediaItem.Builder().setUri(url)
        mimeFor(url)?.let { item.setMimeType(it) }

        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    everPlayed = true
                    retries = 0
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                handleError(error)
            }
        })

        playerView.player = exo
        player = exo
        exo.setMediaItem(item.build())
        exo.playWhenReady = true
        exo.prepare()
    }

    private fun handleError(e: PlaybackException) {
        val p = player ?: return
        val netError = e.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
            e.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
        val maxRetry = if (everPlayed) 6 else 2

        when {
            e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> {
                p.seekToDefaultPosition()
                p.prepare()
            }
            netError && retries < maxRetry -> {
                retries++
                ui.postDelayed({ player?.prepare() }, 1500)
            }
            else -> {
                Toast.makeText(this, "यह लिंक नहीं चल पाया। कोई और लिंक आज़माओ।", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mimeFor(u: String): String? {
        val l = u.lowercase()
        return when {
            l.contains(".m3u8") -> MimeTypes.APPLICATION_M3U8
            l.contains(".mpd") -> MimeTypes.APPLICATION_MPD
            else -> null
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onDestroy() {
        ui.removeCallbacksAndMessages(null)
        player?.release()
        player = null
        super.onDestroy()
    }
}
