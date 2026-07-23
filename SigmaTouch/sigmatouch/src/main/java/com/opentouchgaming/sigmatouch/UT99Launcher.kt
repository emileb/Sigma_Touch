package com.opentouchgaming.sigmatouch

import android.app.Activity
import androidx.core.util.Pair
import com.opentouchgaming.androidcore.AppInfo
import com.opentouchgaming.androidcore.DebugLog
import com.opentouchgaming.androidcore.GameEngine
import com.opentouchgaming.androidcore.SubGame
import com.opentouchgaming.androidcore.Utils
import com.opentouchgaming.androidcore.common.GameLauncherInterface
import com.opentouchgaming.sigmatouch.engineoptions.EngineOptionsUT99
import java.io.File
import java.util.ArrayList

// Unreal Tournament (engine v400) launcher, sibling of UE1Launcher.
// See CLAUDE.md "UT99 engine notes" for the -GamePath/-INI wiring details.
class UT99Launcher : GameLauncherInterface
{
    private val log = DebugLog(DebugLog.Module.CONTROLS, "UT99Launcher")

    val SUB_DIR = "UT99"

    val WEAPON_WHEEL_NBR = 10

    override fun updateSubGames(engine: GameEngine, availableSubGames: ArrayList<SubGame>)
    {
        log.log(DebugLog.Level.D, "updateSubGames")

        availableSubGames.clear()

        File(runDirectory).mkdirs()

        // Botpack.u is the UT-specific gameplay package (distinguishes a UT install
        // from plain Unreal); Engine.u is present in every UE1-family install.
        SubGame.addGame(availableSubGames, runDirectory, secondaryDirectory, SUB_DIR, "", 0, WEAPON_WHEEL_NBR,
                arrayOf("System/Botpack.u", "System/Engine.u"), R.drawable.ut99_icon, "Unreal Tournament",
                "Copy your UnrealTournament folders (System, Maps, Textures, Sounds, Music) to: ",
                "Put your Unreal Tournament folders here.txt")

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
        // SAF storage can't resolve the engine's CWD-relative lookups, so pass
        // -GamePath= for absolute rewrites (Core/Src/UnMisc.cpp).
        var args = ""
        if (secondaryDirectory != null && subGame.rootPath != null && subGame.rootPath.contentEquals(secondaryDirectory!!))
        {
            args += " -GamePath=" + Utils.quoteString(secondaryDirectory)
        }

        return args
    }

    override fun checkForDownloads(activity: Activity, engine: GameEngine, subGame: SubGame): Boolean
    {
        // Seed both ini files from assets on first run only, so player changes persist.
        if (!EngineOptionsUT99.iniFile.exists())
        {
            Utils.copyAsset(activity, EngineOptionsUT99.INI_FILENAME, EngineOptionsUT99.iniDir.absolutePath)
        }
        if (!EngineOptionsUT99.userIniFile.exists())
        {
            Utils.copyAsset(activity, EngineOptionsUT99.USER_INI_FILENAME, EngineOptionsUT99.iniDir.absolutePath)
        }
        return false
    }
}
