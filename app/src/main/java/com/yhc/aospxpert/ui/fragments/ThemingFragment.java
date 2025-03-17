package com.yhc.aospxpert.ui.fragments;

import com.yhc.aospxpert.R;
import com.yhc.aospxpert.utils.ControlledPreferenceFragmentCompat;

public class ThemingFragment extends ControlledPreferenceFragmentCompat {
	@Override
	public String getTitle() {
		return getString(R.string.theme_customization_category);
	}

	@Override
	public int getLayoutResource() {
		return R.xml.theming_prefs;
	}
}
