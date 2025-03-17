package com.yhc.aospxpert.ui.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.yhc.aospxpert.R;
import com.yhc.aospxpert.utils.ControlledPreferenceFragmentCompat;
import com.yhc.aospxpert.utils.PreferenceHelper;

public class QuickSettingsFragment extends ControlledPreferenceFragmentCompat {
	private FrameLayout pullDownIndicator;

	@Override
	public String getTitle() {
		return getString(R.string.qs_panel_category_title);
	}

	@Override
	public int getLayoutResource() {
		return R.xml.quicksettings_prefs;
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		super.onCreatePreferences(savedInstanceState, rootKey);

		createPullDownIndicator();
	}

	@Override
	public void onDestroy() {
		((ViewGroup) pullDownIndicator.getParent()).removeView(pullDownIndicator);
		super.onDestroy();
	}

	@SuppressLint("RtlHardcoded")
	@Override
	public void updateScreen(String key) {
		super.updateScreen(key);
		try {
			int displayWidth = getActivity().getWindowManager().getCurrentWindowMetrics().getBounds().width();

			pullDownIndicator.setVisibility(PreferenceHelper.isVisible("QSPulldownPercent") ? View.VISIBLE : View.GONE);
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) pullDownIndicator.getLayoutParams();
			lp.width = Math.round(mPreferences.getSliderInt("QSPulldownPercent", 25) * displayWidth / 100f);
			lp.gravity = Gravity.TOP | (Integer.parseInt(mPreferences.getString("QSPulldownSide", "1")) == 1 ? Gravity.RIGHT : Gravity.LEFT);
			pullDownIndicator.setLayoutParams(lp);
		} catch (Exception ignored) {
		}
	}

	private void createPullDownIndicator() {
		pullDownIndicator = new FrameLayout(getContext());
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(0, 25);
		lp.gravity = Gravity.TOP;

		pullDownIndicator.setLayoutParams(lp);
		pullDownIndicator.setBackgroundColor(getContext().getColor(android.R.color.system_accent1_200));
		pullDownIndicator.setAlpha(.7f);
		pullDownIndicator.setVisibility(View.VISIBLE);

		((ViewGroup) getActivity().getWindow().getDecorView().getRootView()).addView(pullDownIndicator);
	}
}
