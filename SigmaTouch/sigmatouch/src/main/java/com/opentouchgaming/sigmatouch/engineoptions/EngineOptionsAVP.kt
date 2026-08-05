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
import com.opentouchgaming.androidcore.ui.widgets.DeleteDataWidget
import com.opentouchgaming.androidcore.ui.widgets.GamepadConfigWidget
import com.opentouchgaming.androidcore.ui.widgets.ResolutionOptionsWidget
import com.opentouchgaming.saffal.FileSAF
import com.opentouchgaming.sigmatouch.R
import com.opentouchgaming.sigmatouch.databinding.DialogOptionsAvpBinding
import java.util.Locale

// Minimal Phase 1 options, mirrors EngineOptionsUT99. Real settings come in Phase 2.
class EngineOptionsAVP : EngineOptionsInterface
{
    var log = DebugLog(DebugLog.Module.GAMEFRAGMENT, "EngineOptionsAVP")

    lateinit var binding: DialogOptionsAvpBinding

    lateinit var dialog: Dialog

    lateinit var resolutionOptionsWidget: ResolutionOptionsWidget

    val PREFIX = "avp"

    val GAMEPAD_CONFIG_KEY = "avp_gamepad_config"

    override fun showDialog(activity: Activity, engine: GameEngine, version: Int, update: Function<Int, Void>)
    {
        binding = DialogOptionsAvpBinding.inflate(activity.layoutInflater)

        dialog = Dialog(activity, R.style.DialogEngineSettings)
        dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.setTitle("Aliens vs Predator options")
        dialog.setContentView(binding.root)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)

        // Framebuffer scaler resolution (handled by the Android SDL layer). AVP
        // picks its own render size from the desktop display mode, so this only
        // drives the offscreen framebuffer size for now.
        resolutionOptionsWidget = ResolutionOptionsWidget(activity, binding.glResolution.root, PREFIX)

        // Menus and HUD text are drawn at fixed pixel sizes, so they shrink as the
        // screen gets bigger. Stored as the SeekBar step, UI_SCALE_STEP each.
        val scaleStep = AppSettings.getIntOption(activity, UI_SCALE_PREFIX, UI_SCALE_DEFAULT).coerceIn(0, UI_SCALE_MAX_STEP)
        binding.uiScaleSlider.progress = scaleStep
        binding.uiScaleValue.text = uiScaleLabel(scaleStep)
        binding.uiScaleSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener
        {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean)
            {
                binding.uiScaleValue.text = uiScaleLabel(progress)
                AppSettings.setIntOption(activity, UI_SCALE_PREFIX, progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        GamepadConfigWidget(activity, binding.gamepadConfigSpinner.root, GAMEPAD_CONFIG_KEY)

        DeleteDataWidget(
            activity, binding.deleteDataButton.root,
            "Delete all Aliens vs Predator settings files?", arrayOf("/$USER_DIR_NAME/"), arrayOf(),
            "", arrayOf(""), arrayOf("")
        )

        dialog.setOnDismissListener { resolutionOptionsWidget.save() }

        dialog.show()
    }

    // AVP writes its config and saves under user_files/avp (always writable) -
    // see the USER_FILES branch in the engine's files.c InitGameDirectories().
    companion object
    {
        const val USER_DIR_NAME = "avp"

        const val UI_SCALE_PREFIX = "avp_ui_scale"
        const val UI_SCALE_STEP = 0.25f
        const val UI_SCALE_MAX_STEP = 12 // 1.0x .. 4.0x
        const val UI_SCALE_DEFAULT = 4 // 2.0x

        fun uiScaleValue(step: Int) = 1.0f + step * UI_SCALE_STEP

        fun uiScaleLabel(step: Int) = "Menu/HUD size: %.2fx".format(uiScaleValue(step))

        val userDir: FileSAF
            get() = FileSAF(AppInfo.getUserFiles(), USER_DIR_NAME)
    }

    override fun getRunInfo(version: Int): RunInfo
    {
        val info = RunInfo()

        // The renderer is desktop GL 1.x translated by gl4es onto a GLES2
        // context - see src/oglfunc.c's dlopen of libGL4ES.so.
        info.glesVersion = 2
        info.useGL4ES = true

        // NakedAVP is an SDL3 engine, so it needs the app3000 SDL activity.
        info.sdlVersion = 3

        val res = ResolutionOptionsWidget.getResOption(PREFIX)
        info.frameBufferWidth = res.w
        info.frameBufferHeight = res.h
        info.maintainAspect = res.maintainAspect

        // Must be non-null: SigmaFragment concatenates runInfo.args into the
        // command line, and a null here becomes the literal string "null".
        info.args = " "

        // Locale.US: the engine parses this with atof(), which wants a dot.
        val scaleStep = AppSettings.getIntOption(AppInfo.getContext(), UI_SCALE_PREFIX, UI_SCALE_DEFAULT).coerceIn(0, UI_SCALE_MAX_STEP)
        info.args += " -u %.2f ".format(Locale.US, uiScaleValue(scaleStep))

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
