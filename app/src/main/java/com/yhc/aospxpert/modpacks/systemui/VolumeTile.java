package com.yhc.aospxpert.modpacks.systemui;

import static android.media.AudioManager.STREAM_MUSIC;
import static android.service.quicksettings.Tile.STATE_INACTIVE;
import static java.lang.Math.round;
import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;
import static com.yhc.aospxpert.modpacks.systemui.ThemeManager_13.STATE_ACTIVE;
import static com.yhc.aospxpert.modpacks.utils.SystemUtils.AudioManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;

import com.yhc.aospxpert.BuildConfig;
import com.yhc.aospxpert.modpacks.utils.SystemUtils;
import com.yhc.aospxpert.modpacks.utils.slidingtile.SlidingTile;

/** @noinspection DataFlowIssue*/
public class VolumeTile extends SlidingTile {
	private static int unMuteVolumePCT = 50;
	private static int minVol = -1;
	private static int maxVol = -1;
	private static int currentVolumePercent;

	@Override
	public void updatePrefs(String... Key) {
		super.updatePrefs(Key);
		unMuteVolumePCT = Xprefs.getSliderInt("UnMuteVolumePCT", 50);
	}

	public VolumeTile(Context context) {
		super(context);
	}

	@Override
	public int getInitialValue() {
		return currentVolumePercent;
	}

	private int getCurrentVolumePercent() {
		int currentVol = AudioManager().getStreamVolume(STREAM_MUSIC);

		return round(100f * (currentVol - minVol) / (maxVol - minVol));
	}


	@Override
	public int getInitialState() {
		return currentVolumePercent > 0
				? STATE_ACTIVE
				: STATE_INACTIVE;
	}

	@Override
	public int clampToLevelSteps(int value) {
		return round(round((maxVol - minVol) * value / 100f) * 1f / (maxVol - minVol) * 100f);
	}

	@Override
	public void saveCurrentState(int currentState, int currentValue) {}

	@Override
	public void handleClick(int currentValue) {
		if (currentValue > 0) {
			changeVolume(0);
		} else {
			changeVolume(clampToLevelSteps(unMuteVolumePCT));
		}
	}
	private void changeVolume(int currentPct) {
		AudioManager().setStreamVolume(
				STREAM_MUSIC,
				round((maxVol - minVol) * currentPct / 100f) + minVol,
				0 /* don't show UI */);
	}


	@Override
	public int handleValueChange(int newValue) {
		changeVolume(newValue);
		return newValue > 0
				? STATE_ACTIVE
				: STATE_INACTIVE;
	}

	@Override
	public String getTargetSpec() {
		return String.format("custom(%s/.service.tileServices.VolumeTileService)", BuildConfig.APPLICATION_ID);
	}

	@SuppressLint("DiscouragedApi")
	@Override
	public String getTextForLevel(int currentPct) {
		Resources res = mContext.getResources();
		return String.format("%s - %s%%",
				res.getText(
						res.getIdentifier(
								"media_output_dialog_accessibility_seekbar",
								"string", mContext.getPackageName())),
				currentPct
		);
	}

	@Override
	public boolean shallControlTiles() {
		return true;
	}

	@Override
	public void init() {
		new Thread(() -> {
			minVol = AudioManager().getStreamMinVolume(STREAM_MUSIC);
			maxVol = AudioManager().getStreamMaxVolume(STREAM_MUSIC);

			currentVolumePercent = getCurrentVolumePercent();
		}).start();

		SystemUtils.registerVolumeChangeListener(newVal -> {
			currentVolumePercent = getCurrentVolumePercent();
			notifyValueChanged(currentVolumePercent);
		});
	}

	@Override
	public boolean isStateControlledByMod() {
		return true;
	}
}