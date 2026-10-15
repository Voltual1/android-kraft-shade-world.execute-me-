package me.voltual.asp.engine

data class CommandContext(
    val line: ParsedCommandLine,
    val runtime: StoryRuntime
)

typealias CommandHandler = suspend (ctx: CommandContext) -> Unit

object StoryCommandRegistry {

    private val registry = mutableMapOf<String, CommandHandler>()

    init {
        registerDefaultCommands()
    }

    fun register(name: String, handler: CommandHandler) {
        registry[name.lowercase()] = handler
    }

    suspend fun execute(line: ParsedCommandLine, runtime: StoryRuntime) {
        val handler = registry[line.command.lowercase()]
        if (handler != null) {
            handler(CommandContext(line, runtime))
        } else {
            runtime.onUnhandledCommand(line)
        }
    }

    private fun registerDefaultCommands() {
        register("dialog") { ctx ->
            val speaker = ctx.line.args["name"]?.toString() ?: ctx.line.args["speaker"]?.toString()
            val text = ctx.line.content
            ctx.runtime.showDialogue(speaker = speaker, text = text)
        }

        register("image") { ctx ->
            val imageKey = ctx.line.args["image"]?.toString() ?: ctx.line.args["file"]?.toString()
            val duration = (ctx.line.args["fadetime"] as? Number)?.toFloat() ?: (ctx.line.args["time"] as? Number)?.toFloat() ?: 0f
            val x = (ctx.line.args["x"] as? Number)?.toFloat() ?: 0f
            val y = (ctx.line.args["y"] as? Number)?.toFloat() ?: 0f
            val scale = (ctx.line.args["xScale"] as? Number)?.toFloat() ?: (ctx.line.args["scale"] as? Number)?.toFloat() ?: 1f

            if (imageKey != null) {
                ctx.runtime.setBackground(imageKey, duration = duration, x = x, y = y, scale = scale)
            }
        }

        register("background") { ctx ->
            val imageKey = ctx.line.args["image"]?.toString() ?: ctx.line.args["file"]?.toString()
            val duration = (ctx.line.args["fadetime"] as? Number)?.toFloat() ?: (ctx.line.args["time"] as? Number)?.toFloat() ?: 0f
            if (imageKey != null) {
                ctx.runtime.setBackground(imageKey, duration = duration)
            }
        }

        register("character") { ctx ->
            val name = ctx.line.args["name"]?.toString()
            val name2 = ctx.line.args["name2"]?.toString()
            val focus = (ctx.line.args["focus"] as? Number)?.toInt() ?: 1

            ctx.runtime.updateCharacters(
                slot1 = name,
                slot2 = name2,
                focusSlot = focus
            )
        }

        register("blocker") { ctx ->
            val a = (ctx.line.args["a"] as? Number)?.toFloat() ?: 0f
            val r = (ctx.line.args["r"] as? Number)?.toFloat() ?: 0f
            val g = (ctx.line.args["g"] as? Number)?.toFloat() ?: 0f
            val b = (ctx.line.args["b"] as? Number)?.toFloat() ?: 0f
            val time = (ctx.line.args["fadetime"] as? Number)?.toFloat() ?: (ctx.line.args["time"] as? Number)?.toFloat() ?: 0f
            val block = ctx.line.args["block"] == true
            val style = ctx.line.args["style"]?.toString() ?: "default"
            val inverse = ctx.line.args["inverse"] == true

            ctx.runtime.setBlocker(
                r = r, g = g, b = b, a = a,
                duration = time,
                isBlocking = block,
                style = style,
                inverse = inverse
            )
        }

        register("cameraeffect") { ctx ->
            val effect = ctx.line.args["effect"]?.toString() ?: ""
            val amount = (ctx.line.args["amount"] as? Number)?.toFloat() ?: 1f
            val duration = (ctx.line.args["fadetime"] as? Number)?.toFloat() ?: 0.4f
            val block = ctx.line.args["block"] == true
            val keep = ctx.line.args["keep"] == true
            val initAmount = (ctx.line.args["initamount"] as? Number)?.toFloat()

            ctx.runtime.setCameraEffect(
                effect = effect,
                amount = amount,
                duration = duration,
                isBlocking = block,
                keep = keep,
                initAmount = initAmount
            )
        }

        register("camerashake") { ctx ->
            val duration = (ctx.line.args["duration"] as? Number)?.toFloat() ?: (ctx.line.args["time"] as? Number)?.toFloat() ?: 0.5f
            val xstrength = (ctx.line.args["xstrength"] as? Number)?.toFloat() ?: 10f
            val ystrength = (ctx.line.args["ystrength"] as? Number)?.toFloat() ?: 0f
            val vibrato = (ctx.line.args["vibrato"] as? Number)?.toInt() ?: 10
            val randomness = (ctx.line.args["randomness"] as? Number)?.toFloat() ?: 90f
            val fadeout = ctx.line.args["fadeout"] == true
            val stop = ctx.line.args["stop"] == true

            ctx.runtime.triggerCameraShake(
                duration = duration,
                xStrength = xstrength,
                yStrength = ystrength,
                vibrato = vibrato,
                randomness = randomness,
                fadeOut = fadeout,
                stop = stop
            )
        }

        register("decision") { ctx ->
            val options = ctx.line.args["options"]?.toString()?.split(";") ?: emptyList()
            val values = ctx.line.args["values"]?.toString()?.split(";") ?: emptyList()

            if (options.isNotEmpty()) {
                ctx.runtime.showDecision(options = options, values = values)
            }
        }

        register("delay") { ctx ->
            val time = (ctx.line.args["time"] as? Number)?.toFloat() ?: 0f
            if (time > 0) {
                ctx.runtime.delay(time)
            }
        }

        register("playmusic") { ctx ->
            val key = ctx.line.args["intro"]?.toString() ?: ctx.line.args["key"]?.toString()
            val volume = (ctx.line.args["volume"] as? Number)?.toFloat() ?: 1f
            if (key != null) {
                ctx.runtime.playMusic(key, volume)
            }
        }

        register("stopmusic") { ctx ->
            val time = (ctx.line.args["fadetime"] as? Number)?.toFloat() ?: (ctx.line.args["time"] as? Number)?.toFloat() ?: 0f
            ctx.runtime.stopMusic(fadeTime = time)
        }

        register("playsound") { ctx ->
            val key = ctx.line.args["key"]?.toString()
            if (key != null) {
                ctx.runtime.playSound(key)
            }
        }

        register("predicate") { ctx ->
            val references = ctx.line.args["references"]?.toString()?.split(";") ?: emptyList()
            ctx.runtime.applyPredicate(references)
        }
    }
}