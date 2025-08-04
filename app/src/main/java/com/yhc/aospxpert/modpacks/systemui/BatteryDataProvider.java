package com.yhc.aospxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedHelpers.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.ResourceManager;
import com.yhc.aospxpert.modpacks.XPLauncher;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;

/**
 * @noinspection RedundantThrows
 */
public class BatteryDataProvider extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	public static final int CHARGING_UNKNOWN = -1;
	public static final int CHARGING_SLOWLY = 0;
	public static final int CHARGING_REGULAR = 1;
	public static final int CHARGING_FAST = 2;

	public static final int BATTERY_STATUS_DISCHARGING = 3;

	@SuppressLint("StaticFieldLeak")
	private static BatteryDataProvider instance = null;

	List<BatteryStatusCallback> mStatusCallbacks = new ArrayList<>();
	private boolean mCharging;
	private int mCurrentLevel = 0;


	private final ArrayList<BatteryInfoCallback> mInfoCallbacks = new ArrayList<>();
	private boolean mPowerSave = false;
	private boolean mIsFastCharging = false;
	private boolean mIsBatteryDefender = false;
	private int mMaxChargingWattage = 0;


	public BatteryDataProvider(Context context) {
		super(context);
		instance = this;
	}

	@Override
	public void updatePrefs(String... Key) {
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		XC_InitPackageResources.InitPackageResourcesParam resparam = ResourceManager.resparams.get(lpParam.packageName);
		ReflectedClass BatteryStatusClass = ReflectedClass.of("com.android.settingslib.fuelgauge.BatteryStatus");
		ReflectedClass BatteryControllerImplClass = ReflectedClass.of("com.android.systemui.statusbar.policy.BatteryControllerImpl");

		final int slowThreshold = resparam.res.getIdentifier(
				"config_chargingSlowlyThreshold", "values", "com.android.settingslib");
		final int fastThreshold = resparam.res.getIdentifier(
				"config_chargingFastThreshold", "values", "com.android.settingslib");

		//once an intent is received, it's either battery level change, powersave change, or demo mode. we don't expect demo
		// intents normally. So it's safe to assume we'll need to update battery anyway
		BatteryControllerImplClass
				.after("onReceive")
				.run(param -> {
					mCurrentLevel = getIntField(param.thisObject, "mLevel");
					mCharging = getBooleanField(param.thisObject, "mPluggedIn")
							|| getBooleanField(param.thisObject, "mCharging")
							|| getBooleanField(param.thisObject, "mWirelessCharging");
					mPowerSave = getBooleanField(param.thisObject, "mPowerSave");

					try {
						mIsBatteryDefender = getBooleanField(param.thisObject, "mIsBatteryDefender");
					}
					catch (Throwable ignored) { //older versions of Android don't have defender
						mIsBatteryDefender = false;
					}

					fireBatteryInfoChanged();
				});

		BatteryStatusClass
				.afterConstruction()
				.run(param -> {
					mMaxChargingWattage = (int) getObjectField(param.thisObject, "maxChargingWattage");
					mIsFastCharging = (mMaxChargingWattage <= 0 ? CHARGING_UNKNOWN :
							mMaxChargingWattage < slowThreshold ? CHARGING_SLOWLY :
							mMaxChargingWattage > fastThreshold ? CHARGING_FAST :
									CHARGING_REGULAR) == (CHARGING_FAST);

					if (param.args.length > 0 && (param.args[0] instanceof Intent)) {
						onBatteryStatusChanged((int) getObjectField(param.thisObject, "status"), (Intent) param.args[0]);
					}
				});
	}

	private void onBatteryStatusChanged(int status, Intent intent) {
		for (BatteryStatusCallback callback : mStatusCallbacks) {
			try {
				callback.onBatteryStatusChanged(status, intent);
			} catch (Throwable ignored) {
			}
		}
	}

	public static void registerStatusCallback(BatteryStatusCallback callback) {
		try {
			instance.mStatusCallbacks.add(callback);
		} catch (Throwable ignored) {
		}
	}

	/**
	 * @noinspection unused
	 */
	public static void unRegisterStatusCallback(BatteryStatusCallback callback) {
		try {
			instance.mStatusCallbacks.remove(callback);
		} catch (Throwable ignored) {
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	public static void registerInfoCallback(BatteryInfoCallback callback) {
		try {
			instance.mInfoCallbacks.add(callback);
		} catch (Throwable ignored) {
		}
	}

	/**
	 * @noinspection unused
	 */
	public static void unRegisterInfoCallback(BatteryInfoCallback callback) {
		try {
			instance.mInfoCallbacks.remove(callback);
		} catch (Throwable ignored) {
		}
	}

	public static boolean isCharging() {
		try {
			return instance.mCharging && !instance.mIsBatteryDefender;
		} catch (Throwable ignored) {
			return false;
		}
	}

	public static boolean isBatteryDefender()
	{
		try {
			return instance.mIsBatteryDefender;
		} catch (Throwable ignored) {
			return false;
		}
	}

	public static int getCurrentLevel() {
		try {
			return instance.mCurrentLevel;
		} catch (Throwable ignored) {
			return 0;
		}
	}

	public static boolean isPowerSaving() {
		try {
			return instance.mPowerSave;
		} catch (Throwable ignored) {
			return false;
		}
	}

	public static boolean isFastCharging() {
		try {
			return instance.mCharging && instance.mIsFastCharging;
		} catch (Throwable ignored) {
			return false;
		}
	}

	private void fireBatteryInfoChanged() {
		for (BatteryInfoCallback callback : mInfoCallbacks) {
			try {
				callback.onBatteryInfoChanged();
			} catch (Throwable ignored) {
			}
		}
	}

	public interface BatteryInfoCallback {
		void onBatteryInfoChanged();
	}


	public interface BatteryStatusCallback {
		void onBatteryStatusChanged(int batteryStatus, Intent batteryStatusIntent);
	}
}

