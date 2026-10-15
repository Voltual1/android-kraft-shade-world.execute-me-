package me.voltual.asp.engine.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import me.voltual.asp.engine.PlayMusicInput
import me.voltual.asp.engine.PlaySoundInput
import me.voltual.asp.engine.StoryAsset
import me.voltual.asp.engine.StoryAudio
import me.voltual.asp.engine.StoryContext
import java.util.concurrent.ConcurrentHashMap

class StoryAudioPlayer(
    private val context: Context,
    private val storyContext: StoryContext,
    private val scope: CoroutineScope
) : StoryAudio {

    private var musicPlayer: ExoPlayer? = null
    private val soundPlayers = ConcurrentHashMap<String, ExoPlayer>()
    private var musicBaseVolume = 1f

    init {
        musicPlayer = ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    override fun destroy() {
        musicPlayer?.release()
        musicPlayer = null
        soundPlayers.values.forEach { it.release() }
        soundPlayers.clear()
    }

    override suspend fun playMusic(input: PlayMusicInput) {
        val url = resolveAudioUrl(input.key) ?: return
        musicBaseVolume = input.volume

        withContext(Dispatchers.Main) {
            val player = musicPlayer ?: return@withContext
            player.stop()
            val mediaItem = MediaItem.fromUri(url)
            player.setMediaItem(mediaItem)
            player.volume = input.volume
            player.prepare()
            player.play()
        }
    }

    override suspend fun stopMusic(fadeMs: Long) {
        withContext(Dispatchers.Main) {
            val player = musicPlayer ?: return@withContext
            if (fadeMs > 0) {
                val steps = 10
                val interval = fadeMs / steps
                val stepVolume = player.volume / steps
                for (i in 0 until steps) {
                    player.volume = (player.volume - stepVolume).coerceAtLeast(0f)
                    delay(interval)
                }
            }
            player.stop()
        }
    }

    override suspend fun setMusicVolume(volume: Float, fadeMs: Long) {
        musicBaseVolume = volume
        withContext(Dispatchers.Main) {
            musicPlayer?.volume = volume
        }
    }

    override suspend fun playSound(input: PlaySoundInput) {
        val url = resolveAudioUrl(input.key) ?: return
        withContext(Dispatchers.Main) {
            val channelKey = if (input.channel.isNotEmpty()) input.channel else input.key
            soundPlayers[channelKey]?.release()

            val soundPlayer = ExoPlayer.Builder(context).build().apply {
                repeatMode = if (input.loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
                volume = input.volume
            }
            soundPlayers[channelKey] = soundPlayer

            val mediaItem = MediaItem.fromUri(url)
            soundPlayer.setMediaItem(mediaItem)
            soundPlayer.prepare()
            soundPlayer.play()

            if (!input.loop) {
                soundPlayer.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            soundPlayer.release()
                            soundPlayers.remove(channelKey)
                        }
                    }
                })
            }
        }
    }

    override suspend fun stopSound(channel: String, fadeMs: Long) {
        withContext(Dispatchers.Main) {
            val player = soundPlayers.remove(channel)
            player?.stop()
            player?.release()
        }
    }

    override suspend fun setSoundVolume(channel: String, volume: Float, fadeMs: Long) {
        withContext(Dispatchers.Main) {
            soundPlayers[channel]?.volume = volume
        }
    }

    private fun resolveAudioUrl(key: String): String? {
        return StoryAsset.resolveStoryAudioByKey(key, storyContext.audioVariables)
    }
}