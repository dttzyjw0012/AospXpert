package com.yhc.aospxpert.ui.fragments;

import com.yhc.aospxpert.R;
import com.yhc.aospxpert.utils.ControlledPreferenceFragmentCompat;

public class QSTileQtyFragment extends ControlledPreferenceFragmentCompat {
	@Override
	public String getTitle() {
		return getString(R.string.qs_tile_qty_title);
	}

	@Override
	public int getLayoutResource() {
		return R.xml.qs_tile_qty_prefs;
	}
}
