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

// Far Cry (NearChuckle) launcher. See docs/engines/farcry.md for the data layout.
class FarCryLauncher : GameLauncherInterface
{
    private val log = DebugLog(DebugLog.Module.CONTROLS, "FarCryLauncher")

    val SUB_DIR = "FarCry"

    val WEAPON_WHEEL_NBR = 10

    override fun updateSubGames(engine: GameEngine, availableSubGames: ArrayList<SubGame>)
    {
        log.log(DebugLog.Level.D, "updateSubGames")

        availableSubGames.clear()

        File(runDirectory).mkdirs()

        // The retail install: FCData, Levels, Shaders and Languages beside each other.
        SubGame.addGame(availableSubGames, runDirectory, secondaryDirectory, SUB_DIR, "", 0, WEAPON_WHEEL_NBR,
                arrayOf("FCData/Objects.pak", "FCData/Scripts.pak", "FCData/Textures.pak"), R.drawable.farcry_icon, "Far Cry",
                "Copy your Far Cry 'FCData', 'Levels', 'Shaders', 'Languages' and 'Profiles' folders to: ",
                "Put your Far Cry folders here.txt")

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
        var args = ""

        // Always absolute: on secondary storage the game folder is a SAF path that SAFFAL matches by
        // literal prefix and the JNI glue cannot chdir() into, so the engine builds every path on it.
        if (subGame.rootPath != null)
            args += " -GAMEPATH " + Utils.quoteString(subGame.rootPath)

        // Generated caches (terrain, shader script, lightmap) go to the app's cache folder, never the game data.
        args += " -CACHEPATH " + Utils.quoteString(AppInfo.cacheFiles + "/farcry")

        // The shader cache pak is staged on primary storage; only outside the game folder does it need naming.
        // Named even before it exists: the args are built on selection, the pak is staged at launch.
        val pak = File(shaderCacheDir, SHADER_CACHE_PAK)
        if (subGame.rootPath != runDirectory)
            args += " -SHADERPAK " + Utils.quoteString(pak.absolutePath)

        return args
    }

    override fun checkForDownloads(activity: Activity, engine: GameEngine, subGame: SubGame): Boolean
    {
        stageShaderCache(activity, subGame)
        return false
    }

    // The ES renderer runs the precompiled ARB shaders from the GL shader cache pak
    // (no Cg on device), so the pak ships in the APK and is copied once. It always goes
    // to primary storage (writable, a real path), which is the game folder itself when the
    // data is there too. Stale copies are refreshed on an app update like Quake 4's paks.
    private val shaderCacheDir: File
        get() = File(runDirectory, "FCData")

    private fun stageShaderCache(activity: Activity, subGame: SubGame)
    {
        val destDir = shaderCacheDir
        val stamp = File(destDir, STAGE_STAMP_FILENAME)

        val installedAt = try
        {
            "${activity.packageManager.getPackageInfo(activity.packageName, 0).lastUpdateTime}"
        }
        catch (e: Exception)
        {
            ""
        }

        val stampedAt = runCatching { if (stamp.exists()) stamp.readText().trim() else "" }.getOrDefault("")

        if (installedAt.isNotEmpty() && stampedAt == installedAt && File(destDir, SHADER_CACHE_PAK).length() > 0)
            return

        log.log(DebugLog.Level.D, "staging $SHADER_CACHE_PAK into $destDir")
        Utils.copyAsset(activity, "$ASSET_DIR/$SHADER_CACHE_PAK", destDir.absolutePath, SHADER_CACHE_PAK)

        // Superseded paks we staged; in the game folder the engine would open them too.
        for (old in OLD_SHADER_CACHE_PAKS)
            runCatching { File(destDir, old).delete() }

        if (File(destDir, SHADER_CACHE_PAK).length() > 0 && installedAt.isNotEmpty())
            runCatching { stamp.writeText(installedAt) }
        else
            log.log(DebugLog.Level.E, "failed to stage $SHADER_CACHE_PAK into $destDir")
    }

    companion object
    {
        // Games/FarCry/src/main/assets/farcry/, merged into the APK by AGP.
        private const val ASSET_DIR = "farcry"

        // The 20260517 community cache plus the programs it lacked, compiled with NearChuckle's tools/cgbuild.py.
        private const val SHADER_CACHE_PAK = "GL_Shaders_20260927.pak"

        private val OLD_SHADER_CACHE_PAKS = arrayOf("GL_Shaders_20260517.pak")

        private const val STAGE_STAMP_FILENAME = ".shadercache_staged"
    }
}
