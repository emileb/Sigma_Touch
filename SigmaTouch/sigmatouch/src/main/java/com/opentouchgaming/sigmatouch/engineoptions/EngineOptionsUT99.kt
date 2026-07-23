package com.opentouchgaming.sigmatouch.engineoptions

import android.app.Activity
import android.app.Dialog
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
import com.opentouchgaming.sigmatouch.databinding.DialogOptionsUt99Binding
import java.io.File

// Minimal Phase 1 options, mirrors EngineOptionsUnreal. Real settings (audio,
// engine-side -ResX/-ResY) come in Phase 2.
class EngineOptionsUT99 : EngineOptionsInterface
{
    var log = DebugLog(DebugLog.Module.GAMEFRAGMENT, "EngineOptionsUT99")

    lateinit var binding: DialogOptionsUt99Binding

    lateinit var dialog: Dialog

    lateinit var resolutionOptionsWidget: ResolutionOptionsWidget

    val PREFIX = "ut99"

    val GAMEPAD_CONFIG_KEY = "ut99_gamepad_config"

    // Render-quality toggles (NOpenGLESRenderDevice's CPF_Config bools -
    // UnCamera.cpp's URenderDevice::StaticConstructor). No UWindow checkbox
    // reaches these on Android (menu is mouse-driven, buttons removed), and the
    // engine constructor defaults all of them to true, so expose them here and
    // pass overrides on the command line - see NSDLClient.cpp::TryRenderDevice.
    // FOV slider, passed via -FOV= and applied by the config-read intercept
    // (FConfigCacheIni.h) which reports it for the player's globalconfig DefaultFOV.
    // SeekBar is 0..30 (min SDK 19 has no android:min), offset by FOV_MIN.
    val FOV_PREFIX = "ut99_fov"
    val FOV_MIN = 90
    val FOV_MAX = 120
    val FOV_DEFAULT = 90

    val VOLUMETRIC_LIGHTING_PREFIX = "ut99_volumetric_lighting"
    val SHINY_SURFACES_PREFIX = "ut99_shiny_surfaces"
    val CORONAS_PREFIX = "ut99_coronas"
    val HIGH_DETAIL_ACTORS_PREFIX = "ut99_high_detail_actors"
    val DETAIL_TEXTURES_PREFIX = "ut99_detail_textures"
    val RENDER_TOGGLE_DEFAULT = true

    override fun showDialog(activity: Activity, engine: GameEngine, version: Int, update: Function<Int, Void>)
    {
        binding = DialogOptionsUt99Binding.inflate(activity.layoutInflater)

        dialog = Dialog(activity, R.style.DialogEngineSettings)
        dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.setTitle("Unreal Tournament options")
        dialog.setContentView(binding.root)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)

        // Framebuffer scaler resolution (handled by the Android SDL layer). The
        // engine-side -ResX=/-ResY= flags are a UE1-only patch not yet ported to
        // UT99, so for now this only drives the offscreen framebuffer size.
        resolutionOptionsWidget = ResolutionOptionsWidget(activity, binding.glResolution.root, PREFIX)

        // Field of view slider. Stored as the real degree value (FOV_MIN..FOV_MAX);
        // the SeekBar works in 0..(FOV_MAX-FOV_MIN) and is offset for display/save.
        // Leftmost (FOV_MIN) shows "Default" - no override, the game's default FOV.
        fun fovLabel(deg: Int) = "Field of view: " + if (deg <= FOV_MIN) "Default" else "$deg°"
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

        GamepadConfigWidget(activity, binding.gamepadConfigSpinner.root, GAMEPAD_CONFIG_KEY)

        SwitchWidget(activity, binding.volumetricLightingSwitch.root, "Volumetric lighting", "Fog around dynamic lights",
            VOLUMETRIC_LIGHTING_PREFIX, RENDER_TOGGLE_DEFAULT)
        SwitchWidget(activity, binding.shinySurfacesSwitch.root, "Shiny surfaces", "Reflective/translucent surface effects",
            SHINY_SURFACES_PREFIX, RENDER_TOGGLE_DEFAULT)
        SwitchWidget(activity, binding.coronasSwitch.root, "Coronas", "Light flares around bright light sources",
            CORONAS_PREFIX, RENDER_TOGGLE_DEFAULT)
        SwitchWidget(activity, binding.highDetailActorsSwitch.root, "High detail actors", "Higher detail models for players/monsters",
            HIGH_DETAIL_ACTORS_PREFIX, RENDER_TOGGLE_DEFAULT)
        SwitchWidget(activity, binding.detailTexturesSwitch.root, "Detail textures", "Extra close-up texture detail",
            DETAIL_TEXTURES_PREFIX, RENDER_TOGGLE_DEFAULT)

        DeleteDataWidget(
            activity, binding.deleteDataButton.root,
            "Delete all Unreal Tournament settings files?", arrayOf("/$INI_DIR_NAME/"), arrayOf(INI_FILENAME, USER_INI_FILENAME),
            "", arrayOf(""), arrayOf("")
        )

        dialog.setOnDismissListener { resolutionOptionsWidget.save() }

        dialog.show()
    }

    // Both config files (UnrealTournament.ini + User.ini) live in user_files/ut99
    // (always writable). Companion object so UT99Launcher.checkForDownloads can
    // seed them from assets without duplicating the paths.
    companion object
    {
        const val INI_DIR_NAME = "ut99"
        const val INI_FILENAME = "UnrealTournament.ini"
        const val USER_INI_FILENAME = "User.ini"

        val iniDir: FileSAF
            get() = FileSAF(AppInfo.getUserFiles(), INI_DIR_NAME)

        val iniFile: FileSAF
            get() = FileSAF(iniDir, INI_FILENAME)

        val userIniFile: FileSAF
            get() = FileSAF(iniDir, USER_INI_FILENAME)
    }

    override fun getRunInfo(version: Int): RunInfo
    {
        val info = RunInfo()
        // NOpenGLESDrv is an OpenGL ES 2.0 renderer (glad_es loader).
        info.glesVersion = 2

        val res = ResolutionOptionsWidget.getResOption(PREFIX)
        info.frameBufferWidth = res.w
        info.frameBufferHeight = res.h
        info.maintainAspect = res.maintainAspect

        // Must be non-null: SigmaFragment concatenates runInfo.args into the
        // command line, and a null here becomes the literal string "null", which
        // the engine parses as the startup map name (-> "Failed to enter null").
        // -ResX/-ResY are a no-op on UT99 for now (engine-side parsing is a UE1
        // patch not yet ported); the leading '-' also makes UnGame.cpp fall back
        // to the ini's default map, which is the correct Phase 1 startup.
        info.args = " -ResX=${res.w} -ResY=${res.h} "

        // FOV (90..120). Read by the config-intercept in FConfigCacheIni.h, which
        // reports it for the player's globalconfig DefaultFOV. Ignored at 90 (Auto).
        val fov = AppSettings.getIntOption(AppInfo.getContext(), FOV_PREFIX, FOV_DEFAULT).coerceIn(FOV_MIN, FOV_MAX)
        info.args += " -FOV=$fov "

        info.gamepadConfig = GamepadConfigWidget.fetchValue(AppInfo.getContext(), GAMEPAD_CONFIG_KEY)

        // Render-quality toggles - parsed in NSDLClient.cpp::TryRenderDevice,
        // overriding the ini-loaded (engine-default-true) values.
        info.args += " -VolumetricLighting=" + SwitchWidget.fetchValue(AppInfo.getContext(), VOLUMETRIC_LIGHTING_PREFIX, RENDER_TOGGLE_DEFAULT)
        info.args += " -ShinySurfaces=" + SwitchWidget.fetchValue(AppInfo.getContext(), SHINY_SURFACES_PREFIX, RENDER_TOGGLE_DEFAULT)
        info.args += " -Coronas=" + SwitchWidget.fetchValue(AppInfo.getContext(), CORONAS_PREFIX, RENDER_TOGGLE_DEFAULT)
        info.args += " -HighDetailActors=" + SwitchWidget.fetchValue(AppInfo.getContext(), HIGH_DETAIL_ACTORS_PREFIX, RENDER_TOGGLE_DEFAULT)
        info.args += " -DetailTextures=" + SwitchWidget.fetchValue(AppInfo.getContext(), DETAIL_TEXTURES_PREFIX, RENDER_TOGGLE_DEFAULT)

        // Point both ini files at the writable user_files copies.
        info.args += " -INI=" + Utils.quoteString(iniFile.absolutePath)
        info.args += " -USERINI=" + Utils.quoteString(userIniFile.absolutePath)

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
