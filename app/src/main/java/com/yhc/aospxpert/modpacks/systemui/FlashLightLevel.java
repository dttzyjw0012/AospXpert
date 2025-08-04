package com.yhc.aospxpert.modpacks.systemui;

import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;
import static com.yhc.aospxpert.modpacks.utils.SystemUtils.getFlashlightLevel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;

import com.yhc.aospxpert.modpacks.utils.SystemUtils;
import com.yhc.aospxpert.modpacks.utils.slidingtile.SlidingTile;

public class FlashLightLevel extends SlidingTile {
	private static boolean leveledFlashTile = false;
	private static boolean AnimateFlashlight = false;


	public FlashLightLevel(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key)
	{
		super.updatePrefs(Key);
		leveledFlashTile = Xprefs.getBoolean("leveledFlashTile", false);
		AnimateFlashlight = Xprefs.getBoolean("AnimateFlashlight", false);

		if(Key.length > 0 && Key[0].equals("leveledFlashTile"))
		{
			refreshAllTiles();
		}
	}

	@SuppressLint("DiscouragedApi")
	@Override
	public String getTextForLevel(int level) {
		Resources res = mContext.getResources();
		return String.format("%s - %s%%",
				res.getText(
						res.getIdentifier(
								"quick_settings_flashlight_label",
								"string", mContext.getPackageName())),
				level
		);
	}

	@Override
	public boolean shallControlTiles() {
		return leveledFlashTile && SystemUtils.supportsFlashLevels();
	}

	@Override
	public void init() {}

	@Override
	public boolean isStateControlledByMod() {
		return false;
	}

	@Override
	public int getInitialValue() {
		try
		{
			return Xprefs.getInt("flashPCT", 50);
		}
		catch (Throwable ignored){}
		return 50;
	}

	@Override
	public int getInitialState() {
		return STATE_NO_CHANGE;
	}

	@Override
	public int clampToLevelSteps(int value) {
		return Math.round(100f * getFlashlightLevel(value/100f) / SystemUtils.getMaxFlashLevel());
	}

	@Override
	public void saveCurrentState(int currentState, int currentValue) {
		Xprefs.edit().putInt("flashPCT", currentValue).apply();
	}

	@Override
	public void handleClick(int currentValue) {
		SystemUtils.setFlash(!SystemUtils.isFlashOn(), getFlashlightLevel(currentValue/100f), AnimateFlashlight);
	}

	@Override
	public int handleValueChange(int newValue) {
		SystemUtils.setFlash(SystemUtils.isFlashOn(), getFlashlightLevel(newValue/100f), false);
		return STATE_NO_CHANGE;
	}

	@Override
	public String getTargetSpec() {
		return "flashlight";
	}
}