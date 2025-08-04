package com.yhc.aospxpert.ui.fragments;


import static com.yhc.aospxpert.utils.NavigationExtensionKt.navigateTo;

import android.app.Activity;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.yhc.aospxpert.R;
import com.yhc.aospxpert.ui.models.SearchPreferenceItem;
import com.yhc.aospxpert.ui.preferences.preferencesearch.SearchConfiguration;
import com.yhc.aospxpert.ui.preferences.preferencesearch.SearchPreference;
import com.yhc.aospxpert.ui.preferences.preferencesearch.SearchPreferenceResult;
import com.yhc.aospxpert.utils.ControlledPreferenceFragmentCompat;
import com.yhc.aospxpert.utils.DisplayUtils;

public class HeaderFragment extends ControlledPreferenceFragmentCompat {

	SearchPreference searchPreference;
	public static SearchPreferenceItem[] searchItems = null;

	@Override
	public boolean isBackButtonEnabled() {
		return false;
	}

	@Override
	public String getTitle() {
		return getString(R.string.app_name);
	}

	@Override
	public int getLayoutResource() {
		return R.xml.header_preferences;
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		super.onCreatePreferences(savedInstanceState, rootKey);
		initSearchableItems();

		searchPreference = findPreference("searchPreference");

		if (searchPreference != null && getActivity() != null) {
			SearchConfiguration config = searchPreference.getSearchConfiguration();
			config.setActivity((AppCompatActivity) getActivity());
			config.setNavigationController(NavHostFragment.findNavController(this), R.id.searchPreferenceFragment, R.id.action_headerFragment_to_searchPreferenceFragment);

			for (SearchPreferenceItem searchItem : searchItems) {
				config.index(searchItem.getXml()).addBreadcrumb(this.getResources().getString(searchItem.getTitle()));
			}

			config.setBreadcrumbsEnabled(true);
			config.setHistoryEnabled(true);
			config.setFuzzySearchEnabled(false);
		}
	}

	public void onSearchResultClicked(SearchPreferenceResult result, NavController navController, Activity activity) {
		for (SearchPreferenceItem searchItem : searchItems) {
			if (searchItem.getXml() == result.getResourceFile()) {
				int newActionId = searchItem.getActionId();
				boolean isTabletDevice = DisplayUtils.isTablet();

				if (isTabletDevice) {
					String originalActionName = activity.getResources().getResourceEntryName(searchItem.getActionId());
					String newActionName = originalActionName.replace("searchPreferenceFragment", "headerFragment");
					newActionId = activity.getResources().getIdentifier(newActionName, "id", activity.getPackageName());
				}

				Bundle bundle = new Bundle();
				bundle.putString("searchKey", result.getKey());
				navigateTo(navController, newActionId, bundle);
				break;
			}
		}
	}

	private void initSearchableItems() {
		searchItems = new SearchPreferenceItem[]{
				new SearchPreferenceItem(R.xml.dialer_prefs, R.string.dialer_header, R.id.action_searchPreferenceFragment_to_dialerFragment),
				new SearchPreferenceItem(R.xml.gesture_nav_prefs, R.string.gesturenav_header, R.id.action_searchPreferenceFragment_to_gestureNavFragment),
				new SearchPreferenceItem(R.xml.hotspot_prefs, R.string.hotspot_header, R.id.action_searchPreferenceFragment_to_hotSpotFragment),
				new SearchPreferenceItem(R.xml.lock_screen_prefs, R.string.lockscreen_header_title, R.id.action_searchPreferenceFragment_to_lockScreenFragment),
				new SearchPreferenceItem(R.xml.misc_prefs, R.string.misc_header, R.id.action_searchPreferenceFragment_to_miscFragment),
				new SearchPreferenceItem(R.xml.nav_prefs, R.string.nav_header, R.id.action_searchPreferenceFragment_to_navFragment),
				new SearchPreferenceItem(R.xml.sbqs_network_prefs, R.string.ntsb_category_title, R.id.action_searchPreferenceFragment_to_networkFragment),
				new SearchPreferenceItem(R.xml.lsqs_custom_text, R.string.netstat_header, R.id.action_searchPreferenceFragment_to_networkStatFragment),
				new SearchPreferenceItem(R.xml.own_prefs_header, R.string.own_prefs_header, R.id.action_searchPreferenceFragment_to_ownPrefsFragment),
				new SearchPreferenceItem(R.xml.packagemanger_prefs, R.string.pm_header, R.id.action_searchPreferenceFragment_to_packageManagerFragment),
				new SearchPreferenceItem(R.xml.physical_buttons_prefs, R.string.remap_physical_buttons_title, R.id.action_searchPreferenceFragment_to_physicalButtonRemapFragment),
				new SearchPreferenceItem(R.xml.qs_tile_qty_prefs, R.string.qs_tile_qty_title, R.id.action_searchPreferenceFragment_to_QSTileQtyFragment),
				new SearchPreferenceItem(R.xml.quicksettings_prefs, R.string.qs_panel_category_title, R.id.action_searchPreferenceFragment_to_quickSettingsFragment),
				new SearchPreferenceItem(R.xml.statusbar_batterybar_prefs, R.string.sbbb_header, R.id.action_searchPreferenceFragment_to_SBBBFragment),
				new SearchPreferenceItem(R.xml.statusbar_batteryicon_prefs, R.string.sbbIcon_header, R.id.action_searchPreferenceFragment_to_SBBIconFragment),
				new SearchPreferenceItem(R.xml.statusbar_clock_prefs, R.string.sbc_header, R.id.action_searchPreferenceFragment_to_SBCFragment2),
				new SearchPreferenceItem(R.xml.sleep_on_flat_prefs, R.string.sleep_on_flat_screen_title, R.id.action_searchPreferenceFragment_to_sleepOnFlatFragment),
				new SearchPreferenceItem(R.xml.statusbar_settings, R.string.statusbar_header, R.id.action_searchPreferenceFragment_to_statusbarFragment),
				new SearchPreferenceItem(R.xml.taskbar_prefs, R.string.taskbar_header_title, R.id.action_searchPreferenceFragment_to_taskbarNavFragment),
				new SearchPreferenceItem(R.xml.theming_prefs, R.string.theme_customization_category, R.id.action_searchPreferenceFragment_to_themingFragment),
				new SearchPreferenceItem(R.xml.three_button_prefs, R.string.threebutton_header_title, R.id.action_searchPreferenceFragment_to_threeButtonNavFragment),
		};
	}
}
