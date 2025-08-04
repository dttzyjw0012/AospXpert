package com.yhc.aospxpert.utils;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.yhc.aospxpert.ui.preferences.preferencesearch.SearchPreferenceResult.highlightPreference;
import static com.yhc.aospxpert.utils.MiscUtils.setOnBackPressedDispatcherCallback;
import static com.yhc.aospxpert.utils.MiscUtils.setupToolbar;

import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;

import java.util.Objects;

import com.yhc.aospxpert.R;

public abstract class ControlledPreferenceFragmentCompat extends PreferenceFragmentCompat {

	public ExtendedSharedPreferences mPreferences;
	private final OnSharedPreferenceChangeListener changeListener = (sharedPreferences, key) -> updateScreen(key);
	private static boolean firstAppLaunch = true;

	protected boolean isBackButtonEnabled() {
		return true;
	}

	public boolean getBackButtonEnabled() {
		return isBackButtonEnabled();
	}

	public abstract String getTitle();

	public abstract int getLayoutResource();

	protected int getDefaultThemeResource() {
		return R.style.PrefsThemeCollapsingToolbar;
	}

	public int getThemeResource() {
		return getDefaultThemeResource();
	}

	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		inflater.getContext().setTheme(getThemeResource());
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		AppCompatActivity baseContext = (AppCompatActivity) getActivity();

		setupToolbar(this, view, getTitle(), getBackButtonEnabled());

		setOnBackPressedDispatcherCallback(requireActivity(), this);

		if (baseContext != null) {
			AppBarLayout appBarLayout = baseContext.findViewById(R.id.appBarLayout);
			if (appBarLayout != null && Objects.equals(getTitle(), getString(R.string.app_name)) && firstAppLaunch) {
				appBarLayout.setExpanded(true, false);
				firstAppLaunch = false;
			}
		}

		RecyclerView recyclerView = view.findViewById(androidx.preference.R.id.recycler_view);

		if (recyclerView != null) {
			ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
				Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());
				boolean isRtl = view.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;

				if (insets.left > 0 || insets.right > 0) {
					int endInset = isRtl ? insets.left : insets.right;

					ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) recyclerView.getLayoutParams();
					if (isRtl) params.leftMargin = endInset;
					else params.rightMargin = endInset;
					recyclerView.setLayoutParams(params);
				}

				return windowInsets;
			});
		}

		if (getArguments() != null) {
			Bundle bundle = getArguments();
			if (bundle.containsKey("searchKey")) {
				highlightPreference(this, view, bundle.getString("searchKey"));
			}
		}
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		getPreferenceManager().setStorageDeviceProtected();
		setPreferencesFromResource(getLayoutResource(), rootKey);
	}

	@NonNull
	@Override
	public RecyclerView.Adapter<?> onCreateAdapter(@NonNull PreferenceScreen preferenceScreen) {
		mPreferences = ExtendedSharedPreferences.from(getDefaultSharedPreferences(requireContext().createDeviceProtectedStorageContext()));

		mPreferences.registerOnSharedPreferenceChangeListener(changeListener);

		updateScreen(null);

		return super.onCreateAdapter(preferenceScreen);
	}

	@Override
	public void onDestroy() {
		if (mPreferences != null) {
			mPreferences.unregisterOnSharedPreferenceChangeListener(changeListener);
		}
		super.onDestroy();
	}

	public void updateScreen(String key) {
		PreferenceHelper.setupAllPreferences(this.getPreferenceScreen());
	}

	@Override
	public void onResume() {
		super.onResume();
		PreferenceHelper.setupMainSwitches(this.getPreferenceScreen());
	}
}
