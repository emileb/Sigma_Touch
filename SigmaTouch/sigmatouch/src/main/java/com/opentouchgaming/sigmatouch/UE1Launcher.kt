package com.opentouchgaming.sigmatouch

import android.app.Activity
import androidx.core.util.Pair
import com.opentouchgaming.androidcore.AppInfo
import com.opentouchgaming.androidcore.DebugLog
import com.opentouchgaming.androidcore.GameEngine
import com.opentouchgaming.androidcore.SubGame
import com.opentouchgaming.androidcore.Utils
import com.opentouchgaming.androidcore.common.GameLauncherInterface
import com.opentouchgaming.sigmatouch.engineoptions.EngineOptionsUnreal
import java.io.File
import java.util.ArrayList

class UE1Launcher : GameLauncherInterface
{
    private val log = DebugLog(DebugLog.Module.CONTROLS, "UE1Launcher")

    val SUB_DIR = "UE1"

    val WEAPON_WHEEL_NBR = 10

    override fun updateSubGames(engine: GameEngine, availableSubGames: ArrayList<SubGame>)
    {
        log.log(DebugLog.Level.D, "updateSubGames")

        availableSubGames.clear()

        File(runDirectory).mkdirs()

        // Every Unreal install (retail or 205 demo) ships these two packages.
        SubGame.addGame(availableSubGames, runDirectory, secondaryDirectory, SUB_DIR, "", 0, WEAPON_WHEEL_NBR,
                arrayOf("System/Engine.u", "Maps/Entry.unr"), R.drawable.unreal_icon, "Unreal",
                "Copy your Unreal folders (System, Maps, Textures, Sounds, Music) to: ", "Put your Unreal folders here.txt")

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
        // On real (non-SAF) storage the engine finds its own files via the
        // classic CWD-relative "../Maps/", "../Textures/" etc paths (game_path
        // sets CWD to .../UE1/System). SAFFAL's interceptor decides "is this
        // SAF-backed" purely by string prefix - it never resolves "../" against
        // the working directory - so a relative path can never match and silently
        // falls through to a real (failing) fopen when the data is SAF-backed.
        // -GamePath tells the engine (Core/Src/UnPlat.cpp) to rewrite those
        // relative lookups into absolute, SAF-rooted ones instead.
        var args = ""
        if (secondaryDirectory != null && subGame.rootPath != null && subGame.rootPath.contentEquals(secondaryDirectory!!))
        {
            args += " -GamePath=" + Utils.quoteString(secondaryDirectory)
        }

        return args
    }

    override fun checkForDownloads(activity: Activity, engine: GameEngine, subGame: SubGame): Boolean
    {
        // System/Unreal.ini isn't writable when the game data is SAF-backed
        // (and may not be on real storage either, e.g. read-only mounts) -
        // EngineOptionsUnreal points the engine at a copy in user_files instead;
        // seed it from assets/Unreal.ini on first run only, so player changes persist.
        val ini = EngineOptionsUnreal.iniFile
        if (!ini.exists())
        {
            Utils.copyAsset(activity, EngineOptionsUnreal.INI_FILENAME, ini.parent)
        }
        return false
    }
}
