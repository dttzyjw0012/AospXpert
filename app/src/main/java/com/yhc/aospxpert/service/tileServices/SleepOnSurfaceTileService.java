package com.yhc.aospxpert.service.tileServices;

import static android.service.quicksettings.Tile.STATE_ACTIVE;
import static android.service.quicksettings.Tile.STATE_INACTIVE;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.yhc.aospxpert.R;
import com.yhc.aospxpert.utils.PXPreferences;

public class SleepOnSurfaceTileService extends TileService {
	private boolean mLastEnabled = false;

	@Override
	public void onStartListening() {
		super.onStartListening();

		PXPreferences.getPrefs().registerOnSharedPreferenceChangeListener((sharedPreferences, key) ->
				setTile(PXPreferences.getBoolean("SleepOnFlatScreen", false)));

		setTile(PXPreferences.getBoolean("SleepOnFlatScreen", false));
	}

	@Override
	public void onClick() {
		setTile(!mLastEnabled); //Isn't mandatory, but without it tile click will take time to reflect on UI

		new Thread(() -> PXPreferences.putBoolean("SleepOnFlatScreen", mLastEnabled)).start(); //otherwise click will be blocked until pref is saved
	}

	private void setTile(boolean enabled) {
		if(mLastEnabled == enabled) return;

		mLastEnabled = enabled;

		Tile thisTile = getQsTile();

		thisTile.setIcon(Icon.createWithResource(getApplicationContext(), enabled
				? R.drawable.ic_sleep_surface_on
				: R.drawable.ic_sleep_surface_off));

		thisTile.setState(enabled
				? STATE_ACTIVE
				: STATE_INACTIVE);

		thisTile.setSubtitle(getString(enabled
				? R.string.general_on
				: R.string.general_off));

		thisTile.setLabel(getString(R.string.sleep_on_flat_screen_tile_title));

		thisTile.updateTile();
	}
}
