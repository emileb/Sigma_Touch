package com.opentouchgaming.sigmatouch

import android.app.Activity
import androidx.core.util.Pair
import com.opentouchgaming.androidcore.AppInfo
import com.opentouchgaming.androidcore.DebugLog
import com.opentouchgaming.androidcore.GameEngine
import com.opentouchgaming.androidcore.SubGame
import com.opentouchgaming.androidcore.Utils
import com.opentouchgaming.androidcore.common.GameLauncherInterface
import com.opentouchgaming.sigmatouch.engineoptions.EngineOptionsUT2004
import java.io.File
import java.util.ArrayList

// Unreal Tournament 2004 (engine-ut2004) launcher, sibling of UT99Launcher.
class UT2004Launcher : GameLauncherInterface
{
    private val log = DebugLog(DebugLog.Module.CONTROLS, "UT2004Launcher")

    val SUB_DIR = "UT2004"

    val WEAPON_WHEEL_NBR = 10

    override fun updateSubGames(engine: GameEngine, availableSubGames: ArrayList<SubGame>)
    {
        log.log(DebugLog.Level.D, "updateSubGames")

        availableSubGames.clear()

        File(runDirectory).mkdirs()

        // XGame.u is UT2004's gameplay package; Engine.u is in every Unreal install.
        SubGame.addGame(availableSubGames, runDirectory, secondaryDirectory, SUB_DIR, "", 0, WEAPON_WHEEL_NBR,
                arrayOf("System/XGame.u", "System/Engine.u"), R.drawable.ut2004_icon, "Unreal Tournament 2004",
                "Copy your UT2004 folders (System, Maps, Textures, Sounds, Music, Animations, StaticMeshes, Speech) to: ",
                "Put your Unreal Tournament 2004 folders here.txt")

        for (game in availableSubGames)
        {
            if (game.getName() != null && game.getRootPath() != null)
            {
                game.load(AppInfo.getContext())
            }
        }
    }

    override fun getRunDirectory(): String
    {
        return AppInfo.getAppDirectory() + "/$SUB_DIR"
    }

    override fun getSecondaryDirectory(): String?
    {
        val secFolder = AppInfo.getAppSecDirectory()

        return if (secFolder != null) "$secFolder/$SUB_DIR" else null
    }

    override fun getQuickCommandsDirectory(subGame: SubGame): Pair<String, String>
    {
        val commonPath = AppInfo.getQuickCommandsPath() + "/" + SUB_DIR
        val modPath = commonPath + "/" + subGame.name
        return Pair(commonPath, modPath)
    }

    override fun getArgs(engine: GameEngine, subGame: SubGame): String
    {
        // No CPU-speed probe off x86, so the engine needs CPUSPEED. -INI= before -USERINI=: the engine finds "INI="
        // by substring.
        var args = " CPUSPEED=3000"
        args += " -INI=" + EngineOptionsUT2004.iniFile.absolutePath
        args += " -USERINI=" + EngineOptionsUT2004.userIniFile.absolutePath
        return args
    }

    override fun checkForDownloads(activity: Activity, engine: GameEngine, subGame: SubGame): Boolean
    {
        // Seed both inis from the engine's Android boot inis on first run only, so player changes persist.
        val dir = EngineOptionsUT2004.iniDir.absolutePath
        if (!EngineOptionsUT2004.iniFile.exists())
        {
            Utils.copyAsset(activity, EngineOptionsUT2004.INI_ASSET, dir, EngineOptionsUT2004.INI_FILENAME)
        }
        if (!EngineOptionsUT2004.userIniFile.exists())
        {
            Utils.copyAsset(activity, EngineOptionsUT2004.USER_INI_ASSET, dir, EngineOptionsUT2004.USER_INI_FILENAME)
        }
        return false
    }
}
