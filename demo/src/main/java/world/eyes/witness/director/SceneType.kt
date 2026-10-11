package world.eyes.witness.director

enum class SceneType(
    val title: String,
    val startSeconds: Float,
    val endSeconds: Float
) {
    /**
     * 00:00.000 ~ 00:16.000
     * 系统开机 / 终端初始化 / 数据参数注入 / 模拟启动
     */
    BOOT_SEQUENCE(
        title = "INITIALIZATION",
        startSeconds = 0.000f,
        endSeconds = 16.000f
    ),

    /**
     * 00:16.000 ~ 00:29.709
     * 世界引导间奏 / 3D 网格世界生成 / 钢琴独奏
     */
    WORLD_BOOT_INSTRUMENTAL(
        title = "WORLD BOOT",
        startSeconds = 16.000f,
        endSeconds = 29.709f
    ),

    /**
     * 00:29.709 ~ 00:44.452
     * 几何学 / 点集维度 / 圆周 / 正弦波切线 / 趋于无穷与极限
     */
    GEOMETRY_MATH(
        title = "GEOMETRY & LIMITATIONS",
        startSeconds = 29.709f,
        endSeconds = 44.452f
    ),

    /**
     * 00:44.452 ~ 00:59.223
     * 电流转换 (AC/DC) / 视觉眩晕模糊 / 时空跃迁 (AD/BC) / 深度联合
     */
    CURRENT_CONVERSION(
        title = "CURRENT & DIZZINESS",
        startSeconds = 44.452f,
        endSeconds = 59.223f
    ),

    /**
     * 00:59.223 ~ 01:14.045
     * 刺激 (STIMULATIONS) / 满足 (SATISFACTION) / 执行 (EXECUTION) / 困于牢笼
     */
    SIMULATION_EXECUTION(
        title = "STIMULATION & SIMULATION",
        startSeconds = 59.223f,
        endSeconds = 74.045f
    ),

    /**
     * 01:14.045 ~ 01:28.587
     * 茄子营养素 / 番茄抗氧化剂 / 虎斑猫呼噜 / 唯一神与存在之证明
     */
    BIOLOGY_EXISTENCE(
        title = "BIOLOGY & PROOF OF GOD",
        startSeconds = 74.045f,
        endSeconds = 88.587f
    ),

    /**
     * 01:28.587 ~ 01:43.489
     * 性别翻转 (F/M) / AM-PM 无尽运转 / 角色转换 (S/M) / 步入出神之境 (Trance)
     */
    GENDER_AND_ROLES(
        title = "ROLES & TRANCE",
        startSeconds = 88.587f,
        endSeconds = 103.489f
    ),

    /**
     * 01:43.489 ~ 02:14.380
     * 振动感应 / 补完 / "You have left me in ISOLATION" / 内存抹除 / 非法参数 (ILLEGAL ARGUMENTS)
     */
    ISOLATION_AND_ERROR(
        title = "ISOLATION & ILLEGAL ARGUMENTS",
        startSeconds = 103.489f,
        endSeconds = 134.380f
    ),

    /**
     * 02:14.380 ~ 02:27.660
     * 参数调用栈溢出间奏 / 高速数据倾泻与核心转储
     */
    ARGUMENT_STACK(
        title = "STACK OVERFLOW",
        startSeconds = 134.380f,
        endSeconds = 147.660f
    ),

    /**
     * 02:27.660 ~ 02:42.632
     * 连续 12 次暴击 EXECUTION / 多国语言计数 (德/西/法/韩/瑞/中)
     */
    RAPID_EXECUTION_STORM(
        title = "EXECUTION STORM",
        startSeconds = 147.660f,
        endSeconds = 162.632f
    ),

    /**
     * 02:42.632 ~ 02:57.246
     * 全域执行 / 唯你独尊 / 困兽之斗
     */
    DESPERATE_EXECUTION(
        title = "UNAVOIDABLE EXECUTION",
        startSeconds = 162.632f,
        endSeconds = 177.246f
    ),

    /**
     * 02:57.246 ~ 03:13.460
     * 研习爱 / 回答爱 / 爱的代数表达式 / 困于爱中 (Trapped in LO-O-OVE)
     */
    ALGEBRAIC_LOVE(
        title = "ALGEBRAIC EXPRESSION OF LOVE",
        startSeconds = 177.246f,
        endSeconds = 193.460f
    ),

    /**
     * 03:13.460 ~ 03:31.984
     * 死循环尾奏 / 终局执行 / 系统停机
     */
    OPEN_LOOP_SHUTDOWN(
        title = "SHUTDOWN / EXECUTE(ME)",
        startSeconds = 193.460f,
        endSeconds = 211.984f
    );

    val durationSeconds: Float = endSeconds - startSeconds
}