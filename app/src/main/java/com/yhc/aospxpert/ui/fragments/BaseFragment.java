package com.yhc.aospxpert.ui.fragments;

import static com.yhc.aospxpert.utils.MiscUtils.setOnBackPressedDispatcherCallback;
import static com.yhc.aospxpert.utils.MiscUtils.setupToolbar;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.yhc.aospxpert.utils.DisplayUtils;

public abstract class BaseFragment extends Fragment {

	private final boolean isTabletDevice = DisplayUtils.isTablet();

	protected boolean isBackButtonEnabled() {
		return true;
	}

	public boolean getBackButtonEnabled() {
		return isBackButtonEnabled();
	}

	public abstract String getTitle();

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		setupToolbar(this, view, getTitle(), getBackButtonEnabled());

		setOnBackPressedDispatcherCallback(requireActivity(), this);
	}
}