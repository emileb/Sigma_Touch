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

// Quake 4 (openQ4) launcher. See docs/engines/quake4.md for the data layout.
class Quake4Launcher : GameLauncherInterface
{
    private val log = DebugLog(DebugLog.Module.CONTROLS, "Quake4Launcher")

    // Names the game folder under both storage roots, and tags the SubGame.
    val SUB_DIR = "Q4"

    val WEAPON_WHEEL_NBR = 10

    override fun updateSubGames(engine: GameEngine, availableSubGames: ArrayList<SubGame>)
    {
        log.log(DebugLog.Level.D, "updateSubGames")

        availableSubGames.clear()

        File(runDirectory).mkdirs()

        // Only the retail install is the player's to provide. baseoq4/pak0.pk4
        // and pak1.pk4 ship in the APK and are staged by checkForDownloads, so
        // they must not be listed here: addGame only offers a game once every
        // required file is present, and checkForDownloads does not run until a
        // game can be launched. Listing them would deadlock the two.
        SubGame.addGame(availableSubGames, runDirectory, secondaryDirectory, SUB_DIR, "", 0, WEAPON_WHEEL_NBR,
                arrayOf("q4base/pak001.pk4"), R.drawable.quake4_icon, "Quake 4",
                "Copy your Quake 4 'q4base' folder to: ",
                "Put your Quake 4 q4base folder here.txt")

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
        // Always absolute: SAFFAL matches SAF-backed storage by literal path
        // prefix and can never resolve a relative lookup, and the engine's own
        // Sys_DefaultBasePath only ever reports the cwd.
        var args = ""
        if (subGame.rootPath != null)
        {
            args += " +set fs_basepath " + Utils.quoteString(subGame.rootPath)
        }

        return args
    }

    override fun checkForDownloads(activity: Activity, engine: GameEngine, subGame: SubGame): Boolean
    {
        stageRuntimePaks(activity)
        return false
    }

    // openQ4's own content packs travel in the APK and are unpacked next to the
    // player's retail data. The engine compiles their MD5s in and fatal-errors
    // on a mismatch, so a stale copy is not a soft failure -- it stops the game
    // starting. That makes the staleness test the important part: the packs are
    // rewritten whenever the app has been installed or updated since the last
    // staging, which covers a rebuild during development just as well as a
    // store update, and costs one small file read on every other launch.
    private fun stageRuntimePaks(activity: Activity)
    {
        // Always primary, even when the retail data was found on the secondary
        // volume: the app can always write here, which is not true of removable
        // storage on every device.
        val destDir = File(runDirectory, GAME_DIR)
        val stamp = File(destDir, STAGE_STAMP_FILENAME)

        val installedAt = try
        {
            val info = activity.packageManager.getPackageInfo(activity.packageName, 0)
            "${info.lastUpdateTime}"
        }
        catch (e: Exception)
        {
            log.log(DebugLog.Level.W, "could not read package info, staging anyway: $e")
            ""
        }

        // Any failure reading the stamp means "stage again": re-copying 7MB is
        // cheap, and skipping it wrongly stops the game booting.
        val stampedAt = runCatching { if (stamp.exists()) stamp.readText().trim() else "" }.getOrDefault("")

        val alreadyStaged = installedAt.isNotEmpty() &&
                stampedAt == installedAt &&
                PAK_FILENAMES.all { File(destDir, it).length() > 0 }

        if (alreadyStaged)
        {
            return
        }

        log.log(DebugLog.Level.D, "staging openQ4 content packs into $destDir")
        destDir.mkdirs()

        for (pak in PAK_FILENAMES)
        {
            Utils.copyAsset(activity, "$ASSET_DIR/$pak", destDir.absolutePath, pak)
        }

        // copyAsset logs and swallows its own IO errors, so confirm the result
        // rather than recording a success that did not happen. Leaving the
        // stamp absent means the next launch tries again.
        val staged = PAK_FILENAMES.all { File(destDir, it).length() > 0 }
        if (staged && installedAt.isNotEmpty())
        {
            runCatching { stamp.writeText(installedAt) }
        }
        else if (!staged)
        {
            log.log(DebugLog.Level.E, "failed to stage openQ4 content packs into $destDir")
        }
    }

    companion object
    {
        // Where the engine expects its own packs, relative to fs_basepath.
        private const val GAME_DIR = "baseoq4"

        // Written by the Quake4 CMake build via OPENQ4_PAK_ASSET_DIR.
        private const val ASSET_DIR = "quake4"

        // mod.json belongs here as much as the packs do: the engine refuses to
        // start when fs_game is its own game dir and the manifest is missing or
        // its requiredopenQ4Version does not match, which is the same fatal
        // error a stale pack produces. All three are written by the same CMake
        // configure pass, so they are only ever consistent as a set.
        private val PAK_FILENAMES = arrayOf("pak0.pk4", "pak1.pk4", "mod.json")

        private const val STAGE_STAMP_FILENAME = ".staged"
    }
}
