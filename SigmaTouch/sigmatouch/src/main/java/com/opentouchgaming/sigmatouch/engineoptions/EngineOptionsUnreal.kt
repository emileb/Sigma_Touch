package com.opentouchgaming.sigmatouch.engineoptions

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.arch.core.util.Function
import com.opentouchgaming.androidcore.AppInfo
import com.opentouchgaming.androidcore.AppSettings
import com.opentouchgaming.androidcore.DebugLog
import com.opentouchgaming.androidcore.EngineOptionsInterface
import com.opentouchgaming.androidcore.EngineOptionsInterface.MultiplayerCallback
import com.opentouchgaming.androidcore.EngineOptionsInterface.RunInfo
import com.opentouchgaming.androidcore.GameEngine
import com.opentouchgaming.androidcore.Utils
import com.opentouchgaming.androidcore.ui.widgets.DeleteDataWidget
import com.opentouchgaming.androidcore.ui.widgets.GamepadConfigWidget
import com.opentouchgaming.androidcore.ui.widgets.ResolutionOptionsWidget
import com.opentouchgaming.androidcore.ui.widgets.SwitchWidget
import com.opentouchgaming.saffal.FileSAF
import com.opentouchgaming.sigmatouch.R
import com.opentouchgaming.sigmatouch.databinding.DialogOptionsUnrealBinding
import java.io.File

// Minimal Phase 1 options. Grows real settings (audio) in Phase 2.
class EngineOptionsUnreal : EngineOptionsInterface
{
    var log = DebugLog(DebugLog.Module.GAMEFRAGMENT, "EngineOptionsUnreal")

    lateinit var binding: DialogOptionsUnrealBinding

    lateinit var dialog: Dialog

    lateinit var resolutionOptionsWidget: ResolutionOptionsWidget

    val PREFIX = "unreal"

    val GAMEPAD_CONFIG_KEY = "unreal_gamepad_config"

    // Render-quality toggles. VolumetricLighting/ShinySurfaces/Coronas/HighDetailActors
    // are URenderDevice CPF_Config bools; DetailTextures is the GL driver's own. No
    // in-menu option reaches them, and the engine defaults all to true, so expose them
    // here and pass overrides on the command line - parsed in NOpenGLESDrv::Init.
    // Base (unzoomed) FOV slider, passed via -FOV= and applied by NOpenGLESDrv's
    // AutoFOV path. SeekBar is 0..30 (min SDK 19 has no android:min), offset by FOV_MIN.
    val FOV_PREFIX = "unreal_fov"
    val FOV_MIN = 90
    val FOV_MAX = 120
    val FOV_DEFAULT = 90

    // Hosting a game (botmatch = listen server) makes MainLoop() sleep to the net
    // driver's MaxTicksPerSecond, which caps frame rate, not just replication - the
    // ini ships 35. Passed via -MaxTickRate=, applied in UGameEngine::GetMaxTickRate.
    // SeekBar is 0..9 (min SDK 19 has no android:min), so value = MIN + progress*STEP.
    val TICK_RATE_PREFIX = "unreal_tick_rate"
    val TICK_RATE_MIN = 30
    val TICK_RATE_STEP = 10
    val TICK_RATE_MAX = 120
    val TICK_RATE_DEFAULT = 60

    // Vanilla blocks a new dodge until 0.35s after the previous one lands (and
    // kills the landing velocity). Off by default - unrestricted dodging is far
    // more useful with the touch dodge buttons than with a double-tap.
    val DODGE_COOLDOWN_PREFIX = "unreal_dodge_cooldown"
    val DODGE_COOLDOWN_DEFAULT = false

    val VOLUMETRIC_LIGHTING_PREFIX = "unreal_volumetric_lighting"
    val SHINY_SURFACES_PREFIX = "unreal_shiny_surfaces"
    val CORONAS_PREFIX = "unreal_coronas"
    val HIGH_DETAIL_ACTORS_PREFIX = "unreal_high_detail_actors"
    val DETAIL_TEXTURES_PREFIX = "unreal_detail_textures"
    val RENDER_TOGGLE_DEFAULT = true

    // System/Unreal.ini isn't always writable (SAF-backed installs, read-only
    // mounts), so the engine is pointed at a copy in user_files instead, always
    // on real, writable app storage. Companion object so UE1Launcher.checkForDownloads
    // can seed it from assets without duplicating the path.
    companion object
    {
        const val INI_DIR_NAME = "unreal"
        const val INI_FILENAME = "Unreal.ini"

        val iniFile: File
            get() = FileSAF(AppInfo.getUserFiles() + "/$INI_DIR_NAME", INI_FILENAME)
    }

    // Snapped to a SeekBar step so a stale saved value still lands on a tick.
    private fun tickRateSetting(context: Context): Int
    {
        val raw = AppSettings.getIntOption(context, TICK_RATE_PREFIX, TICK_RATE_DEFAULT).coerceIn(TICK_RATE_MIN, TICK_RATE_MAX)
        return TICK_RATE_MIN + ((raw - TICK_RATE_MIN) / TICK_RATE_STEP) * TICK_RATE_STEP
    }

    override fun showDialog(activity: Activity, engine: GameEngine, version: Int, update: Function<Int, Void>)
    {
        binding = DialogOptionsUnrealBinding.inflate(activity.layoutInflater)

        dialog = Dialog(activity, R.style.DialogEngineSettings)
        dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.setTitle("Unreal options")
        dialog.setContentView(binding.root)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)

        // Render resolution / framebuffer scaler, passed to the engine via -ResX=/-ResY=
        // (Source/Engine/Src/UnGame.cpp), same idea as OpenJK's r_customwidth/r_customheight.
        resolutionOptionsWidget = ResolutionOptionsWidget(activity, binding.glResolution.root, PREFIX)

        // Field of view slider. Stored as the real degree value (FOV_MIN..FOV_MAX);
        // the SeekBar works in 0..(FOV_MAX-FOV_MIN) and is offset for display/save.
        // Leftmost (FOV_MIN) shows "Auto" - the engine's aspect-corrected default.
        fun fovLabel(deg: Int) = "Field of view: " + if (deg <= FOV_MIN) "Auto" else "$deg°"
        val fov = AppSettings.getIntOption(activity, FOV_PREFIX, FOV_DEFAULT).coerceIn(FOV_MIN, FOV_MAX)
        binding.fovSlider.progress = fov - FOV_MIN
        binding.fovValue.text = fovLabel(fov)
        binding.fovSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener
        {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean)
            {
                val deg = FOV_MIN + progress
                binding.fovValue.text = fovLabel(deg)
                AppSettings.setIntOption(activity, FOV_PREFIX, deg)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        // Host tick rate. Snapped to TICK_RATE_STEP so the SeekBar stays coarse.
        val tickRate = tickRateSetting(activity)
        binding.tickRateSlider.progress = (tickRate - TICK_RATE_MIN) / TICK_RATE_STEP
        binding.tickRateValue.text = "Multiplayer tick rate: $tickRate"
        binding.tickRateSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener
        {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean)
            {
                val rate = TICK_RATE_MIN + progress * TICK_RATE_STEP
                binding.tickRateValue.text = "Multiplayer tick rate: $rate"
                AppSettings.setIntOption(activity, TICK_RATE_PREFIX, rate)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        SwitchWidget(activity, binding.dodgeCooldownSwitch.root, "Dodge cooldown", "Original delay between dodges (off: dodge freely)",
            DODGE_COOLDOWN_PREFIX, DODGE_COOLDOWN_DEFAULT)

        SwitchWidget(activity, binding.volumetricLightingSwitch.root, "Volumetric lighting", "Fog around dynamic lights",
            VOLUMETRIC_LIGHTING_PREFIX, RENDER_TOGGLE_DEFAULT, R.drawable.setting_gpu)
        SwitchWidget(activity, binding.shinySurfacesSwitch.root, "Shiny surfaces", "Reflective/translucent surface effects",
            SHINY_SURFACES_PREFIX, RENDER_TOGGLE_DEFAULT, R.drawable.setting_gpu)
        SwitchWidget(activity, binding.coronasSwitch.root, "Coronas", "Light flares around bright light sources",
            CORONAS_PREFIX, RENDER_TOGGLE_DEFAULT, R.drawable.setting_gpu)
        SwitchWidget(activity, binding.highDetailActorsSwitch.root, "High detail actors", "Higher detail models for players/monsters",
            HIGH_DETAIL_ACTORS_PREFIX, RENDER_TOGGLE_DEFAULT, R.drawable.setting_gpu)
        SwitchWidget(activity, binding.detailTexturesSwitch.root, "Detail textures", "Extra close-up texture detail",
            DETAIL_TEXTURES_PREFIX, RENDER_TOGGLE_DEFAULT, R.drawable.setting_gpu)

        GamepadConfigWidget(activity, binding.gamepadConfigSpinner.root, GAMEPAD_CONFIG_KEY)

        DeleteDataWidget(
            activity, binding.deleteDataButton.root,
            "Delete all Unreal settings files?", arrayOf("/$INI_DIR_NAME/"), arrayOf(INI_FILENAME),
            "", arrayOf(""), arrayOf("")
        )

        dialog.setOnDismissListener { resolutionOptionsWidget.save() }

        dialog.show()
    }

    override fun getRunInfo(version: Int): RunInfo
    {
        val info = RunInfo()
        // NOpenGLESDrv is an OpenGL ES 2.0 renderer (glad_es loader).
        info.glesVersion = 2

        // Render the engine at the selected resolution and let the native framebuffer
        // scaler blit it to the screen, same pattern as OpenJK's EngineOptionsOpenJKBase.
        val res = ResolutionOptionsWidget.getResOption(PREFIX)
        info.frameBufferWidth = res.w
        info.frameBufferHeight = res.h
        info.maintainAspect = res.maintainAspect

        info.args = " -ResX=${res.w} -ResY=${res.h} "

        // Base FOV (90..120), applied by NOpenGLESDrv's AutoFOV path.
        val fov = AppSettings.getIntOption(AppInfo.getContext(), FOV_PREFIX, FOV_DEFAULT).coerceIn(FOV_MIN, FOV_MAX)
        info.args += " -FOV=$fov "

        info.gamepadConfig = GamepadConfigWidget.fetchValue(AppInfo.getContext(), GAMEPAD_CONFIG_KEY)

        // Host tick rate - parsed in UGameEngine::GetMaxTickRate, overriding the
        // ini's MaxTicksPerSecond. Only applies while hosting; ignored otherwise.
        info.args += " -MaxTickRate=" + tickRateSetting(AppInfo.getContext())

        // Dodge rate limit - parsed in mobile/game_interface.cpp.
        info.args += " -DodgeCooldown=" + SwitchWidget.fetchValue(AppInfo.getContext(), DODGE_COOLDOWN_PREFIX, DODGE_COOLDOWN_DEFAULT)

        // Render-quality toggles - parsed in NOpenGLESDrv::Init, overriding the
        // ini-loaded (engine-default-true) values.
        info.args += " -VolumetricLighting=" + SwitchWidget.fetchValue(AppInfo.getContext(), VOLUMETRIC_LIGHTING_PREFIX, RENDER_TOGGLE_DEFAULT)
        info.args += " -ShinySurfaces=" + SwitchWidget.fetchValue(AppInfo.getContext(), SHINY_SURFACES_PREFIX, RENDER_TOGGLE_DEFAULT)
        info.args += " -Coronas=" + SwitchWidget.fetchValue(AppInfo.getContext(), CORONAS_PREFIX, RENDER_TOGGLE_DEFAULT)
        info.args += " -HighDetailActors=" + SwitchWidget.fetchValue(AppInfo.getContext(), HIGH_DETAIL_ACTORS_PREFIX, RENDER_TOGGLE_DEFAULT)
        info.args += " -DetailTextures=" + SwitchWidget.fetchValue(AppInfo.getContext(), DETAIL_TEXTURES_PREFIX, RENDER_TOGGLE_DEFAULT)

        // Point the engine at the writable user_files copy of Unreal.ini.
        info.args += " -INI=" + Utils.quoteString(iniFile.absolutePath)

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
