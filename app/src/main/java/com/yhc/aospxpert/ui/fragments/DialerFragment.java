package com.yhc.aospxpert.ui.fragments;

import com.yhc.aospxpert.R;
import com.yhc.aospxpert.utils.ControlledPreferenceFragmentCompat;

public class DialerFragment extends ControlledPreferenceFragmentCompat {
	@Override
	public String getTitle() {
		return getString(R.string.dialer_header);
	}

	@Override
	public int getLayoutResource() {
		return R.xml.dialer_prefs;
	}
}
