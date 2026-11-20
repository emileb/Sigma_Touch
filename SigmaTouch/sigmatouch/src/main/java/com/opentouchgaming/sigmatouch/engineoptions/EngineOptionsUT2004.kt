package com.opentouchgaming.sigmatouch.engineoptions

import android.app.Activity
import android.app.Dialog
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
import com.opentouchgaming.saffal.FileSAF
import com.opentouchgaming.sigmatouch.R
import com.opentouchgaming.sigmatouch.databinding.DialogOptionsUt2004Binding

// Minimal Phase 1 options, mirrors EngineOptionsAVP.
class EngineOptionsUT2004 : EngineOptionsInterface
{
    var log = DebugLog(DebugLog.Module.GAMEFRAGMENT, "EngineOptionsUT2004")

    lateinit var binding: DialogOptionsUt2004Binding

    lateinit var dialog: Dialog

    val GAMEPAD_CONFIG_KEY = "ut2004_gamepad_config"

    override fun showDialog(activity: Activity, engine: GameEngine, version: Int, update: Function<Int, Void>)
    {
        binding = DialogOptionsUt2004Binding.inflate(activity.layoutInflater)

        dialog = Dialog(activity, R.style.DialogEngineSettings)
        dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.setTitle("Unreal Tournament 2004 options")
        dialog.setContentView(binding.root)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)

        GamepadConfigWidget(activity, binding.gamepadConfigSpinner.root, GAMEPAD_CONFIG_KEY)

        DeleteDataWidget(
            activity, binding.deleteDataButton.root,
            "Delete all Unreal Tournament 2004 settings files?", arrayOf("/$USER_DIR_NAME/"), arrayOf(INI_FILENAME, USER_INI_FILENAME),
            "", arrayOf(""), arrayOf("")
        )

        dialog.show()
    }

    // Both inis live in user_files/ut2004 (always writable), seeded by UT2004Launcher and passed with -INI=/-USERINI=.
    companion object
    {
        const val USER_DIR_NAME = "ut2004"

        const val INI_FILENAME = "UT2004.ini"
        const val USER_INI_FILENAME = "User.ini"

        // The engine's Android boot inis (the :UT2004 module's assets).
        const val INI_ASSET = "ut2004/Default.ini"
        const val USER_INI_ASSET = "ut2004/DefUser.ini"

        val iniDir: FileSAF
            get() = FileSAF(AppInfo.getUserFiles(), USER_DIR_NAME)

        val iniFile: FileSAF
            get() = FileSAF(iniDir, INI_FILENAME)

        val userIniFile: FileSAF
            get() = FileSAF(iniDir, USER_INI_FILENAME)
    }

    override fun getRunInfo(version: Int): RunInfo
    {
        val info = RunInfo()

        // GL4ESDrv draws through the engine's own gl4es on a GLES2 context; the touch overlay draws with plain GLES2.
        info.glesVersion = 2
        info.useGL4ES = false

        // Must be non-null: SigmaFragment concatenates runInfo.args into the
        // command line, and a null here becomes the literal string "null".
        info.args = " "

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
