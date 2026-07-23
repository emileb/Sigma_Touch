package com.opentouchgaming.sigmatouch;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.opentouchgaming.androidcore.AboutDialog;
import com.opentouchgaming.androidcore.AppInfo;
import com.opentouchgaming.androidcore.AppSettings;
import com.opentouchgaming.androidcore.DebugLog;
import com.opentouchgaming.androidcore.GD;
import com.opentouchgaming.androidcore.GameEngine;
import com.opentouchgaming.androidcore.ScopedStorage;
import com.opentouchgaming.androidcore.Utils;
import com.opentouchgaming.androidcore.controls.GamepadDefinitions;
import com.opentouchgaming.androidcore.ui.OptionsDialogKt;
import com.opentouchgaming.androidcore.ui.ScopedStorageDialog;
import com.opentouchgaming.androidcore.ui.ScopedStorageFirstTimeDialog;
import com.opentouchgaming.androidcore.ui.StorageConfigDialog;
import com.opentouchgaming.androidcore.ui.UserFilesDialog;
import com.opentouchgaming.androidcore.ui.tutorial.Tutorial;
import com.opentouchgaming.sigmatouch.engineoptions.EngineOptionsUT99;
import com.opentouchgaming.sigmatouch.engineoptions.EngineOptionsUnreal;

import java.util.ArrayList;
import java.util.List;


public class EntryActivity extends FragmentActivity
{
    static DebugLog log;

    static
    {
        log = new DebugLog(DebugLog.Module.APP, "EntryActivity");
    }

    static
    {
        Tutorial tut = new Tutorial("Installing games", "ic_tut_install");
        tut.addScreen(new Tutorial.Screen("Download the PC version of the game you wish to install to your computer.",
                                          "",
                                          "http://opentouchgaming.com/tutorial/quad/install_1_download.png"));
        tut.addScreen(new Tutorial.Screen("Connect your device to your PC and enable file transfer.",
                                          "",
                                          "http://opentouchgaming.com/tutorial/quad/install_2_connect.png"));
        tut.addScreen(new Tutorial.Screen("Find the path to copy files to. The path before '/OpenTouch/' is the internal memory of your device.",
                                          "",
                                          "http://opentouchgaming.com/tutorial/quad/install_3_findpath.png"));
        tut.addScreen(new Tutorial.Screen("Copy the files to your device. Default Steam location:\n" +
                                          "C:\\Program Files (x86)\\Steam\\steamapps\\common\\Quake\\Id1",
                                          "",
                                          "http://opentouchgaming.com/tutorial/quad/install_4_copyfiles.png"));
        tut.addScreen(new Tutorial.Screen("Default folder locations:", "", "http://opentouchgaming.com/tutorial/quad/install_5_default.png"));
        AppInfo.tutorials.add(tut);


        tut = new Tutorial("Using the console/keyboard", "ic_tut_keyboard");
        tut.addScreen(new Tutorial.Screen("Start a new game and press the 'cog' to edit the touch controls",
                                          "",
                                          "http://opentouchgaming.com/tutorial/quad/keyboard_1.png"));
        tut.addScreen(new Tutorial.Screen("Press the 'sliders' button", "", "http://opentouchgaming.com/tutorial/quad/keyboard_2.png"));
        tut.addScreen(new Tutorial.Screen("Press 'Hide/Show buttons'", "", "http://opentouchgaming.com/tutorial/quad/keyboard_3.png"));
        tut.addScreen(new Tutorial.Screen("Enable the Keyboard button", "", "http://opentouchgaming.com/tutorial/quad/keyboard_4.png"));
        tut.addScreen(new Tutorial.Screen("Enable the Console button", "", "http://opentouchgaming.com/tutorial/quad/keyboard_5.png"));
        AppInfo.tutorials.add(tut);

        tut = new Tutorial("Enable the gyroscope", "ic_tut_gyro");
        tut.addScreen(new Tutorial.Screen("Go to the menu of the game and press the 'gyro' button in the top right",
                                          "",
                                          "http://opentouchgaming.com/tutorial/quad/gyro_1.png"));
        tut.addScreen(new Tutorial.Screen("Enable the gyroscope. Remember your device needs to have the gyroscope hardware to enable this feature",
                                          "",
                                          "http://opentouchgaming.com/tutorial/quad/gyro_2.png"));
        AppInfo.tutorials.add(tut);


        AppInfo.gameEngines = new GameEngine[]{

                new GameEngine(GameEngine.Engine.UNREAL,
                               0,
                               "Unreal",
                               "unreal",
                               "",
                               new String[]{"dev"},
                               new String[][]{{"oboe", "openal-soft", "xmp-lite", "unreal"}},
                               "",
                               GamepadDefinitions.getDefinition(AppInfo.Apps.SIGMA_TOUCH),
                               R.drawable.unreal_icon,
                               0,
                               0x00E11C1C,
                               0,
                               EngineOptionsUnreal.class),

                new GameEngine(GameEngine.Engine.UNREAL_TOURNAMENT,
                               0,
                               "Unreal Tournament",
                               "ut99",
                               "",
                               new String[]{"dev"},
                               new String[][]{{"oboe", "openal-soft", "xmp-lite", "ut99"}},
                               "",
                               GamepadDefinitions.getDefinition(AppInfo.Apps.SIGMA_TOUCH),
                               R.drawable.ut99_icon, // TODO: placeholder art, replace with a UT99 icon
                               0,
                               0x00B18A0B,
                               0,
                               EngineOptionsUT99.class),
        };

        List<StorageConfigDialog.StorageExamples> examples = new ArrayList<>();
        examples.add(new StorageConfigDialog.StorageExamples("User files", "(config, saves etc):", StorageConfigDialog.PathLocation.PRIM, "/user_files"));

        AppInfo.storageExamples = examples;

        ScopedStorageDialog.Tutorial scopedTutorial = new ScopedStorageDialog.Tutorial();
        scopedTutorial.folder = "Device > OpenTouch > Sigma";
        scopedTutorial.items = new ArrayList<>();
        scopedTutorial.items.add(new ScopedStorageDialog.Tutorial.Item("Select where you want your files.", R.drawable.ss_2, 0));
        scopedTutorial.items.add(new ScopedStorageDialog.Tutorial.Item("Create (or select) the 'OpenTouch' folder.", R.drawable.ss_3, 0));
        scopedTutorial.items.add(new ScopedStorageDialog.Tutorial.Item("Create (or select) the 'Sigma' folder.", R.drawable.ss_4, 0));
        scopedTutorial.items.add(new ScopedStorageDialog.Tutorial.Item("Check the path is correct and press 'USE THIS FOLDER'",
                                                                       R.drawable.ss_5,
                                                                       R.drawable.ss_5a));

        AppInfo.scopedTutorial = scopedTutorial;

        AppInfo.userFilesEntries = new UserFilesDialog.UserFileEntryDescription[]{
                new UserFilesDialog.UserFileEntryDescription("Mod setups", "", R.drawable.ic_baseline_file_copy, "loadouts"),
                new UserFilesDialog.UserFileEntryDescription("Gamepad setups", "", R.drawable.ic_baseline_file_copy, "gamepad"),
                new UserFilesDialog.UserFileEntryDescription("Touch layouts", "", R.drawable.ic_baseline_file_copy, "touch_layouts"),
                new UserFilesDialog.UserFileEntryDescription("Quick cmds", "", R.drawable.ic_baseline_file_copy, "QC")};
    }

    final int REQUEST_EXTERNAL_STROAGE = 1;

    SigmaFragment mainFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Utils.setImmersionMode(this, getWindow(), OptionsDialogKt.LAUNCHER_HIDE_NAV_BAR);
        Utils.expandToCutout(this, getWindow(), OptionsDialogKt.LAUNCHER_EXPAND_INTO_NOTCH);

        AboutDialog.aboutRes = R.raw.about;

        setContentView(R.layout.activity_entry);

        Utils.setInsets(this, findViewById(R.id.activity_entry_top), false);

        GD.init(getApplicationContext());

        AppSettings.reloadSettings(getApplication());

        AppInfo.setAppInfo(getApplicationContext(),
                           AppInfo.Apps.SIGMA_TOUCH,
                           "Sigma Touch",
                           "Sigma",
                           BuildConfig.APPLICATION_ID,
                           "sigmalogs@opentouchgaming.com",
                           false,
                           0,
                           true,
                           false);


        AppInfo.website = "http://opentouchgaming.com/sigma-touch/";
        AppInfo.showRateButton = false;
        AppInfo.versionCheckKey = "sigma"; // non-null enables the online version check

        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.fragment_container) != null)
        {
            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null)
            {
                mainFragment = (SigmaFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                return;
            }

            // Create a new Fragment to be placed in the activity layout
            mainFragment = new SigmaFragment();

            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
            mainFragment.setArguments(getIntent().getExtras());

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mainFragment).commit();
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) // Android 12 and below, use old code
        {
            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STROAGE);
                log.log(DebugLog.Level.D, "Sending request");
            }
            else
            {
                // Permission has already been granted
                log.log(DebugLog.Level.D, "Permission already granted");
            }
        }
        else // Android 13 just request Scope, permission does not work anymore
        {
            if (AppInfo.getAppSecDirectory() == null && !AppSettings.getBoolOption(getApplication(), "scoped_first_show_done", false))
            {
                new ScopedStorageFirstTimeDialog(this);
                AppSettings.setBoolOption(getApplication(), "scoped_first_show_done", true);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        log.log(DebugLog.Level.D, "onActivityResult, requestCode = " + requestCode + " resultCode = " + resultCode);

        ScopedStorage.activityResult(this, requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        switch (requestCode)
        {
            case REQUEST_EXTERNAL_STROAGE:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    log.log(DebugLog.Level.D, "Permission granted");

                    if (AppInfo.isScopedEnabled())
                        new ScopedStorageFirstTimeDialog(this);
                }
                else
                {
                    log.log(DebugLog.Level.D, "Permission denied");

                    final Activity act = this;

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("You must grant read/write permission to the internal storage!").setCancelable(false).setPositiveButton("OK",
                                                                                                                                               (dialog, id) -> ActivityCompat.requestPermissions(
                                                                                                                                                       act,
                                                                                                                                                       new String[]{
                                                                                                                                                               Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                                                                                                                       REQUEST_EXTERNAL_STROAGE));

                    builder.show();
                }
            }
        }
    }

    @Override
    public void onBackPressed()
    {
        if (mainFragment.onBackPressed())
        {
            // Fragment ate back button press
            return;
        }
        else
        {
            super.onBackPressed();
        }

        super.onBackPressed();
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event)
    {
        return mainFragment.onGenericMotionEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (mainFragment.onKeyDown(keyCode, event))
        {
            return true;
        }
        else
        {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }


}
