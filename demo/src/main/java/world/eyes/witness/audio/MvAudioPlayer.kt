package world.eyes.witness.audio

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.cardinalblue.kraftshade.pipeline.input.SampledInput
import com.cardinalblue.kraftshade.pipeline.input.sampledInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MvAudioPlayer(private val context: Context) {
    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var trackingJob: Job? = null

    private val _playbackTimeSeconds = MutableStateFlow(0f)
    val playbackTimeSeconds: StateFlow<Float> = _playbackTimeSeconds.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    val timeInput: SampledInput<Float> = sampledInput {
        _playbackTimeSeconds.value
    }

    @OptIn(UnstableApi::class)
    fun prepare(assetFilePath: String = "original/Mili - world.execute (me) ;.mp3") {
        if (exoPlayer != null) return

        val player = ExoPlayer.Builder(context).build().apply {
            val assetUri = Uri.parse("asset:///$assetFilePath")
            val dataSourceFactory = DefaultDataSource.Factory(context)
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(assetUri))

            setMediaSource(mediaSource)
            prepare()
            repeatMode = Player.REPEAT_MODE_OFF

            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    _isPlaying.value = isPlayingNow
                    if (isPlayingNow) {
                        startTracking()
                    } else {
                        stopTracking()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        _isPlaying.value = false
                        stopTracking()
                    }
                }
            })
        }
        exoPlayer = player
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun seekTo(seconds: Float) {
        val posMs = (seconds * 1000f).toLong()
        exoPlayer?.seekTo(posMs)
        _playbackTimeSeconds.value = seconds
    }

    private fun startTracking() {
        trackingJob?.cancel()
        trackingJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    val currentPos = player.currentPosition
                    _playbackTimeSeconds.value = currentPos / 1000f
                }
                delay(16) // 约 60Hz 采样频率
            }
        }
    }

    private fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        exoPlayer?.let {
            _playbackTimeSeconds.value = it.currentPosition / 1000f
        }
    }

    fun release() {
        stopTracking()
        exoPlayer?.release()
        exoPlayer = null
    }
}