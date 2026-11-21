package com.opentouchgaming.sigmatouch

import android.app.Activity
import androidx.core.util.Pair
import com.opentouchgaming.androidcore.AppInfo
import com.opentouchgaming.androidcore.AppSettings
import com.opentouchgaming.androidcore.DebugLog
import com.opentouchgaming.androidcore.GameEngine
import com.opentouchgaming.androidcore.SubGame
import com.opentouchgaming.androidcore.Utils
import com.opentouchgaming.androidcore.common.GameLauncherInterface
import com.opentouchgaming.sigmatouch.engineoptions.EngineOptionsQuake4
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

        // only openQ4 0.14+ plays the expansion alpha
        if (selectedVersion(AppInfo.getContext()) == EngineOptionsQuake4.DEV_VERSION)
        {
            // The alpha comes as loose files or packed into pk4s
            val looseMap = arrayOf("q4base/pak001.pk4", "q4xbase/maps/game/m01_stranarus_trench1.map")
            val files = if (Utils.checkFilesInPaths(runDirectory, secondaryDirectory, looseMap) != null) looseMap
                        else arrayOf("q4base/pak001.pk4", "q4xbase/pak0.pk4")
            SubGame.addGame(availableSubGames, runDirectory, secondaryDirectory, AWAKENING_TAG, "", GAME_AWAKENING, WEAPON_WHEEL_NBR,
                    files, R.drawable.quake4_icon,
                    "Quake 4: The Awakening",
                    "Copy the Awakening 'q4xbase' folder next to 'q4base' in: ",
                    "Put your Quake 4 q4base folder here.txt")
        }

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
        // dev keeps its own quick commands, like its other controls
        val dev = selectedVersion(AppInfo.getContext()) == EngineOptionsQuake4.DEV_VERSION
        val commonPath = AppInfo.getQuickCommandsPath() + "/" + SUB_DIR + (if (dev) "_dev" else "")
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

        // si_gameType lets the startup filesystem mount q4xbase, so no engine reload is needed
        if (subGame.gameType == GAME_AWAKENING)
        {
            args += " +set si_gameType singleplayer +set com_nextGameModule game_sp +set fs_game q4xbase"
        }

        return args
    }

    override fun checkForDownloads(activity: Activity, engine: GameEngine, subGame: SubGame): Boolean
    {
        val version = selectedVersion(activity)
        stageRuntimePaks(activity, if (version == EngineOptionsQuake4.DEV_VERSION) DEV_PAKS else RELEASE_PAKS)
        return false
    }

    // the version MainFragment last selected for this engine
    private fun selectedVersion(context: android.content.Context) =
        AppSettings.getIntOption(context, "last_version_" + GameEngine.Engine.QUAKE4.toString(), 0)

    // openQ4's own content packs travel in the APK and are unpacked next to the
    // player's retail data. The engine compiles their MD5s in and fatal-errors
    // on a mismatch, so a stale copy is not a soft failure -- it stops the game
    // starting. That makes the staleness test the important part: the packs are
    // rewritten whenever the app has been installed or updated since the last
    // staging, which covers a rebuild during development just as well as a
    // store update, and costs one small file read on every other launch.
    // Both versions use the same baseoq4 folder, so the stamp names the version
    // too and switching re-stages: each engine bakes in its own packs' MD5s.
    private fun stageRuntimePaks(activity: Activity, paks: PakSet)
    {
        // Always primary, even when the retail data was found on the secondary
        // volume: the app can always write here, which is not true of removable
        // storage on every device.
        val destDir = File(runDirectory, GAME_DIR)
        val stamp = File(destDir, STAGE_STAMP_FILENAME)

        val installedAt = try
        {
            val info = activity.packageManager.getPackageInfo(activity.packageName, 0)
            "${paks.assetDir}:${info.lastUpdateTime}"
        }
        catch (e: Exception)
        {
            log.log(DebugLog.Level.W, "could not read package info, staging anyway: $e")
            ""
        }

        // Any failure reading the stamp means "stage again": re-copying the
        // packs is cheap, and skipping it wrongly stops the game booting.
        val stampedAt = runCatching { if (stamp.exists()) stamp.readText().trim() else "" }.getOrDefault("")

        val alreadyStaged = installedAt.isNotEmpty() &&
                stampedAt == installedAt &&
                paks.files.all { File(destDir, it).length() > 0 }

        if (alreadyStaged)
        {
            return
        }

        log.log(DebugLog.Level.D, "staging openQ4 content packs from ${paks.assetDir} into $destDir")
        destDir.mkdirs()

        // The engine mounts every pakN.pk4 in baseoq4, so the other version's
        // extra pack (dev's pak2) must not be left behind.
        destDir.listFiles()?.filter { it.name.matches(PAK_NAME_PATTERN) && it.name !in paks.files }?.forEach {
            log.log(DebugLog.Level.D, "removing ${it.name}, not part of ${paks.assetDir}")
            it.delete()
        }

        for (pak in paks.files)
        {
            Utils.copyAsset(activity, "${paks.assetDir}/$pak", destDir.absolutePath, pak)
        }

        // copyAsset logs and swallows its own IO errors, so confirm the result
        // rather than recording a success that did not happen. Leaving the
        // stamp absent means the next launch tries again.
        val staged = paks.files.all { File(destDir, it).length() > 0 }
        if (staged && installedAt.isNotEmpty())
        {
            runCatching { stamp.writeText(installedAt) }
        }
        else if (!staged)
        {
            log.log(DebugLog.Level.E, "failed to stage openQ4 content packs into $destDir")
        }
    }

    // assetDir is where the module's CMake build staged them (OPENQ4_PAK_ASSET_DIR).
    class PakSet(val assetDir: String, val files: Array<String>)

    companion object
    {
        // Where the engine expects its own packs, relative to fs_basepath.
        private const val GAME_DIR = "baseoq4"

        // mod.json belongs here as much as the packs do: the engine refuses to
        // start when fs_game is its own game dir and the manifest is missing or
        // its requiredopenQ4Version does not match, which is the same fatal
        // error a stale pack produces. They are all written by the same CMake
        // configure pass, so they are only ever consistent as a set.
        private val RELEASE_PAKS = PakSet("quake4", arrayOf("pak0.pk4", "pak1.pk4", "mod.json"))
        private val DEV_PAKS = PakSet("quake4dev", arrayOf("pak0.pk4", "pak1.pk4", "pak2.pk4", "mod.json"))

        private val PAK_NAME_PATTERN = Regex("pak[0-9]\\.pk4", RegexOption.IGNORE_CASE)

        private const val STAGE_STAMP_FILENAME = ".staged"

        const val GAME_AWAKENING = 1
        private const val AWAKENING_TAG = "Q4X"
    }
}
