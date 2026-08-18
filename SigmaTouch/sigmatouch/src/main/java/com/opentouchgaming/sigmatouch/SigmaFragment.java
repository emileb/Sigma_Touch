package com.opentouchgaming.sigmatouch;

import static com.opentouchgaming.androidcore.DebugLog.Level.D;

import android.content.Intent;
import android.os.Bundle;

import androidx.core.util.Pair;

import com.opentouchgaming.androidcore.AppInfo;
import com.opentouchgaming.androidcore.DebugLog;
import com.opentouchgaming.androidcore.EngineOptionsInterface;
import com.opentouchgaming.androidcore.GameEngine;
import com.opentouchgaming.androidcore.common.LaunchIntent;
import com.opentouchgaming.androidcore.common.MainFragment;
import com.opentouchgaming.androidcore.ui.OptionsDialogKt;
import com.opentouchgaming.androidcore.ui.widgets.SpinnerWidget;


public class SigmaFragment extends MainFragment
{
    static DebugLog log;

    static
    {
        log = new DebugLog(DebugLog.Module.GAMEFRAGMENT, "SigmaFragment");
    }

    UE1Launcher ue1Launcher;

    UT99Launcher ut99Launcher;

    AVPLauncher avpLauncher;

    Quake4Launcher quake4Launcher;

    public SigmaFragment()
    {
        super();
        log.log(D, "New instant created!");
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        noLicCheck = true;

        ue1Launcher = new UE1Launcher();
        ut99Launcher = new UT99Launcher();
        avpLauncher = new AVPLauncher();
        quake4Launcher = new Quake4Launcher();
    }

    public void setLauncher()
    {
        switch (AppInfo.currentEngine.engine)
        {
            case UNREAL:
                launcher = ue1Launcher;
                break;
            case UNREAL_TOURNAMENT:
                launcher = ut99Launcher;
                break;
            case AVP:
                launcher = avpLauncher;
                break;
            case QUAKE4:
                launcher = quake4Launcher;
                break;
        }
    }


    public void launchGame(final GameEngine engine, boolean download, final String multiplayerArgs)
    {
        final String rootPath = launcher.getRunDirectory(selectedSubGame);

        // Check if not installed yet
        if (selectedSubGame.getName() == null)
        {
            return;
        }

        // Check for engine specific downloads or issues
        if (download)
        {
            if (launcher.checkForDownloads(getActivity(), AppInfo.currentEngine, selectedSubGame))
                return;
        }

        // Save history
        engineData.addArgsHistory();

        EngineOptionsInterface.RunInfo runInfo = engine.engineOptions.getRunInfo(selectedVersion);

        // Build args
        String args = engine.args + " ";
        args += runInfo.args + " ";
        args += argsFinal;

        // Create Intent. AVP and Quake 4 are SDL3 engines (app3000); the UE1-family ones are SDL2.
        Intent intent;

        if (runInfo.sdlVersion == 3)
        {
            intent = new Intent(getActivity(), org.libsdl.app3000.SDLActivity.class);
        }
        else
        {
            intent = new Intent(getActivity(), org.libsdl.app2012.SDLActivity.class);
        }

        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        intent.putExtra("load_libs", engine.loadLibs[selectedVersion]);
        intent.putExtra("log_filename", engine.getLogFilename());

        // The UE1-family engines run out of their own System/ subfolder; AVP's
        // and Quake 4's data sits directly in the game folder.
        if (engine.engine == GameEngine.Engine.AVP || engine.engine == GameEngine.Engine.QUAKE4)
            intent.putExtra("game_path", rootPath);
        else
            intent.putExtra("game_path", rootPath + "/System");

        intent.putExtra("args", args);

        Pair<String, String> quickCommandPaths = launcher.getQuickCommandsDirectory(selectedSubGame);
        int wheelNbr = selectedSubGame.getWheelNbr();
        LaunchIntent.populateIntent(getActivity(), intent, engine.engineOptions, runInfo, 0, wheelNbr, quickCommandPaths);

        startActivity(intent);
    }
}
