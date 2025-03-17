package com.yhc.aospxpert.ui.fragments;

import com.yhc.aospxpert.R;
import com.yhc.aospxpert.utils.ControlledPreferenceFragmentCompat;

public class NetworkFragment extends ControlledPreferenceFragmentCompat {
	@Override
	public String getTitle() {
		return getString(R.string.ntsb_category_title);
	}

	@Override
	public int getLayoutResource() {
		return R.xml.sbqs_network_prefs;
	}
}
