package com.yhc.aospxpert.ui.activities;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.yhc.aospxpert.R.string.update_channel_name;
import static com.yhc.aospxpert.ui.Constants.UPDATES_CHANNEL_ID;
import static com.yhc.aospxpert.utils.AppUtils.isLikelyPixelBuild;
import static com.yhc.aospxpert.utils.MiscUtils.REQUEST_EXPORT;
import static com.yhc.aospxpert.utils.MiscUtils.REQUEST_IMPORT;
import static com.yhc.aospxpert.utils.NavigationExtensionKt.navigateTo;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

import com.yhc.aospxpert.BuildConfig;
import com.yhc.aospxpert.R;
import com.yhc.aospxpert.databinding.SettingsActivityBinding;
import com.yhc.aospxpert.service.tileServices.SleepOnSurfaceTileService;
import com.yhc.aospxpert.ui.fragments.HeaderFragment;
import com.yhc.aospxpert.ui.fragments.UpdateFragment;
import com.yhc.aospxpert.ui.preferences.preferencesearch.SearchPreferenceResult;
import com.yhc.aospxpert.ui.preferences.preferencesearch.SearchPreferenceResultListener;
import com.yhc.aospxpert.utils.AppUtils;
import com.yhc.aospxpert.utils.DisplayUtils;
import com.yhc.aospxpert.utils.ExtendedSharedPreferences;
import com.yhc.aospxpert.utils.PrefManager;
import com.yhc.aospxpert.utils.PreferenceHelper;

public class SettingsActivity extends BaseActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback, SearchPreferenceResultListener {

	private SettingsActivityBinding binding;
	private HeaderFragment headerFragment;
	private NavController navControllerMain;
	private NavController navControllerDetails;
	private final boolean isTabletDevice = DisplayUtils.isTablet();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = SettingsActivityBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		createNotificationChannel();
		setupNavigation(savedInstanceState);

		PreferenceHelper.init(ExtendedSharedPreferences.from(getDefaultSharedPreferences(createDeviceProtectedStorageContext())));

		if (getIntent() != null) {
			if (getIntent().getBooleanExtra("updateTapped", false)) {
				Intent intent = getIntent();
				Bundle bundle = new Bundle();
				bundle.putBoolean("updateTapped", intent.getBooleanExtra("updateTapped", false));
				bundle.putString("filePath", intent.getStringExtra("filePath"));
				UpdateFragment updateFragment = new UpdateFragment();
				updateFragment.setArguments(bundle);
				navigateTo(navControllerMain, R.id.updateFragment, bundle);
			} else if ("true".equals(getIntent().getStringExtra("migratePrefs"))) {
				Intent intent = getIntent();
				Bundle bundle = new Bundle();
				bundle.putString("migratePrefs", intent.getStringExtra("migratePrefs"));
				UpdateFragment updateFragment = new UpdateFragment();
				updateFragment.setArguments(bundle);
				navigateTo(navControllerMain, R.id.updateFragment, bundle);
			} else if (getIntent().getBooleanExtra("newUpdate", false)) {
				navigateTo(navControllerMain, R.id.updateFragment);
			} else if (getIntent().hasExtra(Intent.EXTRA_COMPONENT_NAME)) {
				ComponentName callerComponentName = getIntent().getParcelableExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName.class);
				if(callerComponentName != null) {
					String callerClassName = callerComponentName.getClassName();
					if (SleepOnSurfaceTileService.class.getName().equals(callerClassName)) {
						NavController navController = isTabletDevice ? navControllerDetails : navControllerMain;
						navigateTo(navController, R.id.sleepOnFlatFragment);
					}
				}
			}
		}

		//noinspection ConstantValue
		if (!isLikelyPixelBuild() && !BuildConfig.VERSION_NAME.contains("canary")) {
			new MaterialAlertDialogBuilder(this, R.style.MaterialComponents_MaterialAlertDialog)
					.setTitle(R.string.incompatible_alert_title)
					.setMessage(R.string.incompatible_alert_body)
					.setPositiveButton(R.string.incompatible_alert_ok_btn, (dialog, which) -> dialog.dismiss())
					.show();
		}
	}

	@SuppressLint({"RestrictedApi", "NonConstantResourceId"})
	private void setupNavigation(Bundle savedInstanceState) {
		NavHostFragment navHostFragmentMain = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainerView);
		navControllerMain = Objects.requireNonNull(navHostFragmentMain).getNavController();

		NavHostFragment navHostFragmentDetails = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.detailFragmentContainerView);
		navControllerDetails = Objects.requireNonNull(navHostFragmentDetails).getNavController();

		NavGraph navGraphMain = navControllerMain.getNavInflater().inflate(isTabletDevice ? R.navigation.nav_graph_tablet_main : R.navigation.nav_graph_phone);
		navControllerMain.setGraph(navGraphMain, savedInstanceState);

		binding.detailFragmentContainerView.setVisibility(isTabletDevice ? View.VISIBLE : View.GONE);

		if (isTabletDevice) {
			binding.bottomNavigationView.setVisibility(View.GONE);
			binding.navigationRailView.setVisibility(View.VISIBLE);
			binding.navigationRailView.setOnItemSelectedListener(this::setupOnItemSelectedListener);
			binding.navigationRailView.setOnItemReselectedListener(this::setupOnItemReselectedListener);
			NavigationUI.setupWithNavController(binding.navigationRailView, navControllerMain);
		} else {
			binding.navigationRailView.setVisibility(View.GONE);
			binding.bottomNavigationView.setVisibility(View.VISIBLE);
			binding.bottomNavigationView.setOnItemSelectedListener(this::setupOnItemSelectedListener);
			binding.bottomNavigationView.setOnItemReselectedListener(this::setupOnItemReselectedListener);
			NavigationUI.setupWithNavController(binding.bottomNavigationView, navControllerMain);
		}

		ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, windowInsets) -> {
			Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());
			boolean isRtl = view.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;

			if (insets.left > 0 || insets.right > 0) {
				int startInset = isRtl ? insets.right : insets.left;

				((ViewGroup) binding.navigationRailView.getParent()).setPaddingRelative(
						startInset + binding.navigationRailView.getPaddingStart(),
						0, 0, 0
				);
			}

			return windowInsets;
		});
	}

	private boolean setupOnItemSelectedListener(MenuItem item) {
		if (item.getItemId() == R.id.headerFragment) {
			return navControllerMain.popBackStack(R.id.headerFragment, false);
		} else if (item.getItemId() == R.id.updateFragment) {
			navControllerMain.popBackStack(R.id.headerFragment, false);
			return navigateTo(navControllerMain, R.id.updateFragment);
		} else if (item.getItemId() == R.id.hooksFragment) {
			navControllerMain.popBackStack(R.id.headerFragment, false);
			return navigateTo(navControllerMain, R.id.hooksFragment);
		} else if (item.getItemId() == R.id.ownPrefsFragment) {
			navControllerMain.popBackStack(R.id.headerFragment, false);
			return navigateTo(navControllerMain, R.id.ownPrefsFragment);
		}
		return false;
	}

	private void setupOnItemReselectedListener(MenuItem item) {
		if (item.getItemId() == R.id.headerFragment) {
			navControllerMain.popBackStack(R.id.headerFragment, false);
		} else if (item.getItemId() == R.id.updateFragment) {
			navControllerMain.popBackStack(R.id.updateFragment, false);
		} else if (item.getItemId() == R.id.hooksFragment) {
			navControllerMain.popBackStack(R.id.hooksFragment, false);
		} else if (item.getItemId() == R.id.ownPrefsFragment) {
			navControllerMain.popBackStack(R.id.ownPrefsFragment, false);
		}
	}

	@Override
	public void onSearchResultClicked(@NonNull final SearchPreferenceResult result, NavController navController) {
		headerFragment = new HeaderFragment();
		NavController myNavController = isTabletDevice ? navControllerDetails : navController;
		new Handler(getMainLooper()).post(() -> headerFragment.onSearchResultClicked(result, myNavController, this));
	}

	private void createNotificationChannel() {
		NotificationManager notificationManager = getSystemService(NotificationManager.class);

		notificationManager.createNotificationChannel(new NotificationChannel(UPDATES_CHANNEL_ID, getString(update_channel_name), IMPORTANCE_DEFAULT));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (data == null) return; //user hit cancel. Nothing to do

		SharedPreferences prefs = getDefaultSharedPreferences(createDeviceProtectedStorageContext());
		switch (requestCode) {
			case REQUEST_IMPORT:
				try {
					//noinspection DataFlowIssue
					PrefManager.importPath(prefs, getContentResolver().openInputStream(data.getData()));
					AppUtils.restart("systemui");
				} catch (Exception ignored) {
				}
				break;
			case REQUEST_EXPORT:
				try {
					//noinspection DataFlowIssue
					PrefManager.exportPrefs(prefs, getContentResolver().openOutputStream(data.getData()));
				} catch (Exception ignored) {
				}
				break;
		}
	}

	@Override
	public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, @NonNull Preference pref) {
		String key = pref.getKey();
		if (key == null) return false;

		NavController navController = isTabletDevice ? navControllerDetails : navControllerMain;

		return switch (key) {
			case "quicksettings_header" -> {
				if (isTabletDevice) navController.popBackStack(R.id.headerFragment, false);
				yield navigateTo(navController, R.id.action_headerFragment_to_quickSettingsFragment);
			}
			case "lockscreen_header" -> {
				if (isTabletDevice) navController.popBackStack(R.id.headerFragment, false);
				yield navigateTo(navController, R.id.action_headerFragment_to_lockScreenFragment);
			}
			case "theming_header" -> {
				if (isTabletDevice) navController.popBackStack(R.id.headerFragment, false);
				yield navigateTo(navController, R.id.action_headerFragment_to_themingFragment);
			}
			case "statusbar_header" -> {
				if (isTabletDevice) navController.popBackStack(R.id.headerFragment, false);
				yield navigateTo(navController, R.id.action_headerFragment_to_statusbarFragment);
			}
			case "nav_header" -> {
				if (isTabletDevice) navController.popBackStack(R.id.headerFragment, false);
				yield navigateTo(navController, R.id.action_headerFragment_to_navFragment);
			}
			case "dialer_header" -> {
				if (isTabletDevice) navController.popBackStack(R.id.headerFragment, false);
				yield navigateTo(navController, R.id.action_headerFragment_to_dialerFragment);
			}
			case "hotspot_header" -> {
				if (isTabletDevice) navController.popBackStack(R.id.headerFragment, false);
				yield navigateTo(navController, R.id.action_headerFragment_to_hotSpotFragment);
			}
			case "pm_header" -> {
				if (isTabletDevice) navController.popBackStack(R.id.headerFragment, false);
				yield navigateTo(navController, R.id.action_headerFragment_to_packageManagerFragment);
			}
			case "misc_header" -> {
				if (isTabletDevice) navController.popBackStack(R.id.headerFragment, false);
				yield navigateTo(navController, R.id.action_headerFragment_to_miscFragment);
			}
			case "CheckForUpdate" -> {
				if (isTabletDevice) {
					binding.navigationRailView.setSelectedItemId(R.id.updateFragment);
				} else {
					binding.bottomNavigationView.setSelectedItemId(R.id.updateFragment);
				}
				yield true;
			}
			case "qs_tile_qty" ->
					navigateTo(navController, R.id.action_quickSettingsFragment_to_QSTileQtyFragment);
			case "network_settings_header_qs" ->
					navigateTo(navController, R.id.action_quickSettingsFragment_to_networkFragment);
			case "sbc_header" ->
					navigateTo(navController, R.id.action_statusbarFragment_to_SBCFragment);
			case "BBarEnabled" ->
					navigateTo(navController, R.id.action_statusbarFragment_to_SBBBFragment);
			case "sbbIcon_header" ->
					navigateTo(navController, R.id.action_statusbarFragment_to_SBBIconFragment);
			case "network_settings_header" ->
					navigateTo(navController, R.id.action_statusbarFragment_to_networkFragment);
			case "threebutton_header" ->
					navigateTo(navController, R.id.action_navFragment_to_threeButtonNavFragment);
			case "taskbar_header" ->
					navigateTo(navController, R.id.action_navFragment_to_taskbarNavFragment);
			case "gesturenav_header" ->
					navigateTo(navController, R.id.action_navFragment_to_gestureNavFragment);
			case "remap_physical_buttons" ->
					navigateTo(navController, R.id.action_miscFragment_to_physicalButtonRemapFragment);
			case "netstat_header" ->
					navigateTo(navController, R.id.action_miscFragment_to_networkStatFragment);
			case "SleepOnFlatScreen" ->
					navigateTo(navController, R.id.action_miscFragment_to_sleepOnFlatFragment);
			case "icon_packs" ->
					navigateTo(navController, R.id.action_themingFragment_to_iconPackFragment);
			default -> false;
		};
	}

	@Override
	protected void onNewIntent(@NonNull Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}
}