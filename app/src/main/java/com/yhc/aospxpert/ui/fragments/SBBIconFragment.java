package com.yhc.aospxpert.ui.fragments;

import com.yhc.aospxpert.R;
import com.yhc.aospxpert.utils.ControlledPreferenceFragmentCompat;

public class SBBIconFragment extends ControlledPreferenceFragmentCompat {
	@Override
	public String getTitle() {
		return getString(R.string.sbbIcon_header);
	}

	@Override
	public int getLayoutResource() {
		return R.xml.statusbar_batteryicon_prefs;
	}
}
