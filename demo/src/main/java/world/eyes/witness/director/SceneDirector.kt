package world.eyes.witness.director

object SceneDirector {
    private val scenes = SceneType.values().sortedBy { it.startSeconds }

    fun resolveState(globalTimeSeconds: Float): SceneState {
        val clampedTime = globalTimeSeconds.coerceIn(0f, 211.984f)

        val activeScene = scenes.firstOrNull { scene ->
            clampedTime >= scene.startSeconds && clampedTime < scene.endSeconds
        } ?: scenes.last()

        val localTime = (clampedTime - activeScene.startSeconds).coerceAtLeast(0f)
        val progress = if (activeScene.durationSeconds > 0f) {
            (localTime / activeScene.durationSeconds).coerceIn(0f, 1f)
        } else {
            1f
        }

        val glitchActive = checkGlitch(activeScene, localTime, clampedTime)
        val pulse = calculateBeatPulse(clampedTime)

        return SceneState(
            scene = activeScene,
            globalTimeSeconds = clampedTime,
            localTimeSeconds = localTime,
            progress = progress,
            isGlitchActive = glitchActive,
            beatPulse = pulse
        )
    }

    private fun checkGlitch(scene: SceneType, localTime: Float, globalTime: Float): Boolean {
        return when (scene) {
            SceneType.ISOLATION_AND_ERROR -> {
                // 在 ILLEGAL ARGUMENTS 唱响时触发强烈故障抽搐
                globalTime in 125.7f..134.4f || (localTime % 1.0f < 0.15f && globalTime > 110f)
            }
            SceneType.ARGUMENT_STACK -> {
                // 崩溃转储阶段的周期性高频闪烁
                (localTime * 8f).toInt() % 3 == 0
            }
            SceneType.RAPID_EXECUTION_STORM -> {
                // 连续卡点强闪
                (localTime * 4f).toInt() % 2 == 0
            }
            SceneType.CURRENT_CONVERSION -> {
                // AC/DC 转换脉冲
                localTime in 0.0f..1.5f || localTime in 3.2f..4.5f
            }
            else -> false
        }
    }

    /**
     * 歌曲基准速度为 130 BPM，一拍约等于 60 / 130 ≈ 0.4615 秒
     */
    private fun calculateBeatPulse(globalTime: Float): Float {
        if (globalTime < 16.0f) return 0f // 前奏阶段不发拍点光
        val beatPeriod = 60f / 130f
        val phase = (globalTime % beatPeriod) / beatPeriod
        // 指数衰减脉冲波形 (1.0 -> 0.0)
        return kotlin.math.exp(-phase * 4.5f)
    }
}