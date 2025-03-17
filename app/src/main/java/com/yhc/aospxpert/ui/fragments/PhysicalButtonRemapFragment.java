package com.yhc.aospxpert.ui.fragments;

import com.yhc.aospxpert.R;
import com.yhc.aospxpert.utils.ControlledPreferenceFragmentCompat;

public class PhysicalButtonRemapFragment extends ControlledPreferenceFragmentCompat {
	@Override
	public String getTitle() {
		return getString(R.string.remap_physical_buttons_title);
	}

	@Override
	public int getLayoutResource() {
		return R.xml.physical_buttons_prefs;
	}
}
