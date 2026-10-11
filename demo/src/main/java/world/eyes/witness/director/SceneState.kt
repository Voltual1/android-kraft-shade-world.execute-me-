package world.eyes.witness.director

data class SceneState(
    val scene: SceneType,
    val globalTimeSeconds: Float,
    val localTimeSeconds: Float,
    val progress: Float,
    val isGlitchActive: Boolean = false,
    val beatPulse: Float = 0f
) {
    companion object {
        val Initial = SceneState(
            scene = SceneType.BOOT_SEQUENCE,
            globalTimeSeconds = 0f,
            localTimeSeconds = 0f,
            progress = 0f,
            isGlitchActive = false,
            beatPulse = 0f
        )
    }
}