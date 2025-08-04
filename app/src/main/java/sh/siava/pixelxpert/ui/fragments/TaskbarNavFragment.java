package com.yhc.aospxpert.ui.fragments;

import com.yhc.aospxpert.R;
import com.yhc.aospxpert.utils.ControlledPreferenceFragmentCompat;

public class TaskbarNavFragment extends ControlledPreferenceFragmentCompat {
	@Override
	public String getTitle() {
		return getString(R.string.taskbar_header_title);
	}

	@Override
	public int getLayoutResource() {
		return R.xml.taskbar_prefs;
	}
}
