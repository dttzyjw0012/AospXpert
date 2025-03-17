package com.yhc.aospxpert.modpacks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.yhc.aospxpert.BuildConfig;
import com.yhc.aospxpert.modpacks.utils.ExtendedRemotePreferences;
import com.yhc.aospxpert.modpacks.utils.Overlays;


public class XPrefs {

	@SuppressLint("StaticFieldLeak")
	public static ExtendedRemotePreferences Xprefs;
	public static final String MagiskRoot = "/data/adb/modules/AospXpert";
	private static String packageName;

	private static final OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> loadEverything(packageName, key);
	public static void init(Context context) {
		packageName = context.getPackageName();

		Xprefs = (ExtendedRemotePreferences) new ExtendedRemotePreferences(context, BuildConfig.APPLICATION_ID, BuildConfig.APPLICATION_ID + "_preferences", true);

		Xprefs.registerOnSharedPreferenceChangeListener(listener);
	}


	public static void loadEverything(String packageName, String... key) {
		if (key.length > 0 && (key[0] == null || Constants.PREF_UPDATE_EXCLUSIONS.stream().anyMatch(exclusion -> key[0].startsWith(exclusion))))
			return;

		setPackagePrefs(packageName);

		for (XposedModPack thisMod : XPLauncher.runningMods) {
			thisMod.updatePrefs(key);
		}
	}

	public static void setPackagePrefs(String packageName) {
		if (Constants.SYSTEM_UI_PACKAGE.equals(packageName) && !XPLauncher.isChildProcess) {
			Overlays.setAll(false);
		}
	}
}
