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
            // 未知的指令类型作为默认文本或空指令处理
            runtime.onUnhandledCommand(line)
        }
    }

    private fun registerDefaultCommands() {
        // [Dialog] 指令或对话设置
        register("dialog") { ctx ->
            val speaker = ctx.line.args["speaker"]?.toString()
            val text = ctx.line.content
            ctx.runtime.showDialogue(speaker = speaker, text = text)
        }

        // [Image] 或 [Background] 改变背景/全屏CG
        register("image") { ctx ->
            val imageKey = ctx.line.args["image"]?.toString() ?: ctx.line.args["file"]?.toString()
            val duration = (ctx.line.args["time"] as? Number)?.toFloat() ?: 0f
            val x = (ctx.line.args["x"] as? Number)?.toFloat() ?: 0f
            val y = (ctx.line.args["y"] as? Number)?.toFloat() ?: 0f
            val scale = (ctx.line.args["xScale"] as? Number)?.toFloat() ?: (ctx.line.args["scale"] as? Number)?.toFloat() ?: 1f
            
            if (imageKey != null) {
                ctx.runtime.setBackground(imageKey, duration = duration, x = x, y = y, scale = scale)
            }
        }
        register("background") { ctx ->
            val imageKey = ctx.line.args["image"]?.toString() ?: ctx.line.args["file"]?.toString()
            val duration = (ctx.line.args["time"] as? Number)?.toFloat() ?: 0f
            if (imageKey != null) {
                ctx.runtime.setBackground(imageKey, duration = duration)
            }
        }

        // [Character] 改变角色立绘与位置
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

        // [Blocker] 画面遮罩/黑屏闪烁/过渡效果
        register("blocker") { ctx ->
            val a = (ctx.line.args["a"] as? Number)?.toFloat() ?: 0f
            val r = (ctx.line.args["r"] as? Number)?.toFloat() ?: 0f
            val g = (ctx.line.args["g"] as? Number)?.toFloat() ?: 0f
            val b = (ctx.line.args["b"] as? Number)?.toFloat() ?: 0f
            val time = (ctx.line.args["time"] as? Number)?.toFloat() ?: 0f
            val block = ctx.line.args["block"] == true

            ctx.runtime.setBlocker(r = r, g = g, b = b, a = a, duration = time, isBlocking = block)
        }

        // [Decision] 分支选项
        register("decision") { ctx ->
            val options = ctx.line.args["options"]?.toString()?.split(";") ?: emptyList()
            val values = ctx.line.args["values"]?.toString()?.split(";") ?: emptyList()
            
            if (options.isNotEmpty()) {
                ctx.runtime.showDecision(options = options, values = values)
            }
        }

        // [Delay] 延迟指令
        register("delay") { ctx ->
            val time = (ctx.line.args["time"] as? Number)?.toFloat() ?: 0f
            if (time > 0) {
                ctx.runtime.delay(time)
            }
        }

        // [PlayMusic] / [StopMusic] 背景音乐控制
        register("playmusic") { ctx ->
            val key = ctx.line.args["intro"]?.toString() ?: ctx.line.args["key"]?.toString()
            val volume = (ctx.line.args["volume"] as? Number)?.toFloat() ?: 1f
            if (key != null) {
                ctx.runtime.playMusic(key, volume)
            }
        }
        register("stopmusic") { ctx ->
            val time = (ctx.line.args["time"] as? Number)?.toFloat() ?: 0f
            ctx.runtime.stopMusic(fadeTime = time)
        }

        // [PlaySound] 播放单次音效
        register("playsound") { ctx ->
            val key = ctx.line.args["key"]?.toString()
            if (key != null) {
                ctx.runtime.playSound(key)
            }
        }

        // [CameraShake] 镜头震动效果
        register("camerashake") { ctx ->
            val duration = (ctx.line.args["time"] as? Number)?.toFloat() ?: 0.5f
            val xstrength = (ctx.line.args["xstrength"] as? Number)?.toFloat() ?: 10f
            val ystrength = (ctx.line.args["ystrength"] as? Number)?.toFloat() ?: 10f
            ctx.runtime.triggerCameraShake(duration, xstrength, ystrength)
        }

        // [Predicate] 条件分支逻辑标记
        register("predicate") { ctx ->
            val references = ctx.line.args["references"]?.toString()?.split(";") ?: emptyList()
            ctx.runtime.applyPredicate(references)
        }
    }
}