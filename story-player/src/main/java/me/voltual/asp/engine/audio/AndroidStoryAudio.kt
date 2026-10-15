package me.voltual.asp.engine.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import me.voltual.asp.engine.PlayMusicInput
import me.voltual.asp.engine.PlaySoundInput
import me.voltual.asp.engine.StoryAudio
import kotlin.math.max

class AndroidStoryAudio(private val context: Context) : StoryAudio {

    private val audioScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private var activeMusicPlayer: ExoPlayer? = null
    private var activeMusicIdentity: String? = null
    private var musicBaseVolume: Float = 1f
    private var musicRequestId: Int = 0
    private var musicFadeJob: Job? = null

    private val soundChannels = mutableMapOf<String, ExoPlayer>()
    private val soundBaseVolumes = mutableMapOf<String, Float>()
    private val soundFadeJobs = mutableMapOf<String, Job>()
    private val soundRequestIds = mutableMapOf<String, Int>()

    private var isDestroyed = false

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private fun createPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(this@AndroidStoryAudio.audioAttributes, true)
        }
    }

    override fun destroy() {
        isDestroyed = true
        audioScope.cancel()

        musicFadeJob?.cancel()
        activeMusicPlayer?.stop()
        activeMusicPlayer?.release()
        activeMusicPlayer = null
        activeMusicIdentity = null

        soundFadeJobs.values.forEach { it.cancel() }
        soundFadeJobs.clear()

        soundChannels.values.forEach { player ->
            player.stop()
            player.release()
        }
        soundChannels.clear()
        soundBaseVolumes.clear()
        soundRequestIds.clear()
    }

    override suspend fun playMusic(input: PlayMusicInput): Unit = withContext(Dispatchers.Main.immediate) {
        if (isDestroyed) return@withContext

        val identity = "${input.intro ?: ""}\n${input.key}"
        musicBaseVolume = input.volume

        if (activeMusicIdentity == identity && activeMusicPlayer != null) {
            setMusicVolume(input.volume, 0)
            return@withContext
        }

        val requestId = ++musicRequestId
        if (input.delayMs > 0) {
            delay(input.delayMs)
        }
        if (isDestroyed || requestId != musicRequestId) return@withContext

        val previousPlayer = activeMusicPlayer
        musicFadeJob?.cancel()

        val nextPlayer = createPlayer()
        activeMusicPlayer = nextPlayer
        activeMusicIdentity = identity

        val crossfade = previousPlayer != null && input.crossfadeMs > 10L
        nextPlayer.volume = if (crossfade) 0f else musicBaseVolume

        if (input.intro != null) {
            val introItem = MediaItem.fromUri(Uri.parse(input.intro))
            val loopItem = MediaItem.fromUri(Uri.parse(input.key))
            nextPlayer.setMediaItems(listOf(introItem, loopItem))
            nextPlayer.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    if (nextPlayer.currentMediaItemIndex == 1) {
                        nextPlayer.repeatMode = Player.REPEAT_MODE_ONE
                    }
                }
            })
        } else {
            val loopItem = MediaItem.fromUri(Uri.parse(input.key))
            nextPlayer.setMediaItem(loopItem)
            nextPlayer.repeatMode = Player.REPEAT_MODE_ONE
        }

        nextPlayer.prepare()
        nextPlayer.play()

        if (previousPlayer != null) {
            if (crossfade) {
                audioScope.launch {
                    fadeVolumeAndRelease(previousPlayer, input.crossfadeMs)
                }
            } else {
                previousPlayer.stop()
                previousPlayer.release()
            }
        }

        if (crossfade) {
            musicFadeJob = audioScope.launch {
                tweenVolume(nextPlayer, 0f, musicBaseVolume, input.crossfadeMs)
            }
        }
    }

    override suspend fun stopMusic(fadeMs: Long): Unit = withContext(Dispatchers.Main.immediate) {
        musicRequestId++
        val player = activeMusicPlayer ?: return@withContext
        activeMusicPlayer = null
        activeMusicIdentity = null
        musicFadeJob?.cancel()

        if (fadeMs > 0) {
            audioScope.launch {
                fadeVolumeAndRelease(player, fadeMs)
            }
        } else {
            player.stop()
            player.release()
        }
    }

    override suspend fun setMusicVolume(volume: Float, fadeMs: Long): Unit = withContext(Dispatchers.Main.immediate) {
        musicBaseVolume = volume
        val player = activeMusicPlayer ?: return@withContext
        musicFadeJob?.cancel()
        if (fadeMs <= 0) {
            player.volume = volume
        } else {
            val fromVol = player.volume
            musicFadeJob = audioScope.launch {
                tweenVolume(player, fromVol, volume, fadeMs)
            }
        }
    }

    override suspend fun playSound(input: PlaySoundInput): Unit = withContext(Dispatchers.Main.immediate) {
        if (isDestroyed) return@withContext

        val channel = input.channel
        val requestId = (soundRequestIds[channel] ?: 0) + 1
        soundRequestIds[channel] = requestId
        soundBaseVolumes[channel] = input.volume

        if (input.delayMs > 0) {
            delay(input.delayMs)
        }
        if (isDestroyed || soundRequestIds[channel] != requestId) return@withContext

        soundFadeJobs[channel]?.cancel()
        soundChannels.remove(channel)?.let { prev ->
            prev.stop()
            prev.release()
        }

        val soundPlayer = createPlayer()
        soundChannels[channel] = soundPlayer

        val soundItem = MediaItem.fromUri(Uri.parse(input.key))
        soundPlayer.setMediaItem(soundItem)
        soundPlayer.repeatMode = if (input.loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        soundPlayer.volume = soundBaseVolumes[channel] ?: input.volume

        if (!input.loop) {
            soundPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        if (soundChannels[channel] == soundPlayer) {
                            soundChannels.remove(channel)
                            soundPlayer.release()
                        }
                    }
                }
            })
        }

        soundPlayer.prepare()
        soundPlayer.play()
    }

    override suspend fun stopSound(channel: String, fadeMs: Long): Unit = withContext(Dispatchers.Main.immediate) {
        val requestId = (soundRequestIds[channel] ?: 0) + 1
        soundRequestIds[channel] = requestId
        soundBaseVolumes.remove(channel)
        soundFadeJobs[channel]?.cancel()

        val player = soundChannels.remove(channel) ?: return@withContext
        if (fadeMs > 0) {
            soundFadeJobs[channel] = audioScope.launch {
                fadeVolumeAndRelease(player, fadeMs)
            }
        } else {
            player.stop()
            player.release()
        }
    }

    override suspend fun setSoundVolume(channel: String, volume: Float, fadeMs: Long): Unit = withContext(Dispatchers.Main.immediate) {
        soundBaseVolumes[channel] = volume
        val player = soundChannels[channel] ?: return@withContext
        soundFadeJobs[channel]?.cancel()

        if (fadeMs <= 0) {
            player.volume = volume
        } else {
            val fromVol = player.volume
            soundFadeJobs[channel] = audioScope.launch {
                tweenVolume(player, fromVol, volume, fadeMs)
            }
        }
    }

    private suspend fun tweenVolume(
        player: ExoPlayer,
        fromVol: Float,
        toVol: Float,
        durationMs: Long
    ) {
        if (durationMs <= 0) {
            player.volume = toVol
            return
        }
        val startTime = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
            player.volume = fromVol + (toVol - fromVol) * progress
            if (progress >= 1f) break
            delay(16)
        }
        player.volume = toVol
    }

    private suspend fun fadeVolumeAndRelease(player: ExoPlayer, durationMs: Long) {
        val startVol = player.volume
        val startTime = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = (elapsed.toFloat() / max(1L, durationMs)).coerceIn(0f, 1f)
            player.volume = startVol * (1f - progress)
            if (progress >= 1f) break
            delay(16)
        }
        player.stop()
        player.release()
    }
}