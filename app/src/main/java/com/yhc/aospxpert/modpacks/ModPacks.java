package com.yhc.aospxpert.modpacks;

import static com.yhc.aospxpert.modpacks.utils.toolkit.OverlayTools.setOverlay;

import android.os.Build;

import java.util.ArrayList;

import com.yhc.aospxpert.modpacks.allApps.HookTester;
import com.yhc.aospxpert.modpacks.allApps.OverScrollDisabler;
import com.yhc.aospxpert.modpacks.android.BrightnessRange;
import com.yhc.aospxpert.modpacks.android.FaceUpScreenSleep;
import com.yhc.aospxpert.modpacks.android.HotSpotController;
import com.yhc.aospxpert.modpacks.android.PackageManager;
import com.yhc.aospxpert.modpacks.android.PhoneWindowManager;
import com.yhc.aospxpert.modpacks.android.RingerVolSeparator;
import com.yhc.aospxpert.modpacks.android.ScreenOffKeys;
import com.yhc.aospxpert.modpacks.android.ScreenRotation;
import com.yhc.aospxpert.modpacks.android.StatusbarSize;
import com.yhc.aospxpert.modpacks.android.SystemScreenRecord;
import com.yhc.aospxpert.modpacks.dialer.RecordingMessage;
import com.yhc.aospxpert.modpacks.ksu.KSUInjector;
import com.yhc.aospxpert.modpacks.launcher.ClearAllButtonMod;
import com.yhc.aospxpert.modpacks.launcher.CustomNavGestures;
import com.yhc.aospxpert.modpacks.launcher.FeatureFlags;
import com.yhc.aospxpert.modpacks.launcher.LauncherGestureNavbarManager;
import com.yhc.aospxpert.modpacks.launcher.AospXpertIconUpdater;
import com.yhc.aospxpert.modpacks.launcher.TaskbarActivator;
import com.yhc.aospxpert.modpacks.settings.AppCloneEnabler;
import com.yhc.aospxpert.modpacks.settings.PXSettingsLauncher;
import com.yhc.aospxpert.modpacks.systemui.BatteryDataProvider;
import com.yhc.aospxpert.modpacks.systemui.BatteryStyleManager;
import com.yhc.aospxpert.modpacks.systemui.BrightnessSlider;
import com.yhc.aospxpert.modpacks.systemui.DepthWallpaper;
import com.yhc.aospxpert.modpacks.systemui.EasyUnlock;
import com.yhc.aospxpert.modpacks.systemui.FeatureFlagsMods;
import com.yhc.aospxpert.modpacks.systemui.FingerprintWhileDozing;
import com.yhc.aospxpert.modpacks.systemui.FlashLightLevel;
import com.yhc.aospxpert.modpacks.systemui.GestureNavbarManager;
import com.yhc.aospxpert.modpacks.systemui.IconPacks;
import com.yhc.aospxpert.modpacks.systemui.KSURootReceiver;
import com.yhc.aospxpert.modpacks.systemui.KeyGuardPinScrambler;
import com.yhc.aospxpert.modpacks.systemui.KeyguardMods;
import com.yhc.aospxpert.modpacks.systemui.MultiStatusbarRows;
import com.yhc.aospxpert.modpacks.systemui.NotificationExpander;
import com.yhc.aospxpert.modpacks.systemui.NotificationManager;
import com.yhc.aospxpert.modpacks.systemui.PowerMenu;
import com.yhc.aospxpert.modpacks.systemui.QSFooterManager;
import com.yhc.aospxpert.modpacks.systemui.QSTileGrid;
import com.yhc.aospxpert.modpacks.systemui.ScreenGestures;
import com.yhc.aospxpert.modpacks.systemui.ScreenRecord;
import com.yhc.aospxpert.modpacks.systemui.ScreenshotManager;
import com.yhc.aospxpert.modpacks.systemui.StatusIconTuner;
import com.yhc.aospxpert.modpacks.systemui.StatusbarGestures;
import com.yhc.aospxpert.modpacks.systemui.StatusbarMods;
import com.yhc.aospxpert.modpacks.systemui.ThemeManager_13;
import com.yhc.aospxpert.modpacks.systemui.ThemeManager_14;
import com.yhc.aospxpert.modpacks.systemui.ThermalProvider;
import com.yhc.aospxpert.modpacks.systemui.ThreeButtonNavMods;
import com.yhc.aospxpert.modpacks.systemui.UDFPSManager;
import com.yhc.aospxpert.modpacks.systemui.VolumeDialog;
import com.yhc.aospxpert.modpacks.systemui.VolumeTile;
import com.yhc.aospxpert.modpacks.telecom.CallVibrator;


public class ModPacks {

	public static ArrayList<Class<? extends XposedModPack>> getMods(String packageName)
	{
		ArrayList<Class<? extends XposedModPack>> modPacks = new ArrayList<>();

		//all packages
		modPacks.add(HookTester.class);

		switch (packageName)
		{
			case Constants.SYSTEM_FRAMEWORK_PACKAGE:
				modPacks.add(StatusbarSize.class);
				modPacks.add(PackageManager.class);
				modPacks.add(BrightnessRange.class);
				modPacks.add(PhoneWindowManager.class);
				modPacks.add(ScreenRotation.class);
				modPacks.add(ScreenOffKeys.class);
				modPacks.add(HotSpotController.class);
				modPacks.add(RingerVolSeparator.class);
				modPacks.add(SystemScreenRecord.class);
				modPacks.add(FaceUpScreenSleep.class);
				break;

			case Constants.SYSTEM_UI_PACKAGE:
				if(XPLauncher.isChildProcess && XPLauncher.processName.contains("screenshot"))
				{
					modPacks.add(ScreenshotManager.class);
				}
				else
				{
					//load before others
					modPacks.add(ThermalProvider.class);

					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
					{
						setOverlay("QSLightThemeOverlay", false, true, true);
						modPacks.add(ThemeManager_14.class);
					}
					else
					{
						modPacks.add(ThemeManager_13.class);
					}

					modPacks.add(BatteryDataProvider.class);
					modPacks.add(IconPacks.class);
					modPacks.add(BrightnessRange.class);
					modPacks.add(NotificationExpander.class);
					modPacks.add(QSTileGrid.class);
					modPacks.add(BrightnessSlider.class);
					modPacks.add(FeatureFlagsMods.class);
					modPacks.add(ThreeButtonNavMods.class);
					modPacks.add(ScreenGestures.class);
					modPacks.add(MiscSettings.class);
					modPacks.add(StatusbarGestures.class);
					modPacks.add(KeyguardMods.class);
					modPacks.add(UDFPSManager.class);
					modPacks.add(EasyUnlock.class);
					modPacks.add(MultiStatusbarRows.class);
					modPacks.add(StatusbarMods.class);
					modPacks.add(BatteryStyleManager.class);
					modPacks.add(GestureNavbarManager.class);
					modPacks.add(QSFooterManager.class);
					modPacks.add(KeyGuardPinScrambler.class);
					modPacks.add(FingerprintWhileDozing.class);
					modPacks.add(StatusbarSize.class);
					modPacks.add(FlashLightLevel.class);
					modPacks.add(NotificationManager.class);
					modPacks.add(VolumeTile.class);
					modPacks.add(ScreenRecord.class);
					modPacks.add(VolumeDialog.class);
					modPacks.add(DepthWallpaper.class);
					modPacks.add(KSURootReceiver.class);
					modPacks.add(PowerMenu.class);
					modPacks.add(StatusIconTuner.class);
				}
				break;

			case Constants.LAUNCHER_PACKAGE:
				modPacks.add(LauncherGestureNavbarManager.class);
				modPacks.add(TaskbarActivator.class);
				modPacks.add(CustomNavGestures.class);
				modPacks.add(ClearAllButtonMod.class);
				modPacks.add(AospXpertIconUpdater.class);
				modPacks.add(FeatureFlags.class);
				break;

			case Constants.TELECOM_SERVER_PACKAGE:
				modPacks.add(CallVibrator.class);
				break;

			case Constants.SETTINGS_PACKAGE:
				modPacks.add(PXSettingsLauncher.class);
				modPacks.add(IconPacks.class);

				if(Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU)
					modPacks.add(AppCloneEnabler.class);
				break;

			case Constants.DIALER_PACKAGE:
				modPacks.add(RecordingMessage.class);
				break;

			case Constants.KSU_PACKAGE:
			case Constants.KSU_NEXT_PACKAGE:
				modPacks.add(KSUInjector.class);
				break;
		}

		//All Apps
		modPacks.add(OverScrollDisabler.class);

		return modPacks;
	}
}