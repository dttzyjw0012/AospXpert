package com.yhc.aospxpert.ui.fragments;

import com.yhc.aospxpert.R;
import com.yhc.aospxpert.utils.ControlledPreferenceFragmentCompat;

public class SBCFragment extends ControlledPreferenceFragmentCompat {
	@Override
	public String getTitle() {
		return getString(R.string.sbc_header);
	}

	@Override
	public int getLayoutResource() {
		return R.xml.statusbar_clock_prefs;
	}
}
