package com.yhc.aospxpert.ui.fragments;

import com.yhc.aospxpert.R;
import com.yhc.aospxpert.utils.ControlledPreferenceFragmentCompat;

public class PackageManagerFragment extends ControlledPreferenceFragmentCompat {
	@Override
	public String getTitle() {
		return getString(R.string.pm_header);
	}

	@Override
	public int getLayoutResource() {
		return R.xml.packagemanger_prefs;
	}
}
