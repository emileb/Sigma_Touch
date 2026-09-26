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
import com.opentouchgaming.androidcore.AppSettings
import com.opentouchgaming.androidcore.ui.widgets.GamepadConfigWidget
import com.opentouchgaming.androidcore.ui.widgets.ResolutionOptionsWidget
import com.opentouchgaming.androidcore.ui.widgets.SpinnerWidget
import com.opentouchgaming.androidcore.ui.widgets.SwitchWidget
import com.opentouchgaming.saffal.FileSAF
import com.opentouchgaming.sigmatouch.R
import com.opentouchgaming.sigmatouch.databinding.DialogOptionsFarcryBinding

// Minimal Phase 1 options, mirrors EngineOptionsAVP. Real settings come in Phase 2.
class EngineOptionsFarCry : EngineOptionsInterface
{
    var log = DebugLog(DebugLog.Module.GAMEFRAGMENT, "EngineOptionsFarCry")

    lateinit var binding: DialogOptionsFarcryBinding

    lateinit var dialog: Dialog

    lateinit var resolutionOptionsWidget: ResolutionOptionsWidget

    val PREFIX = "farcry"

    val GAMEPAD_CONFIG_KEY = "farcry_gamepad_config"

    override fun showDialog(activity: Activity, engine: GameEngine, version: Int, update: Function<Int, Void>)
    {
        binding = DialogOptionsFarcryBinding.inflate(activity.layoutInflater)

        dialog = Dialog(activity, R.style.DialogEngineSettings)
        dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.setTitle("Far Cry options")
        dialog.setContentView(binding.root)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)

        // The renderer's own scene framebuffer takes this size (not the SDL scaler), so the touch
        // controls stay at full resolution.
        resolutionOptionsWidget = ResolutionOptionsWidget(activity, binding.glResolution.root, PREFIX)
        dialog.setOnDismissListener { resolutionOptionsWidget.save() }

        val aimItems = AIM_ASSIST_LEVELS.map { Pair<String, View?>(it, null) }.toTypedArray()
        SpinnerWidget(
            activity, binding.aimAssistSpinner.root, "Aim assist",
            "Helps aim at the enemy nearest the crosshair. Light slows your swipe over a target and pulls gently, Strong pulls harder and follows the target while firing, Snap also turns onto the target when you press fire.",
            aimItems, AIM_ASSIST_PREFIX, AIM_ASSIST_DEFAULT, R.drawable.joystick
        )

        SwitchWidget(
            activity, binding.devModeSwitch.root, "Developer mode",
            "Starts the engine with -DEVMODE: console cheats and the developer keys.",
            DEVMODE_PREFIX, DEVMODE_DEFAULT, R.drawable.setting_gear
        )

        // How glBegin/glEnd geometry reaches the driver; the menu's UI windows issue hundreds of
        // draws per frame, so the choice makes a 5x difference on Adreno (see docs/engines/farcry.md).
        val uploadItems = UPLOAD_MODES.map { Pair<String, View?>(it, null) }.toTypedArray()
        SpinnerWidget(
            activity, binding.uploadModeSpinner.root, "Vertex upload",
            "How immediate-mode geometry is handed to the GPU driver. Client arrays are fastest on Adreno; try the others if the menu or HUD is slow.",
            uploadItems, UPLOAD_MODE_PREFIX, UPLOAD_MODE_DEFAULT, R.drawable.setting_gpu
        )

        // Bullet marks: each live decal adds draw calls per light pass, so sustained fire at the
        // ground is the heaviest load on low-end GPUs.
        val decalItems = DECAL_PRESETS.map { Pair<String, View?>(it, null) }.toTypedArray()
        SpinnerWidget(
            activity, binding.decalSpinner.root, "Bullet decals",
            "Marks left by bullets and explosions. Reduce them on low-end devices if the frame rate drops while firing.",
            decalItems, DECAL_PREFIX, DECAL_DEFAULT, R.drawable.setting_gpu
        )

        // How the renderer's per-frame buffer rewrites (light passes, decals, particles) reach the GPU.
        val mapItems = MAP_MODES.map { Pair<String, View?>(it, null) }.toTypedArray()
        SpinnerWidget(
            activity, binding.mapModeSpinner.root, "Buffer updates",
            "How dynamic vertex and index buffers are updated each frame. The persistent ring is fastest on Adreno (firing at the ground drops to 17 fps without it); the others are for comparison.",
            mapItems, MAP_MODE_PREFIX, MAP_MODE_DEFAULT, R.drawable.setting_gpu
        )

        SwitchWidget(
            activity, binding.glesDebugSwitch.root, "Renderer debug log",
            "FARCRY_GLES_DEBUG: logs shader sources, uploads, GL errors and per-path timing. Slow.",
            GLES_DEBUG_PREFIX, GLES_DEBUG_DEFAULT, R.drawable.setting_gpu
        )

        // Development harness: load a level straight from the command line and
        // dismiss the menus, the same run the Mac tools/run-gles.sh does.
        val levelItems = START_LEVELS.map { Pair<String, View?>(it, null) }.toTypedArray()
        SpinnerWidget(
            activity, binding.startLevelSpinner.root, "Dev: start in level",
            "Skips the intro, menus and opening cutscene and loads this level at launch.",
            levelItems, START_LEVEL_PREFIX, START_LEVEL_DEFAULT, R.drawable.setting_gear
        )

        GamepadConfigWidget(activity, binding.gamepadConfigSpinner.root, GAMEPAD_CONFIG_KEY)

        DeleteDataWidget(
            activity, binding.deleteDataButton.root,
            "Delete all Far Cry settings files?", arrayOf("/$USER_DIR_NAME/"), CONFIG_FILENAMES,
            "", arrayOf(""), arrayOf("")
        )

        dialog.show()
    }

    companion object
    {
        // Phase 2 redirects profiles, system.cfg and log.txt here (see docs/engines/farcry.md).
        const val USER_DIR_NAME = "farcry"

        val CONFIG_FILENAMES = arrayOf("system.cfg", "game.cfg")

        // Spinner index = cl_aim_assist level (CryGame/AimAssist.cpp).
        const val AIM_ASSIST_PREFIX = "farcry_aim_assist"
        const val AIM_ASSIST_DEFAULT = 1
        val AIM_ASSIST_LEVELS = arrayOf("Off", "Light", "Strong", "Snap")

        const val DEVMODE_PREFIX = "farcry_devmode"
        const val DEVMODE_DEFAULT = false

        const val GLES_DEBUG_PREFIX = "farcry_gles_debug"
        const val GLES_DEBUG_DEFAULT = false

        // Spinner index -> FARCRY_GLES_IMMODE value (gles_vertex.cpp). Measured on a Note 20 Ultra
        // in the Options menu: client arrays 52 fps, glBufferData 40, subdata ring 16, mapped ring 9.
        const val UPLOAD_MODE_PREFIX = "farcry_upload_mode"
        const val UPLOAD_MODE_DEFAULT = 0
        val UPLOAD_MODES = arrayOf("Client arrays (fastest)", "glBufferData per draw", "glBufferSubData ring", "Mapped ring (unsynchronized)", "Persistent ring")
        val UPLOAD_MODE_VALUES = intArrayOf(3, 1, 0, 2, 4)

        // Spinner index -> engine cvars (-CVAR:name=value, run after the configs load). Measured on a
        // Note 20 Ultra, lowest fps over two holds of fire at the ground on Pier: full 77, reduced 104, off 120.
        const val DECAL_PREFIX = "farcry_decals"
        const val DECAL_DEFAULT = 0
        val DECAL_PRESETS = arrayOf("Full", "Reduced", "Minimal", "Off")
        val DECAL_ARGS = arrayOf(
            "",
            "-CVAR:e_decals_max_count=128 -CVAR:e_decals_life_time_scale=0.5",
            "-CVAR:e_decals_max_count=32 -CVAR:e_decals_life_time_scale=0.25",
            "-CVAR:e_decals=0"
        )

        // Spinner index -> FARCRY_GLES_MAPMODE value (gles_vertex.cpp). Measured on a Note 20 Ultra holding
        // fire at the ground on Pier: persistent ring 60-150 fps, CPU copy + glBufferData 17-40.
        const val MAP_MODE_PREFIX = "farcry_map_mode"
        const val MAP_MODE_DEFAULT = 0
        val MAP_MODES = arrayOf("Persistent ring (fastest)", "CPU copy + glBufferData", "CPU copy + glBufferSubData", "Direct map")
        val MAP_MODE_VALUES = intArrayOf(4, 1, 2, 0)

        // Index 0 is "menu"; the rest are the campaign levels in story order.
        const val START_LEVEL_PREFIX = "farcry_start_level"
        const val START_LEVEL_DEFAULT = 0
        val START_LEVELS = arrayOf(
            "Menu (normal start)", "Training", "Carrier", "Fort", "Pier", "Research", "Treehouse", "Bunker", "Steam",
            "Regulator", "Control", "Rebellion", "Archive", "Cooler", "Boat", "Catacombs", "River", "Swamp",
            "Factory", "Dam", "Volcano"
        )

        // Raw extra arguments, no UI: set with
        //   adb shell run-as com.opentouchgaming.sigmatouch  (edit shared_prefs/OPTIONS.xml)
        // e.g. "-ENV:FARCRY_SCREENSHOT_FRAME=3000 -ENV:FARCRY_SCREENSHOT_FILE=shot.jpg".
        const val DEV_ARGS_PREFIX = "farcry_dev_args"

        val userDir: FileSAF
            get() = FileSAF(AppInfo.getUserFiles(), USER_DIR_NAME)
    }

    override fun getRunInfo(version: Int): RunInfo
    {
        val info = RunInfo()

        // libXRenderGLES.so: the engine's own renderer on an ES 3.0 context.
        info.glesVersion = 3
        info.useGL4ES = false

        // NearChuckle is an SDL3 engine, so it needs the app3000 SDL activity.
        info.sdlVersion = 3

        // -RENDERER:GLES picks the ES module regardless of the saved r_Driver.
        info.args = " -RENDERER:GLES "

        if (SwitchWidget.fetchValue(AppInfo.getContext(), DEVMODE_PREFIX, DEVMODE_DEFAULT))
            info.args += " -DEVMODE "

        // frameBufferWidth stays null: the GLES layer renders at this size itself and scales up at the
        // swap, under the overlay. $W/$H placeholders are replaced with the display size at launch.
        val res = ResolutionOptionsWidget.getResOption(PREFIX)
        if (res.position != 0)
            info.args += " -ENV:FARCRY_RENDER_SIZE=${res.w}x${res.h} "
        // Aspect only applies to the fixed and custom sizes (the widget hides it for the Screen / n entries).
        if (res.maintainAspect && res.position >= 4)
            info.args += " -ENV:FARCRY_RENDER_ASPECT=1 "

        val aimAssist = SpinnerWidget.fetchValue(AppInfo.getContext(), AIM_ASSIST_PREFIX, AIM_ASSIST_DEFAULT)
        info.args += " -ENV:FARCRY_AIM_ASSIST=$aimAssist "

        // -ENV:NAME=VALUE is turned into setenv() by PortableInit before the engine starts.
        if (SwitchWidget.fetchValue(AppInfo.getContext(), GLES_DEBUG_PREFIX, GLES_DEBUG_DEFAULT))
            info.args += " -ENV:FARCRY_GLES_DEBUG=1 "

        val uploadMode = SpinnerWidget.fetchValue(AppInfo.getContext(), UPLOAD_MODE_PREFIX, UPLOAD_MODE_DEFAULT)
        if (uploadMode in UPLOAD_MODE_VALUES.indices)
            info.args += " -ENV:FARCRY_GLES_IMMODE=" + UPLOAD_MODE_VALUES[uploadMode] + " "

        val decals = SpinnerWidget.fetchValue(AppInfo.getContext(), DECAL_PREFIX, DECAL_DEFAULT)
        if (decals in DECAL_ARGS.indices && DECAL_ARGS[decals].isNotEmpty())
            info.args += " " + DECAL_ARGS[decals] + " "

        val mapMode = SpinnerWidget.fetchValue(AppInfo.getContext(), MAP_MODE_PREFIX, MAP_MODE_DEFAULT)
        if (mapMode in MAP_MODE_VALUES.indices)
            info.args += " -ENV:FARCRY_GLES_MAPMODE=" + MAP_MODE_VALUES[mapMode] + " "

        val startLevel = SpinnerWidget.fetchValue(AppInfo.getContext(), START_LEVEL_PREFIX, START_LEVEL_DEFAULT)
        if (startLevel in 1 until START_LEVELS.size)
            info.args += " -MAP:" + START_LEVELS[startLevel].lowercase() + " -ENV:FARCRY_RESUME_FRAME=100 -ENV:FARCRY_SKIP_CUTSCENES=1 "

        val devArgs = AppSettings.getStringOption(AppInfo.getContext(), DEV_ARGS_PREFIX, "")
        if (!devArgs.isNullOrBlank())
            info.args += " $devArgs "

        info.gamepadConfig = GamepadConfigWidget.fetchValue(AppInfo.getContext(), GAMEPAD_CONFIG_KEY)

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
