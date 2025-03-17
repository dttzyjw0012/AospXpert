package com.yhc.aospxpert.ui.activities;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.yhc.aospxpert.R.string.update_channel_name;
import static com.yhc.aospxpert.ui.Constants.PX_ICON_PACK_REPO;
import static com.yhc.aospxpert.ui.Constants.UPDATES_CHANNEL_ID;
import static com.yhc.aospxpert.utils.AppUtils.isLikelyPixelBuild;
import static com.yhc.aospxpert.utils.NavigationExtensionKt.navigateTo;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.LocaleList;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.transition.Slide;
import androidx.transition.TransitionManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;
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
import com.yhc.aospxpert.utils.ExtendedSharedPreferences;
import com.yhc.aospxpert.utils.PrefManager;
import com.yhc.aospxpert.utils.PreferenceHelper;

public class SettingsActivity extends BaseActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback, SearchPreferenceResultListener {

	private static final int REQUEST_IMPORT = 7;
	private static final int REQUEST_EXPORT = 9;
	private SettingsActivityBinding binding;
	private HeaderFragment headerFragment;
	private NavController navController;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = SettingsActivityBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		createNotificationChannel();
		setupBottomNavigationView();

		PreferenceHelper.init(ExtendedSharedPreferences.from(getDefaultSharedPreferences(createDeviceProtectedStorageContext())));

		if (getIntent() != null) {
			if (getIntent().getBooleanExtra("updateTapped", false)) {
				Intent intent = getIntent();
				Bundle bundle = new Bundle();
				bundle.putBoolean("updateTapped", intent.getBooleanExtra("updateTapped", false));
				bundle.putString("filePath", intent.getStringExtra("filePath"));
				UpdateFragment updateFragment = new UpdateFragment();
				updateFragment.setArguments(bundle);
				navigateTo(navController, R.id.updateFragment, bundle);
			} else if ("true".equals(getIntent().getStringExtra("migratePrefs"))) {
				Intent intent = getIntent();
				Bundle bundle = new Bundle();
				bundle.putString("migratePrefs", intent.getStringExtra("migratePrefs"));
				UpdateFragment updateFragment = new UpdateFragment();
				updateFragment.setArguments(bundle);
				navigateTo(navController, R.id.updateFragment, bundle);
			} else if (getIntent().getBooleanExtra("newUpdate", false)) {
				navigateTo(navController, R.id.updateFragment);
			} else if (getIntent().hasExtra(Intent.EXTRA_COMPONENT_NAME)) {
				ComponentName callerComponentName = getIntent().getParcelableExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName.class);
				if(callerComponentName != null) {
					String callerClassName = callerComponentName.getClassName();
					if(SleepOnSurfaceTileService.class.getName().equals(callerClassName))
					{
						navigateTo(navController, R.id.sleepOnFlatFragment);
					}
				}
			}
		}

		if (!isLikelyPixelBuild() && !BuildConfig.DEBUG) {
			new MaterialAlertDialogBuilder(this, R.style.MaterialComponents_MaterialAlertDialog)
					.setTitle(R.string.incompatible_alert_title)
					.setMessage(R.string.incompatible_alert_body)
					.setPositiveButton(R.string.incompatible_alert_ok_btn, (dialog, which) -> dialog.dismiss())
					.show();
		}
	}

	@SuppressLint({"RestrictedApi", "NonConstantResourceId"})
	private void setupBottomNavigationView() {
		NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
		navController = Objects.requireNonNull(navHostFragment).getNavController();

		NavigationUI.setupWithNavController(binding.bottomNavigationView, navController);

		binding.bottomNavigationView.setOnItemSelectedListener(item -> {
			if (item.getItemId() == R.id.headerFragment) {
				return navController.popBackStack(R.id.headerFragment, false);
			} else if (item.getItemId() == R.id.updateFragment) {
				navController.popBackStack(R.id.headerFragment, false);
				return navigateTo(navController, R.id.updateFragment);
			} else if (item.getItemId() == R.id.hooksFragment) {
				navController.popBackStack(R.id.headerFragment, false);
				return navigateTo(navController, R.id.hooksFragment);
			} else if (item.getItemId() == R.id.ownPrefsFragment) {
				navController.popBackStack(R.id.headerFragment, false);
				return navigateTo(navController, R.id.ownPrefsFragment);
			}
			return false;
		});

		binding.bottomNavigationView.setOnItemReselectedListener(item -> {
			if (item.getItemId() == R.id.headerFragment) {
				navController.popBackStack(R.id.headerFragment, false);
			} else if (item.getItemId() == R.id.updateFragment) {
				navController.popBackStack(R.id.updateFragment, false);
			} else if (item.getItemId() == R.id.hooksFragment) {
				navController.popBackStack(R.id.hooksFragment, false);
			} else if (item.getItemId() == R.id.ownPrefsFragment) {
				navController.popBackStack(R.id.ownPrefsFragment, false);
			}
		});
	}

	@Override
	public void onSearchResultClicked(@NonNull final SearchPreferenceResult result, NavController navController) {
		headerFragment = new HeaderFragment();
		new Handler(getMainLooper()).post(() -> headerFragment.onSearchResultClicked(result, navController));
	}

	@Override
	protected void attachBaseContext(Context newBase) {
		SharedPreferences prefs = getDefaultSharedPreferences(newBase.createDeviceProtectedStorageContext());

		String localeCode = prefs.getString("appLanguage", "");
		Locale locale = !localeCode.isEmpty() ? Locale.forLanguageTag(localeCode) : Locale.getDefault();

		Resources res = newBase.getResources();
		Configuration configuration = res.getConfiguration();

		configuration.setLocale(locale);

		LocaleList localeList = new LocaleList(locale);
		LocaleList.setDefault(localeList);
		configuration.setLocales(localeList);

		super.attachBaseContext(newBase.createConfigurationContext(configuration));
	}

	private void createNotificationChannel() {
		NotificationManager notificationManager = getSystemService(NotificationManager.class);

		notificationManager.createNotificationChannel(new NotificationChannel(UPDATES_CHANNEL_ID, getString(update_channel_name), IMPORTANCE_DEFAULT));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		SharedPreferences prefs = getDefaultSharedPreferences(createDeviceProtectedStorageContext());

		int itemID = item.getItemId();

		if (itemID == android.R.id.home) {
			navController.navigateUp();
		} else if (itemID == R.id.menu_clearPrefs) {
			PrefManager.clearPrefs(prefs);
			AppUtils.Restart("systemui");
		} else if (itemID == R.id.menu_exportPrefs) {
			importExportSettings(true);
		} else if (itemID == R.id.menu_importPrefs) {
			importExportSettings(false);
		} else if (itemID == R.id.menu_restart) {
			AppUtils.Restart("system");
		} else if (itemID == R.id.menu_restartSysUI) {
			AppUtils.Restart("systemui");
		} else if (itemID == R.id.menu_soft_restart) {
			AppUtils.Restart("zygote");
		} else if (itemID == R.id.icon_pack_info) {
			AlertDialog alertDialog = new MaterialAlertDialogBuilder(this, R.style.MaterialComponents_MaterialAlertDialog)
					.setTitle(getString(R.string.icon_pack_disclaimer_title))
					.setMessage(getClickableText(getString(R.string.icon_pack_disclaimer_desc, PX_ICON_PACK_REPO), PX_ICON_PACK_REPO))
					.setPositiveButton(R.string.okay, (dialog, which) -> dialog.dismiss())
					.show();

			TextView messageTextView = alertDialog.findViewById(android.R.id.message);
			if (messageTextView != null) {
				messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
			}
		} else if (itemID == R.id.icon_pack_search) {
			TransitionManager.beginDelayedTransition(findViewById(R.id.toolbar), new Slide(Gravity.START));
		}

		return true;
	}

	/** @noinspection SameParameterValue*/
	@NonNull
	private SpannableString getClickableText(String message, String link) {
		SpannableString spannableMessage = new SpannableString(message);

		int start = message.indexOf(link);
		int end = start + link.length();

		spannableMessage.setSpan(new ClickableSpan() {
			@Override
			public void onClick(@NonNull View widget) {
				try {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
					startActivity(intent);
				} catch (Exception exception) {
					Log.e("IconPackRepo", "Browser not found");
				}
			}
		}, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

		Linkify.addLinks(spannableMessage, Linkify.WEB_URLS);

		return spannableMessage;
	}

	@SuppressWarnings("deprecation")
	private void importExportSettings(boolean export) {
		Intent fileIntent = new Intent();
		fileIntent.setAction(export ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_GET_CONTENT);
		fileIntent.setType("*/*");
		fileIntent.putExtra(Intent.EXTRA_TITLE, "AospXpert_Config" + ".bin");
		startActivityForResult(fileIntent, export ? REQUEST_EXPORT : REQUEST_IMPORT);
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
					AppUtils.Restart("systemui");
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

		return switch (key) {
			case "quicksettings_header" ->
					navigateTo(navController, R.id.action_headerFragment_to_quickSettingsFragment);
			case "lockscreen_header" ->
					navigateTo(navController, R.id.action_headerFragment_to_lockScreenFragment);
			case "theming_header" ->
					navigateTo(navController, R.id.action_headerFragment_to_themingFragment);
			case "statusbar_header" ->
					navigateTo(navController, R.id.action_headerFragment_to_statusbarFragment);
			case "nav_header" ->
					navigateTo(navController, R.id.action_headerFragment_to_navFragment);
			case "dialer_header" ->
					navigateTo(navController, R.id.action_headerFragment_to_dialerFragment);
			case "hotspot_header" ->
					navigateTo(navController, R.id.action_headerFragment_to_hotSpotFragment);
			case "pm_header" ->
					navigateTo(navController, R.id.action_headerFragment_to_packageManagerFragment);
			case "misc_header" ->
					navigateTo(navController, R.id.action_headerFragment_to_miscFragment);
			case "CheckForUpdate" -> {
				navController.popBackStack(R.id.headerFragment, false);
				yield navigateTo(navController, R.id.action_headerFragment_to_updateFragment);
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