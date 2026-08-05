package com.opentouchgaming.sigmatouch

import android.app.Activity
import androidx.core.util.Pair
import com.opentouchgaming.androidcore.AppInfo
import com.opentouchgaming.androidcore.DebugLog
import com.opentouchgaming.androidcore.GameEngine
import com.opentouchgaming.androidcore.SubGame
import com.opentouchgaming.androidcore.Utils
import com.opentouchgaming.androidcore.common.GameLauncherInterface
import java.io.File
import java.util.ArrayList

// Aliens vs Predator (NakedAVP) launcher.
// See CLAUDE.md "AVP (NakedAVP) engine notes" for the -p / data layout details.
class AVPLauncher : GameLauncherInterface
{
    private val log = DebugLog(DebugLog.Module.CONTROLS, "AVPLauncher")

    val SUB_DIR = "AVP"

    val WEAPON_WHEEL_NBR = 10

    override fun updateSubGames(engine: GameEngine, availableSubGames: ArrayList<SubGame>)
    {
        log.log(DebugLog.Level.D, "updateSubGames")

        availableSubGames.clear()

        File(runDirectory).mkdirs()

        // Same files the engine's own check_game_directory() (src/files.c) tests
        // for. Note the data folder must be all lowercase - the engine builds
        // lowercase paths and Android storage is case sensitive.
        SubGame.addGame(availableSubGames, runDirectory, secondaryDirectory, SUB_DIR, "", 0, WEAPON_WHEEL_NBR,
                arrayOf("avp_huds/alien.rif", "avp_rifs/temple.rif", "fastfile"), R.drawable.avp_icon, "Aliens vs Predator",
                "Copy your (lowercased) AvP Gold folders (avp_huds, avp_rifs, fastfile, graphics, sound) to: ",
                "Put your Aliens vs Predator folders here.txt")

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
        // -p is the engine's own game-data path option (src/main.c getopt). Always
        // absolute: SAFFAL matches SAF-backed storage by literal path prefix and
        // can never resolve a relative lookup.
        var args = ""
        if (subGame.rootPath != null)
        {
            args += " -p " + Utils.quoteString(subGame.rootPath)
        }

        return args
    }

    override fun checkForDownloads(activity: Activity, engine: GameEngine, subGame: SubGame): Boolean
    {
        return false
    }
}
