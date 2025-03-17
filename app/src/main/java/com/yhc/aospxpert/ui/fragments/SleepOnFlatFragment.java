package com.yhc.aospxpert.ui.fragments;

import com.yhc.aospxpert.R;
import com.yhc.aospxpert.utils.ControlledPreferenceFragmentCompat;

public class SleepOnFlatFragment extends ControlledPreferenceFragmentCompat {

    @Override
    public String getTitle() {
        return getString(R.string.sleep_on_flat_screen_tile_title);
    }

    @Override
    public int getLayoutResource() {
        return R.xml.sleep_on_flat_prefs;
    }

    @Override
    protected int getDefaultThemeResource() {
        return R.style.PrefsThemeCollapsingToolbar;
    }

}
