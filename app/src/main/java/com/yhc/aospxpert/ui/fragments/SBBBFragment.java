package com.yhc.aospxpert.ui.fragments;

import com.yhc.aospxpert.R;
import com.yhc.aospxpert.utils.ControlledPreferenceFragmentCompat;

public class SBBBFragment extends ControlledPreferenceFragmentCompat {

	@Override
	public String getTitle() {
		return getString(R.string.sbbb_header_title);
	}

	@Override
	public int getLayoutResource() {
		return R.xml.statusbar_batterybar_prefs;
	}
}
