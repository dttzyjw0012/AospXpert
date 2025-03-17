package com.yhc.aospxpert.ui.fragments;

import com.yhc.aospxpert.R;
import com.yhc.aospxpert.utils.ControlledPreferenceFragmentCompat;

public class ThreeButtonNavFragment extends ControlledPreferenceFragmentCompat {
	@Override
	public String getTitle() {
		return getString(R.string.threebutton_header_title);
	}

	@Override
	public int getLayoutResource() {
		return R.xml.three_button_prefs;
	}
}
