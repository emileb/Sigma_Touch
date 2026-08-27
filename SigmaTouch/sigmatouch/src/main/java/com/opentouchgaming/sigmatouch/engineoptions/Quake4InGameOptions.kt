package com.opentouchgaming.sigmatouch.engineoptions

import android.app.Activity
import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import com.opentouchgaming.androidcore.InGameOptionsInterface
import com.opentouchgaming.androidcore.ui.widgets.SwitchWidget
import com.opentouchgaming.sigmatouch.R
import com.opentouchgaming.sigmatouch.databinding.DialogOptionsQuake4IngameBinding
import com.opentouchgaming.sigmatouch.engineoptions.EngineOptionsQuake4.Companion.GRAPHICS_SETTINGS_DEFAULT
import com.opentouchgaming.sigmatouch.engineoptions.EngineOptionsQuake4.Companion.GRAPHICS_SETTINGS_PREFIX

// Shown over the running game (in the :game process) by the overlay's settings
// button. Only the cvars that take effect immediately are offered; on dismiss
// they are pushed to the engine console, and the shared prefs keep the next
// launch's +set args in step.
class Quake4InGameOptions : InGameOptionsInterface
{
    override fun showDialog(activity: Activity, sender: InGameOptionsInterface.CommandSender)
    {
        val binding = DialogOptionsQuake4IngameBinding.inflate(activity.layoutInflater)

        val dialog = Dialog(activity, R.style.DialogEngineSettings)
        dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.setTitle("Quake 4 graphics")
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

        EngineOptionsQuake4.createImmediateGraphicsWidgets(
            activity, binding.screenFractionSpinner.root, binding.resolutionScaleFilterSpinner.root,
            binding.shadowsSwitch.root, binding.specularSwitch.root, binding.bumpSwitch.root,
            binding.ambientSwitch.root, binding.shaderAmbientSwitch.root, binding.fpsCapSpinner.root,
            binding.vsyncSwitch.root, binding.showFpsSwitch.root
        )

        // The resolution pair is always the app's to send; the rest only while
        // the master switch says the app owns them, matching launch.
        dialog.setOnDismissListener {
            for ((name, value) in EngineOptionsQuake4.resolutionScaleCvars(activity))
                sender.send("set $name $value")

            if (SwitchWidget.fetchValue(activity, GRAPHICS_SETTINGS_PREFIX, GRAPHICS_SETTINGS_DEFAULT))
            {
                for ((name, value) in EngineOptionsQuake4.immediateGraphicsCvars(activity))
                    sender.send("set $name $value")
            }
        }

        dialog.show()
    }
}
