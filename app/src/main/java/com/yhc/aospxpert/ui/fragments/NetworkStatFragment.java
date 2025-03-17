package com.yhc.aospxpert.ui.fragments;

import com.yhc.aospxpert.R;
import com.yhc.aospxpert.utils.ControlledPreferenceFragmentCompat;

public class NetworkStatFragment extends ControlledPreferenceFragmentCompat {
	@Override
	public String getTitle() {
		return getString(R.string.netstat_header);
	}

	@Override
	public int getLayoutResource() {
		return R.xml.lsqs_custom_text;
	}
}
