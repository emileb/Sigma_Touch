package com.opentouchgaming.sigmatouch.engineoptions

import android.app.Activity
import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import androidx.arch.core.util.Function
import com.opentouchgaming.androidcore.AppInfo
import com.opentouchgaming.androidcore.DebugLog
import com.opentouchgaming.androidcore.EngineOptionsInterface
import com.opentouchgaming.androidcore.EngineOptionsInterface.MultiplayerCallback
import com.opentouchgaming.androidcore.EngineOptionsInterface.RunInfo
import com.opentouchgaming.androidcore.GameEngine
import com.opentouchgaming.androidcore.ui.widgets.DeleteDataWidget
import com.opentouchgaming.androidcore.ui.widgets.GamepadConfigWidget
import com.opentouchgaming.androidcore.ui.widgets.SpinnerWidget
import com.opentouchgaming.androidcore.ui.widgets.SwitchWidget
import com.opentouchgaming.androidcore.databinding.WidgetViewSpinnerBinding
import com.opentouchgaming.saffal.FileSAF
import com.opentouchgaming.sigmatouch.R
import com.opentouchgaming.sigmatouch.databinding.DialogOptionsQuake4Binding

// Minimal Phase 1 options, mirrors EngineOptionsAVP. Real settings come in Phase 2.
class EngineOptionsQuake4 : EngineOptionsInterface
{
    var log = DebugLog(DebugLog.Module.GAMEFRAGMENT, "EngineOptionsQuake4")

    lateinit var binding: DialogOptionsQuake4Binding

    lateinit var dialog: Dialog

    val PREFIX = "quake4"

    val GAMEPAD_CONFIG_KEY = "quake4_gamepad_config"

    override fun showDialog(activity: Activity, engine: GameEngine, version: Int, update: Function<Int, Void>)
    {
        binding = DialogOptionsQuake4Binding.inflate(activity.layoutInflater)

        dialog = Dialog(activity, R.style.DialogEngineSettings)
        dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.setTitle("Quake 4 options")
        dialog.setContentView(binding.root)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)

        val graphicsSwitch = SwitchWidget(
            activity, binding.graphicsSettingsSwitch.root, "Use these graphics settings",
            "Enable/Disable seting gfx settings in command line",
            GRAPHICS_SETTINGS_PREFIX, GRAPHICS_SETTINGS_DEFAULT, R.drawable.setting_gpu
        )

        // Set before the dialog is shown, so the initial state does not animate
        binding.graphicsSettingsGroup.visibility =
            if (SwitchWidget.fetchValue(activity, GRAPHICS_SETTINGS_PREFIX, GRAPHICS_SETTINGS_DEFAULT)) View.VISIBLE else View.GONE

        graphicsSwitch.callback = { enabled ->
            binding.graphicsSettingsGroup.visibility = if (enabled) View.VISIBLE else View.GONE
        }

        val picmipItems = PICMIP_LABELS.map { Pair<String, View?>(it, null) }.toTypedArray()
        SpinnerWidget(
            activity, binding.picmipSpinner.root, "Texture detail",
            "Each step halves colour, normal and specular texture size. Much less memory, faster loads.",
            picmipItems, PICMIP_PREFIX, PICMIP_DEFAULT, R.drawable.setting_gpu
        )

        val compressionItems = TEXTURE_COMPRESSION_LABELS.map { Pair<String, View?>(it, null) }.toTypedArray()
        SpinnerWidget(
            activity, binding.textureCompressionSpinner.root, "Texture compression",
            "No effect on devices that can use DDS, so it is safe to leave on.",
            compressionItems, TEXTURE_COMPRESSION_PREFIX, TEXTURE_COMPRESSION_DEFAULT, R.drawable.setting_gpu
        )

        createImmediateGraphicsWidgets(
            activity, binding.screenFractionSpinner.root, binding.resolutionScaleFilterSpinner.root,
            binding.shadowsSwitch.root, binding.specularSwitch.root, binding.bumpSwitch.root,
            binding.ambientSwitch.root, binding.shaderAmbientSwitch.root, binding.fpsCapSpinner.root,
            binding.vsyncSwitch.root, binding.showFpsSwitch.root
        )

        SwitchWidget(
            activity, binding.presentSceneTargetSwitch.root, "Blit scene to screen",
            "GLES renderer bring-up scaffolding. Turning it off will most likely give a black screen.",
            PRESENT_SCENE_TARGET_PREFIX, PRESENT_SCENE_TARGET_DEFAULT, R.drawable.setting_gpu
        )

        SwitchWidget(
            activity, binding.releaseSamplePayloadSwitch.root, "Free duplicate sound memory",
            "The audio system keeps its own copy of every sound, so the engine drops its one. Saves around 170MB.",
            RELEASE_SAMPLE_PAYLOAD_PREFIX, RELEASE_SAMPLE_PAYLOAD_DEFAULT, R.drawable.setting_audio
        )

        val smoothItems = MOUSE_SMOOTH_LABELS.map { Pair<String, View?>(it, null) }.toTypedArray()
        SpinnerWidget(
            activity, binding.mouseSmoothSpinner.root, "Look smoothing",
            "Blend this many look samples together. Softens rough touch input at the cost of a little response.",
            smoothItems, MOUSE_SMOOTH_PREFIX, MOUSE_SMOOTH_DEFAULT, R.drawable.setting_gear
        )

        SwitchWidget(
            activity, binding.toggleCrouchSwitch.root, "Toggle crouch",
            "The crouch button switches between crouching and standing instead of having to be held down.",
            TOGGLE_CROUCH_PREFIX, TOGGLE_CROUCH_DEFAULT, R.drawable.setting_gear
        )

        SwitchWidget(
            activity, binding.toggleZoomSwitch.root, "Toggle zoom",
            "The zoom button switches zoom on and off instead of having to be held down.",
            TOGGLE_ZOOM_PREFIX, TOGGLE_ZOOM_DEFAULT, R.drawable.setting_gear
        )

        SwitchWidget(
            activity, binding.validatePaksSwitch.root, "Check game files",
            "Verify the retail q4base pk4 checksums and openQ4's own pak0/pak1 at startup.",
            VALIDATE_PAKS_PREFIX, VALIDATE_PAKS_DEFAULT, R.drawable.setting_gear
        )

        GamepadConfigWidget(activity, binding.gamepadConfigSpinner.root, GAMEPAD_CONFIG_KEY)

        DeleteDataWidget(
            activity, binding.deleteDataButton.root,
            "Delete all Quake 4 settings files?", arrayOf("/$USER_DIR_NAME/"), arrayOf(CONFIG_FILENAME),
            "", arrayOf(""), arrayOf("")
        )

        dialog.show()
    }

    // The engine's Sys_DefaultSavePath points fs_savepath at user_files/quake4,
    // so configs, savegames and the console log all land there.
    companion object
    {
        const val USER_DIR_NAME = "quake4"

        // openQ4's CONFIG_FILE, written into fs_savepath/<gamedir>
        const val CONFIG_FILENAME = "openQ4Config.cfg"

        // Master switch for the whole graphics block. Off, none of those cvars
        // are put on the command line, which is the only way to let
        // Quake4Config.cfg and the in-game menus keep their own values - every
        // one of them is CVAR_ARCHIVE, so a startup +set always wins.
        const val GRAPHICS_SETTINGS_PREFIX = "quake4_graphics_settings"
        const val GRAPHICS_SETTINGS_DEFAULT = true

        // openQ4's picmip cvar is image_picmip, not the Quake 3 spelling it
        // mirrors. Spinner position is the mip shift, so index == cvar value.
        const val PICMIP_PREFIX = "quake4_picmip"
        const val PICMIP_DEFAULT = 0

        val PICMIP_LABELS = arrayOf("Full (0)", "Half (1)", "Quarter (2)", "Eighth (3)")

        // image_picmip on its own reduces far less than the label suggests. In
        // openQ4 it applies only where usage == TD_DIFFUSE, and only inside the
        // namespaces image_picmipFilter allows -- which defaults to textures/*
        // alone. Sending just image_picmip therefore leaves every models/*
        // texture, every normal map and every specular map at authored size,
        // and normal maps are the single largest bucket in Quake 4's set.
        //
        // So each spinner position carries the whole set of cvars: the shift,
        // the filter that widens it to every namespace, and absolute ceilings
        // for the two usages picmip cannot reach.

        // image_picmipFilter is a category mask, and 0 is its "every diffuse
        // texture" value (PICMIP_FILTER_ALL in the engine) rather than "off".
        const val PICMIP_FILTER_ALL = 0

        // Engine default. Stops a small texture being reduced to mush.
        const val PICMIP_MIN_SIZE = 32

        // Ceilings for the usages image_picmip skips, indexed by spinner
        // position; 0 means "leave this usage alone". They are absolute sizes
        // rather than shifts because that is what image_downSize*Limit takes.
        //
        // These deliberately do not enable image_downSize, the catch-all limit.
        // R_ApplyImageDownsizePolicy applies maxDimension first and then the
        // picmip shift on top of the result, so a catch-all ceiling would
        // reduce diffuse twice and level 3 would land on 32px world textures.
        val PICMIP_BUMP_LIMITS = arrayOf(0, 512, 256, 128)
        val PICMIP_SPECULAR_LIMITS = arrayOf(0, 256, 128, 64)

        // image_useETC2. Spinner position is the cvar value, and the levels rise
        // by how much of the frame they can affect: specular is the least
        // visually sensitive, colour carries the alpha channel, and normals are
        // the only one whose format the interaction shaders have to decode
        // differently.
        //
        // The engine gates the whole thing on the driver reporting no S3TC, so
        // on hardware that reads Quake 4's shipped DXT directly this is inert at
        // every position -- there the shipped blocks are already smaller and
        // better than anything re-encoded from them. It is the drivers without
        // it, the Adreno 650 class, that were carrying every texture at 32bpp.
        //
        // Defaults to full because that is where the measurements were taken and
        // because the devices it does anything on are exactly the ones that need
        // it: 1511MB of textures down to 509MB on a loaded map, with no visible
        // difference at a fixed viewpoint.
        const val TEXTURE_COMPRESSION_PREFIX = "quake4_texture_compression"
        const val TEXTURE_COMPRESSION_DEFAULT = 3

        val TEXTURE_COMPRESSION_LABELS = arrayOf(
            "Off", "Specular only", "Specular and colour", "Full (recommended)"
        )

        // r_screenFraction accepts 10..200; these are the useful steps.
        const val SCREEN_FRACTION_PREFIX = "quake4_screen_fraction"
        const val SCREEN_FRACTION_DEFAULT = 5 // index of 100%

        val SCREEN_FRACTIONS = arrayOf(25, 33, 50, 75, 80, 100)

        // r_resolutionScaleMode picks how the cropped 3D image is blitted back
        // out to full size. Only the two that mean something on the GLES
        // renderer are offered: 0 is the legacy crop with no upscale at all,
        // which puts the scene in a corner, and 2's sharpening pass lives in
        // draw_common.cpp, which the GLES module does not build.
        // Defaults to Sharp: bilinear at these fractions reads as mud on a phone
        // panel, and point sampling at a whole-number step is just pixel
        // doubling, which stays legible.
        const val RESOLUTION_SCALE_FILTER_PREFIX = "quake4_resolution_scale_filter"
        const val RESOLUTION_SCALE_FILTER_DEFAULT = 1 // index of Sharp

        val RESOLUTION_SCALE_FILTER_LABELS = arrayOf("Smooth (bilinear)", "Sharp (nearest)")

        val RESOLUTION_SCALE_FILTER_MODES = arrayOf(1, 3)

        const val SHADOWS_PREFIX = "quake4_shadows"
        const val SHADOWS_DEFAULT = true

        // These three read as features, not as the r_skip* cvars behind them,
        // so they switch the same way round as Shadows: on = drawn. The setting
        // keys are named for the feature too, so an older stored "skip" value
        // cannot be picked up with its meaning inverted.
        const val SPECULAR_PREFIX = "quake4_specular"
        const val SPECULAR_DEFAULT = true

        const val BUMP_PREFIX = "quake4_bump"
        const val BUMP_DEFAULT = true

        const val AMBIENT_PREFIX = "quake4_ambient"
        const val AMBIENT_DEFAULT = true

        // r_skipNewAmbient, "bypasses all vertex/fragment program ambient
        // drawing". Same on = drawn polarity as the three above. The engine
        // exempts SS_POST_PROCESS materials, so fullscreen post still runs.
        const val SHADER_AMBIENT_PREFIX = "quake4_shader_ambient"
        const val SHADER_AMBIENT_DEFAULT = true

        const val PRESENT_SCENE_TARGET_PREFIX = "quake4_present_scene_target"
        const val PRESENT_SCENE_TARGET_DEFAULT = true

        // s_releaseSamplePayload. alBufferData copies a sample's PCM into
        // OpenAL's own storage, so the engine's copy is a second resident copy
        // of every byte; releasing it saved ~170MB on a loaded map. Defaults on
        // to match the engine. The switch exists because the restore path -- for
        // a lead-in paired with a different looping sample -- is the one case
        // that can behave differently, and a missing sound needs an off switch
        // that does not require a rebuild.
        const val RELEASE_SAMPLE_PAYLOAD_PREFIX = "quake4_release_sample_payload"
        const val RELEASE_SAMPLE_PAYLOAD_DEFAULT = true

        // com_maxfps. The engine ships 240, but USERCMD_HZ is 60 and nothing
        // interpolates the view between tics, so the default here is 60 - one
        // fresh tic per presented frame.
        const val FPS_CAP_PREFIX = "quake4_fps_cap"
        const val FPS_CAP_DEFAULT = 1 // index of 60

        val FPS_CAPS = arrayOf(30, 60, 72, 90, 120, 0)
        val FPS_CAP_LABELS = arrayOf("30", "60", "72", "90", "120", "Uncapped")

        // r_swapInterval, engine default 0.
        const val VSYNC_PREFIX = "quake4_vsync"
        const val VSYNC_DEFAULT = false

        // m_smooth, 1..8 samples blended; 1 is the engine default and means off.
        const val MOUSE_SMOOTH_PREFIX = "quake4_mouse_smooth"
        const val MOUSE_SMOOTH_DEFAULT = 0 // index of 1

        val MOUSE_SMOOTH_LABELS = arrayOf("Off", "2", "3", "4", "5", "6", "7", "8")

        const val SHOW_FPS_PREFIX = "quake4_show_fps"
        const val SHOW_FPS_DEFAULT = false

        // in_toggleCrouch / in_toggleZoom. Both default off in the engine, and
        // both are worth having on a touchscreen where holding a button down
        // costs a thumb. in_toggleRun is deliberately not exposed - the engine
        // only honours it in multiplayer.
        const val TOGGLE_CROUCH_PREFIX = "quake4_toggle_crouch"
        const val TOGGLE_CROUCH_DEFAULT = false

        const val TOGGLE_ZOOM_PREFIX = "quake4_toggle_zoom"
        const val TOGGLE_ZOOM_DEFAULT = false

        // fs_validateOfficialPaks covers both pak sets: the retail q4base
        // checksums and the md5s of openQ4's own pak0/pak1. Those md5s are baked
        // into the engine at build time, so a stale pair left in user_files by an
        // older install is fatal until this is turned off.
        const val VALIDATE_PAKS_PREFIX = "quake4_validate_paks"
        const val VALIDATE_PAKS_DEFAULT = false

        val userDir: FileSAF
            get() = FileSAF(AppInfo.getUserFiles(), USER_DIR_NAME)

        private fun bit(context: android.content.Context, prefix: String, default: Boolean) =
            if (SwitchWidget.fetchValue(context, prefix, default)) 1 else 0

        private fun invBit(context: android.content.Context, prefix: String, default: Boolean) =
            1 - bit(context, prefix, default)

        // The resolution scale pair lives outside the master switch: it is the app's
        // own control, always sent. Both cvars are read per frame, so also live in-game.
        fun resolutionScaleCvars(ctx: android.content.Context): List<Pair<String, String>>
        {
            val fractionIndex = SpinnerWidget.fetchValue(ctx, SCREEN_FRACTION_PREFIX, SCREEN_FRACTION_DEFAULT)
                .coerceIn(0, SCREEN_FRACTIONS.size - 1)

            val filterIndex = SpinnerWidget.fetchValue(ctx, RESOLUTION_SCALE_FILTER_PREFIX, RESOLUTION_SCALE_FILTER_DEFAULT)
                .coerceIn(0, RESOLUTION_SCALE_FILTER_MODES.size - 1)

            return listOf(
                "r_screenFraction" to "${SCREEN_FRACTIONS[fractionIndex]}",
                "r_resolutionScaleMode" to "${RESOLUTION_SCALE_FILTER_MODES[filterIndex]}"
            )
        }

        // The graphics cvars that take effect immediately on a running game.
        // Shared by the launch args and the in-game dialog so the two cannot drift.
        fun immediateGraphicsCvars(ctx: android.content.Context): List<Pair<String, String>>
        {
            val cvars = mutableListOf<Pair<String, String>>()

            cvars.add("r_shadows" to "${bit(ctx, SHADOWS_PREFIX, SHADOWS_DEFAULT)}")

            // The switches are "is this drawn", the cvars are "skip it".
            cvars.add("r_skipSpecular" to "${invBit(ctx, SPECULAR_PREFIX, SPECULAR_DEFAULT)}")
            cvars.add("r_skipBump" to "${invBit(ctx, BUMP_PREFIX, BUMP_DEFAULT)}")
            cvars.add("r_skipAmbient" to "${invBit(ctx, AMBIENT_PREFIX, AMBIENT_DEFAULT)}")

            // Two cvars, one switch: r_skipNewAmbient for parity, r_glesD3SkipMaterialPrograms
            // is the one that reaches heat haze on the GLES backend.
            cvars.add("r_skipNewAmbient" to "${invBit(ctx, SHADER_AMBIENT_PREFIX, SHADER_AMBIENT_DEFAULT)}")
            cvars.add("r_glesD3SkipMaterialPrograms" to "${invBit(ctx, SHADER_AMBIENT_PREFIX, SHADER_AMBIENT_DEFAULT)}")

            val fpsIndex = SpinnerWidget.fetchValue(ctx, FPS_CAP_PREFIX, FPS_CAP_DEFAULT)
                .coerceIn(0, FPS_CAPS.size - 1)
            cvars.add("com_maxfps" to "${FPS_CAPS[fpsIndex]}")

            cvars.add("r_swapInterval" to "${bit(ctx, VSYNC_PREFIX, VSYNC_DEFAULT)}")
            cvars.add("com_showFPS" to "${bit(ctx, SHOW_FPS_PREFIX, SHOW_FPS_DEFAULT)}")

            return cvars
        }

        // The widget rows behind those cvars, shared by both dialogs.
        fun createImmediateGraphicsWidgets(activity: Activity, screenFraction: View, resolutionFilter: View,
                                           shadows: View, specular: View, bump: View, ambient: View,
                                           shaderAmbient: View, fpsCap: View, vsync: View, showFps: View)
        {
            val fractionItems = SCREEN_FRACTIONS.map { Pair<String, View?>("$it%", null) }.toTypedArray()
            val fractionSpinner = SpinnerWidget(
                activity, screenFraction, "3D resolution scale",
                "Renders the world smaller and upscales it. The HUD and menus stay sharp. Biggest single speed-up.",
                fractionItems, SCREEN_FRACTION_PREFIX, SCREEN_FRACTION_DEFAULT, R.drawable.setting_resolution
            )

            val filterItems = RESOLUTION_SCALE_FILTER_LABELS.map { Pair<String, View?>(it, null) }.toTypedArray()
            SpinnerWidget(
                activity, resolutionFilter, "Upscale filter",
                "How the smaller 3D image is stretched back out. Smooth blurs it; Sharp keeps the pixels crisp.",
                filterItems, RESOLUTION_SCALE_FILTER_PREFIX, RESOLUTION_SCALE_FILTER_DEFAULT, R.drawable.setting_resolution
            )

            // Nothing is upscaled at 100%, so the filter has nothing to do. Grey the
            // row out rather than hiding it, so it stays visible as the thing that
            // becomes available once the scale is turned down.
            val filterBinding = WidgetViewSpinnerBinding.bind(resolutionFilter)
            fun applyFilterEnabled(fractionIndex: Int)
            {
                val enabled = SCREEN_FRACTIONS[fractionIndex.coerceIn(0, SCREEN_FRACTIONS.size - 1)] < 100
                filterBinding.spinner.isEnabled = enabled
                filterBinding.root.alpha = if (enabled) 1.0f else 0.4f
            }

            // Set before the dialog is shown, so the initial state does not animate
            applyFilterEnabled(SpinnerWidget.fetchValue(activity, SCREEN_FRACTION_PREFIX, SCREEN_FRACTION_DEFAULT))
            fractionSpinner.callback = { index -> applyFilterEnabled(index) }

            SwitchWidget(
                activity, shadows, "Shadows",
                "Stencil shadow volumes. Turning them off is a big speed-up.",
                SHADOWS_PREFIX, SHADOWS_DEFAULT, R.drawable.setting_lightbulb
            )

            SwitchWidget(
                activity, specular, "Specular",
                "Specular highlights on lit surfaces. Turning them off is cheaper.",
                SPECULAR_PREFIX, SPECULAR_DEFAULT, R.drawable.setting_lightbulb
            )

            SwitchWidget(
                activity, bump, "Bump maps",
                "Sample normal maps when lighting. Off lights every surface flat.",
                BUMP_PREFIX, BUMP_DEFAULT, R.drawable.setting_lightbulb
            )

            SwitchWidget(
                activity, ambient, "Ambient pass",
                "Draw non-interaction surfaces.",
                AMBIENT_PREFIX, AMBIENT_DEFAULT, R.drawable.setting_lightbulb
            )

            SwitchWidget(
                activity, shaderAmbient, "Shader effects",
                "Heat haze on glass and explosions, and similar shader-program stages. Off also skips the full-screen copy they need, so it saves more than the effects themselves cost.",
                SHADER_AMBIENT_PREFIX, SHADER_AMBIENT_DEFAULT, R.drawable.setting_lightbulb
            )

            val fpsCapItems = FPS_CAP_LABELS.map { Pair<String, View?>(it, null) }.toTypedArray()
            SpinnerWidget(
                activity, fpsCap, "Frame rate cap",
                "The game only simulates at 60Hz and never interpolates, so anything above 60 repeats views unevenly and looks worse than it measures.",
                fpsCapItems, FPS_CAP_PREFIX, FPS_CAP_DEFAULT, R.drawable.setting_gear
            )

            SwitchWidget(
                activity, vsync, "VSync",
                "Pace presentation to the display.",
                VSYNC_PREFIX, VSYNC_DEFAULT, R.drawable.setting_gpu
            )

            SwitchWidget(
                activity, showFps, "Show FPS",
                "Draw the frame rate counter in-game.",
                SHOW_FPS_PREFIX, SHOW_FPS_DEFAULT, R.drawable.setting_gear
            )
        }
    }

    override fun getRunInfo(version: Int): RunInfo
    {
        val info = RunInfo()

        // The renderer is the GLES_D3 back end in librenderer-gles_arm64.so,
        // which asks SDL for an ES 3.0 context.
        info.glesVersion = 3
        info.useGL4ES = false

        // openQ4 is an SDL3 engine, so it needs the app3000 SDL activity.
        info.sdlVersion = 3

        // In-game graphics dialog, opened by the touch overlay's settings button.
        info.inGameOptionsClass = Quake4InGameOptions::class.java.name

        // No framebuffer scaler: leaving frameBufferWidth/Height null keeps the
        // engine on the full surface. r_screenFraction scales the 3D view
        // instead, which leaves the HUD and menus sharp.

        // Must be non-null: SigmaFragment concatenates runInfo.args into the
        // command line, and a null here becomes the literal string "null".
        info.args = " "

        // Everything below goes in as +set. Several of these are CVAR_ARCHIVE,
        // so a stale Quake4Config.cfg would otherwise win; a startup variable
        // takes precedence over the config exec.
        val ctx = AppInfo.getContext()

        // The generated/ tree -- binary image and decoded sound caches -- is all
        // rebuildable from the pk4s, and it is the largest thing the engine
        // writes: hundreds of MB per map on a device with no S3TC, where every
        // texture has to go through the CPU compressor. Sending it to the app's
        // cache directory keeps it out of user_files, so Android can reclaim the
        // space under pressure and it never lands in a backup. Unset, the engine
        // would put it in fs_savepath next to the config and saves.
        info.args += " +set fs_cachepath \"${AppInfo.cacheFiles}\" "

        // Every graphics cvar in one block, so the master switch can leave the
        // whole lot off the command line and hand the engine's own config back
        // its say. Anything outside it is not a graphics setting.
        if (SwitchWidget.fetchValue(ctx, GRAPHICS_SETTINGS_PREFIX, GRAPHICS_SETTINGS_DEFAULT))
        {
            // Texture detail. Every cvar below is CVAR_ARCHIVE, so all of them are
            // sent at every level -- including the "leave it alone" values at level
            // 0 -- rather than only the ones that reduce something. Omitting one
            // would let a stale Quake4Config.cfg keep a reduction the spinner says
            // is off, and the setting would look broken until the config was wiped.
            val picmip = SpinnerWidget.fetchValue(ctx, PICMIP_PREFIX, PICMIP_DEFAULT)
                .coerceIn(0, PICMIP_LABELS.size - 1)
            info.args += " +set image_picmip $picmip "
            info.args += " +set image_picmipFilter $PICMIP_FILTER_ALL "
            info.args += " +set image_picmipMinSize $PICMIP_MIN_SIZE "

            val bumpLimit = PICMIP_BUMP_LIMITS[picmip]
            info.args += " +set image_downSizeBump ${if (bumpLimit > 0) 1 else 0} "
            info.args += " +set image_downSizeBumpLimit $bumpLimit "

            val specularLimit = PICMIP_SPECULAR_LIMITS[picmip]
            info.args += " +set image_downSizeSpecular ${if (specularLimit > 0) 1 else 0} "
            info.args += " +set image_downSizeSpecularLimit $specularLimit "

            // Held off deliberately; see the note on the limit tables above.
            info.args += " +set image_downSize 0 "
            info.args += " +set image_downSizeLimit 0 "

            // Everything safe to change mid-game comes from the shared list the
            // in-game dialog also sends; see immediateGraphicsCvars for the details.
            for ((name, value) in immediateGraphicsCvars(ctx))
                info.args += " +set $name $value "

            info.args += " +set r_glesD3PresentSceneTarget ${bit(ctx, PRESENT_SCENE_TARGET_PREFIX, PRESENT_SCENE_TARGET_DEFAULT)} "

            // Sent at every position for the same reason as the texture detail cvars
            // above: image_useETC2 is CVAR_ARCHIVE, so leaving it out at level 0
            // would let a stale config keep compressing.
            val compression = SpinnerWidget.fetchValue(ctx, TEXTURE_COMPRESSION_PREFIX, TEXTURE_COMPRESSION_DEFAULT)
                .coerceIn(0, TEXTURE_COMPRESSION_LABELS.size - 1)
            info.args += " +set image_useETC2 $compression "
        }

        // Outside the master switch on purpose: the app's own resolution control.
        for ((name, value) in resolutionScaleCvars(ctx))
            info.args += " +set $name $value "

        info.args += " +set s_releaseSamplePayload ${bit(ctx, RELEASE_SAMPLE_PAYLOAD_PREFIX, RELEASE_SAMPLE_PAYLOAD_DEFAULT)} "

        val smoothIndex = SpinnerWidget.fetchValue(ctx, MOUSE_SMOOTH_PREFIX, MOUSE_SMOOTH_DEFAULT)
            .coerceIn(0, MOUSE_SMOOTH_LABELS.size - 1)
        info.args += " +set m_smooth ${smoothIndex + 1} "

        info.args += " +set in_toggleCrouch ${bit(ctx, TOGGLE_CROUCH_PREFIX, TOGGLE_CROUCH_DEFAULT)} "
        info.args += " +set in_toggleZoom ${bit(ctx, TOGGLE_ZOOM_PREFIX, TOGGLE_ZOOM_DEFAULT)} "

        // CVAR_INIT, so the command line is the only place this can be set.
        info.args += " +set fs_validateOfficialPaks ${bit(ctx, VALIDATE_PAKS_PREFIX, VALIDATE_PAKS_DEFAULT)} "

        info.gamepadConfig = GamepadConfigWidget.fetchValue(ctx, GAMEPAD_CONFIG_KEY)

        return info
    }

    override fun hasMultiplayer(): Boolean
    {
        return false
    }

    override fun launchMultiplayer(ac: Activity, engine: GameEngine, version: Int, mainArgs: String, callback: MultiplayerCallback)
    {
    }
}
