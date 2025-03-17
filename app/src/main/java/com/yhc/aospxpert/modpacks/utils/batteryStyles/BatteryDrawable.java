package com.yhc.aospxpert.modpacks.utils.batteryStyles;

import android.graphics.drawable.Drawable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.yhc.aospxpert.modpacks.systemui.BatteryDataProvider;

public abstract class BatteryDrawable extends Drawable {

	public abstract void setShowPercent(boolean showPercent);

	public abstract void setColors(int fgColor, int bgColor, int singleToneColor);

	public abstract void setChargingAnimationEnabled(boolean enabled);
	public abstract void onColorsUpdated();
	public static boolean colorful;
	public static List<Float> batteryLevels = new ArrayList<>();
	public static int[] batteryColors;
	public static boolean showCharging = false;
	public static boolean showFastCharging = false;
	public static int chargingColor = 0;
	public static int fastChargingColor = 0;
	public static boolean transitColors = false;
	public static List<WeakReference<BatteryDrawable>> instances = new ArrayList<>();

	public static void setStaticColor(List<Float> batteryLevels, int[] batteryColors, boolean indicateCharging, int chargingColor, boolean indicateFastCharging, int fastChargingColor, boolean transitColors, boolean colorful) {
		BatteryDrawable.batteryColors = batteryColors;
		BatteryDrawable.batteryLevels = batteryLevels;
		BatteryDrawable.showCharging = indicateCharging;
		BatteryDrawable.showFastCharging = indicateFastCharging;
		BatteryDrawable.chargingColor = chargingColor;
		BatteryDrawable.fastChargingColor = fastChargingColor;
		BatteryDrawable.transitColors = transitColors;
		BatteryDrawable.colorful = colorful;

		cleanupColorCallbacks();
		instances.forEach(callback -> {
			if(callback.get() != null)
			{
				callback.get().onColorsUpdated();
			}
		});
	}

	public BatteryDrawable()
	{
		BatteryDataProvider.registerInfoCallback(this::invalidateSelf);
		instances.add(new WeakReference<>(this));
		onColorsUpdated();
	}

	private static void cleanupColorCallbacks()
	{
		final List<WeakReference<BatteryDrawable>> cleanInstances = new ArrayList<>();
		instances.forEach(instance -> {
			if(instance.get() != null)
			{
				cleanInstances.add(instance);
			}
		});
		instances = cleanInstances;
	}
}
