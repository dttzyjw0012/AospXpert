package com.yhc.aospxpert.utils;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.yhc.aospxpert.ui.preferences.preferencesearch.SearchPreferenceResult.highlightPreference;

import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

import com.yhc.aospxpert.R;

public abstract class ControlledPreferenceFragmentCompat extends PreferenceFragmentCompat {

	public ExtendedSharedPreferences mPreferences;
	private final OnSharedPreferenceChangeListener changeListener = (sharedPreferences, key) -> updateScreen(key);
	public NavController navController;

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

	@SuppressWarnings("deprecation")
	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.main_menu, menu);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(!Objects.equals(getTitle(), getString(R.string.app_name)));
		navController = NavHostFragment.findNavController(this);
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

		AppCompatActivity baseContext = (AppCompatActivity) getContext();
		Toolbar toolbar = view.findViewById(R.id.toolbar);

		if (baseContext != null) {
			if (toolbar != null) {
				baseContext.setSupportActionBar(toolbar);
				toolbar.setTitle(getTitle());
			}
			if (baseContext.getSupportActionBar() != null) {
				baseContext.getSupportActionBar().setDisplayHomeAsUpEnabled(getBackButtonEnabled());
			}
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
