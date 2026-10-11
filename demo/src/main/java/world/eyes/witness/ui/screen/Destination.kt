package world.eyes.witness.ui.screen

import androidx.compose.runtime.Composable
import world.eyes.witness.ui.screen.animation.KraftShadeAnimatedViewTestWindow
import world.eyes.witness.ui.screen.basic.BasicShaderScreen
import world.eyes.witness.ui.screen.basic.BlendingExampleScreen
import world.eyes.witness.ui.screen.blur.CircularBlurPerformanceTestWindow
import world.eyes.witness.ui.screen.blur.CircularBlurTestWindow
import world.eyes.witness.ui.screen.color.ColorBlendShaderScreen
import world.eyes.witness.ui.screen.color.ColorBurnBlendShaderScreen
import world.eyes.witness.ui.screen.color.ColorDodgeBlendShaderScreen
import world.eyes.witness.ui.screen.color.DarkenBlendShaderScreen
import world.eyes.witness.ui.screen.color.DifferenceBlendShaderScreen
import world.eyes.witness.ui.screen.color.DissolveBlendShaderScreen
import world.eyes.witness.ui.screen.color.DivideBlendShaderScreen
import world.eyes.witness.ui.screen.color.ExclusionBlendShaderScreen
import world.eyes.witness.ui.screen.color.HueBlendShaderScreen
import world.eyes.witness.ui.screen.color.FalseColorShaderScreen
import world.eyes.witness.ui.screen.color.LevelsShaderScreen
import world.eyes.witness.ui.screen.color.LookUpTableShaderTestScreen
import world.eyes.witness.ui.screen.dsl.KraftBitmapTestScreen
import world.eyes.witness.ui.screen.effect.ColorfulCrosshatchTestScreen
import world.eyes.witness.ui.screen.effect.CrosshatchTestScreen
import world.eyes.witness.ui.screen.effect.EmbossShaderScreen
import world.eyes.witness.ui.screen.effect.KuwaharaTestWindow
import world.eyes.witness.ui.screen.effect.ToonEffectTestWindow
import world.eyes.witness.ui.screen.media3.ExoPlayerWithKraftShadePipeline
import world.eyes.witness.ui.screen.media3.ExoPlayerWithKraftShaders
import world.eyes.witness.ui.screen.media3.TransformerWithKraftShaderPipeline
import world.eyes.witness.ui.screen.morphology.ErosionDilationTestScreen
import world.eyes.witness.ui.screen.view.ResizeTestScreen
import world.eyes.witness.ui.screen.view.TransparencyTestWindow
import world.eyes.witness.ui.screen.view.compose.KraftShadeEffectViewTestWindow

enum class SampleType {
    Compose,
    TraditionalView,
    None
}

enum class Destination(
    val route: String,
    val title: String,
    val sampleType: SampleType,
    val category: Category = Category.OTHER,
    val screen: @Composable () -> Unit,
) {
    Home("home", "Home", sampleType = SampleType.None, category = Category.OTHER, screen = { HomeScreen() }),
    ComposableSamples("compose_samples", "Compose Samples", sampleType = SampleType.None, category = Category.OTHER, screen = { ComposableSampleScreen() }),
    TraditionalViewSamples("traditional_view_samples", "Traditional View Samples", sampleType = SampleType.None, category = Category.OTHER, screen = { TraditionalViewSampleScreen() }),
    ResizeTest("resize_test", "Resize Test", sampleType = SampleType.Compose, category = Category.OTHER, screen = { ResizeTestScreen() }),
    BasicShader("basic_shader", "Basic Shader", sampleType = SampleType.Compose, category = Category.BASIC, screen = { BasicShaderScreen() }),
    BlendingExample("blending_example", "Blending Example", sampleType = SampleType.Compose, category = Category.BASIC, screen = { BlendingExampleScreen() }),
    EmbossShader("emboss_shader", "Emboss Shader", sampleType = SampleType.Compose, category = Category.EFFECTS, screen = { EmbossShaderScreen() }),
    LookUpTableShader("look_up_table_shader", "Look Up Table Shader", sampleType = SampleType.Compose, category = Category.COLOR, screen = { LookUpTableShaderTestScreen() }),
    ColorBlendShader("color_blend_shader", "Color Blend Shader", sampleType = SampleType.Compose, category = Category.COLOR, screen = { ColorBlendShaderScreen() }),
    ColorBurnBlendShader("color_burn_blend_shader", "Color Burn Blend Shader", sampleType = SampleType.Compose, category = Category.COLOR, screen = { ColorBurnBlendShaderScreen() }),
    ColorDodgeBlendShader("color_dodge_blend_shader", "Color Dodge Blend Shader", sampleType = SampleType.Compose, category = Category.COLOR, screen = { ColorDodgeBlendShaderScreen() }),
    DarkenBlendShader("darken_blend_shader", "Darken Blend Shader", sampleType = SampleType.Compose, category = Category.COLOR, screen = { DarkenBlendShaderScreen() }),
    DifferenceBlendShader("difference_blend_shader", "Difference Blend Shader", sampleType = SampleType.Compose, category = Category.COLOR, screen = { DifferenceBlendShaderScreen() }),
    DissolveBlendShader("dissolve_blend_shader", "Dissolve Blend Shader", sampleType = SampleType.Compose, category = Category.COLOR, screen = { DissolveBlendShaderScreen() }),
    DivideBlendShader("divide_blend_shader", "Divide Blend Shader", sampleType = SampleType.Compose, category = Category.COLOR, screen = { DivideBlendShaderScreen() }),
    ExclusionBlendShader("exclusion_blend_shader", "Exclusion Blend Shader", sampleType = SampleType.Compose, category = Category.COLOR, screen = { ExclusionBlendShaderScreen() }),
    HueBlendShader("hue_blend_shader", "Hue Blend Shader", sampleType = SampleType.Compose, category = Category.COLOR, screen = { HueBlendShaderScreen() }),
    TransparencyTest("transparency_test", "Transparency Test", sampleType = SampleType.Compose, category = Category.OTHER, screen = { TransparencyTestWindow() }),
    KraftShadeAnimatedView("compose_animated", "Compose (animated)", sampleType = SampleType.Compose, category = Category.OTHER, screen = { KraftShadeAnimatedViewTestWindow() }),
    KraftShadeEffectView("compose_effect", "Compose (effect)", sampleType = SampleType.Compose, category = Category.OTHER, screen = { KraftShadeEffectViewTestWindow() }),
    CrosshatchShader("crosshatch_shader", "Crosshatch Shader", sampleType = SampleType.Compose, category = Category.EFFECTS, screen = { CrosshatchTestScreen() }),
    ColorfulCrosshatchShader("colorful_crosshatch_shader", "Colorful Crosshatch Shader", sampleType = SampleType.Compose, category = Category.EFFECTS, screen = { ColorfulCrosshatchTestScreen() }),
    KraftBitmap("kraft_bitmap", "Kraft Bitmap", sampleType = SampleType.Compose, category = Category.OTHER, screen = { KraftBitmapTestScreen() }),
    CircularBlurPerformance("circular_blur_performance", "Circular Blur Performance", sampleType = SampleType.Compose, category = Category.BLUR, screen = { CircularBlurPerformanceTestWindow() }),
    CircularBlur("circular_blur", "Circular Blur", sampleType = SampleType.Compose, category = Category.BLUR, screen = { CircularBlurTestWindow() }),
    ToonEffect("toon_effect", "Toon Effect", sampleType = SampleType.Compose, category = Category.EFFECTS, screen = { ToonEffectTestWindow() }),
    KuwaharaEffect("kuwahara_effect", "Kuwahara Effect", sampleType = SampleType.Compose, category = Category.EFFECTS, screen = { KuwaharaTestWindow() }),
    ErosionDilationShaderTest("erosion_dilation_shader_test", "Erosion/Dilation Shader Test", sampleType = SampleType.Compose, category = Category.BLUR, screen = { ErosionDilationTestScreen() }),
    LevelsShader("levels_shader", "Levels Shader", sampleType = SampleType.Compose, category = Category.COLOR, screen = { LevelsShaderScreen() }),
    FalseColorShader("false_color_shader", "False Color Shader", sampleType = SampleType.Compose, category = Category.COLOR, screen = { FalseColorShaderScreen() }),
    ExoPlayerWithKraftShaders("exo_player_with_kraft_shaders", "ExoPlayer with Kraft Shaders", sampleType = SampleType.Compose, category = Category.MEDIA3, screen = { ExoPlayerWithKraftShaders() }),
    ExoPlayerWithKraftShadePipeline("exo_player_with_kraftshade_pipeline", "ExoPlayer with KraftShade Pipeline", sampleType = SampleType.Compose, category = Category.MEDIA3, screen = { ExoPlayerWithKraftShadePipeline() }),
    TransformerWithKraftShadePipeline("transformer_with_kraftshade_pipeline", "Transformer with KraftShade Pipeline", sampleType = SampleType.Compose, category = Category.MEDIA3, screen = { TransformerWithKraftShaderPipeline() }),
    // Traditional View Samples
    BasicShaderTraditional("basic_shader_traditional", "Basic Shader (Traditional)", sampleType = SampleType.TraditionalView, category = Category.BASIC, screen = { /* Activity will handle this */ }),
    AnimatedShaderTraditional("animated_shader_traditional", "Animated Shader (Traditional)", sampleType = SampleType.TraditionalView, category = Category.BASIC, screen = { /* Activity will handle this */ }),
    VideoShaderTraditional("video_shader_traditional", "Video Shader (Traditional)", sampleType = SampleType.TraditionalView, category = Category.BASIC, screen = { /* Activity will handle this */ }),
}