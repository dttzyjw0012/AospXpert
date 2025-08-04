package com.yhc.aospxpert.utils;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.yhc.aospxpert.ui.Constants.PX_ICON_PACK_REPO;
import static com.yhc.aospxpert.utils.TextUtils.getClickableText;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorInt;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.transition.Slide;
import androidx.transition.TransitionManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

import com.yhc.aospxpert.AOSPXpert;
import com.yhc.aospxpert.R;
import com.yhc.aospxpert.ui.fragments.iconpack.IconPackFragment;

public class MiscUtils {

	public static final int REQUEST_IMPORT = 7;
	public static final int REQUEST_EXPORT = 9;

	public static @ColorInt int getColorFromAttribute(Context context, int attr) {
		TypedValue typedValue = new TypedValue();
		context.getTheme().resolveAttribute(attr, typedValue, true);
		return typedValue.data;
	}

	public static String intToHex(int colorValue) {
		return String.format("#%06X", (0xFFFFFF & colorValue));
	}

	public static int dpToPx(int dp) {
		return dpToPx((float) dp);
	}

	public static int dpToPx(float dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, AOSPXpert.get().getResources().getDisplayMetrics());
	}

	@SuppressWarnings("deprecation")
	public static void setupToolbar(Fragment fragment, View view, String title, boolean isBackButtonEnabled) {
		AppCompatActivity baseContext = (AppCompatActivity) fragment.getContext();
		Toolbar toolbar = view.findViewById(R.id.toolbar);

		if (baseContext != null && toolbar != null) {
			toolbar.setTitle(title);
			if (isBackButtonEnabled) toolbar.setNavigationIcon(R.drawable.ic_toolbar_chevron);
			else toolbar.setNavigationIcon(null);
			toolbar.setNavigationOnClickListener(v -> baseContext.onBackPressed());

			NavController thisNavController = NavHostFragment.findNavController(fragment);
			NavigationUI.setupWithNavController(toolbar, thisNavController);

			thisNavController.addOnDestinationChangedListener((navController, navDestination, bundle) -> {
				if (isBackButtonEnabled) toolbar.setNavigationIcon(R.drawable.ic_toolbar_chevron);
				else toolbar.setNavigationIcon(null);

				if (!Objects.equals(title, baseContext.getString(R.string.app_name))) {
					toolbar.addMenuProvider(new MenuProvider() {
						@Override
						public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
							menu.clear();

							if (Objects.equals(title, baseContext.getString(R.string.icon_packs_title))) {
								menuInflater.inflate(R.menu.icon_pack_toolbar_menu, menu);

								MenuItem mSearchItem = menu.findItem(R.id.icon_pack_search);
								mSearchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
									@Override
									public boolean onMenuItemActionExpand(@androidx.annotation.NonNull MenuItem menuItem) {
										return true;
									}

									@Override
									public boolean onMenuItemActionCollapse(@androidx.annotation.NonNull MenuItem menuItem) {
										TransitionManager.beginDelayedTransition(toolbar, new Slide(Gravity.START));
										if (fragment instanceof IconPackFragment) {
											((IconPackFragment) fragment).mSearchQuery = "";
											((IconPackFragment) fragment).submitQuery();
										}
										return true;
									}
								});

								SearchView mSearchView = (SearchView) mSearchItem.getActionView();

								assert mSearchView != null;
								mSearchView.setQueryHint(baseContext.getString(R.string.searchpreference_search));
								mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
									@Override
									public boolean onQueryTextSubmit(String query) {
										if (fragment instanceof IconPackFragment) {
											((IconPackFragment) fragment).mSearchQuery = query;
											((IconPackFragment) fragment).submitQuery();
										}
										return false;
									}

									@Override
									public boolean onQueryTextChange(String newText) {
										if (fragment instanceof IconPackFragment) {
											((IconPackFragment) fragment).mSearchQuery = newText;
											((IconPackFragment) fragment).submitQuery();
										}
										return false;
									}
								});
							} else {
								menuInflater.inflate(R.menu.main_menu, menu);
							}
						}

						@Override
						public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
							setOnOptionsItemSelected(menuItem, fragment.requireActivity(), fragment);
							return true;
						}
					});
				}
			});
		}

		if (baseContext != null && baseContext.getSupportActionBar() != null) {
			baseContext.getSupportActionBar().setTitle(title);
			baseContext.getSupportActionBar().setDisplayHomeAsUpEnabled(isBackButtonEnabled);
			baseContext.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_toolbar_chevron);
		}
	}

	public static void setOnOptionsItemSelected(MenuItem item, Activity activity, Fragment fragment) {
		int itemID = item.getItemId();
		NavController navController = NavHostFragment.findNavController(fragment);
		SharedPreferences prefs = getDefaultSharedPreferences(activity.createDeviceProtectedStorageContext());

		if (itemID == android.R.id.home) {
			navController.navigateUp();
		} else if (itemID == R.id.menu_clearPrefs) {
			PrefManager.clearPrefs(prefs);
			AppUtils.restart("systemui");
		} else if (itemID == R.id.menu_exportPrefs) {
			importExportSettings(activity, true);
		} else if (itemID == R.id.menu_importPrefs) {
			importExportSettings(activity, false);
		} else if (itemID == R.id.menu_restart) {
			AppUtils.restart("system");
		} else if (itemID == R.id.menu_restartSysUI) {
			AppUtils.restart("systemui");
		} else if (itemID == R.id.menu_soft_restart) {
			AppUtils.restart("zygote");
		} else if (itemID == R.id.icon_pack_info) {
			AlertDialog alertDialog = new MaterialAlertDialogBuilder(activity, R.style.MaterialComponents_MaterialAlertDialog)
					.setTitle(activity.getString(R.string.icon_pack_disclaimer_title))
					.setMessage(getClickableText(activity, activity.getString(R.string.icon_pack_disclaimer_desc, PX_ICON_PACK_REPO), PX_ICON_PACK_REPO))
					.setPositiveButton(R.string.okay, (dialog, which) -> dialog.dismiss())
					.show();

			TextView messageTextView = alertDialog.findViewById(android.R.id.message);
			if (messageTextView != null) {
				messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
			}
		} else if (itemID == R.id.icon_pack_search) {
			TransitionManager.beginDelayedTransition(activity.findViewById(R.id.toolbar), new Slide(Gravity.START));
		}
	}

	public static void importExportSettings(Activity activity, boolean export) {
		Intent fileIntent = new Intent();
		fileIntent.setAction(export ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_GET_CONTENT);
		fileIntent.setType("*/*");
		fileIntent.putExtra(Intent.EXTRA_TITLE, "AospXpert_Config" + ".bin");
		activity.startActivityForResult(fileIntent, export ? REQUEST_EXPORT : REQUEST_IMPORT);
	}

	public static void setOnBackPressedDispatcherCallback(FragmentActivity fragmentActivity, Fragment fragment) {
		final boolean isTabletDevice = DisplayUtils.isTablet();

		NavHostFragment navHostFragmentMain = (NavHostFragment) fragmentActivity.getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainerView);
		NavController navControllerMain = Objects.requireNonNull(navHostFragmentMain).getNavController();

		NavHostFragment navHostFragmentDetails = (NavHostFragment) fragmentActivity.getSupportFragmentManager().findFragmentById(R.id.detailFragmentContainerView);
		NavController navControllerDetails = Objects.requireNonNull(navHostFragmentDetails).getNavController();

		fragmentActivity.getOnBackPressedDispatcher().addCallback(fragment.getViewLifecycleOwner(), new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				if (isTabletDevice &&
						navControllerDetails.getCurrentDestination() != null &&
						navControllerDetails.popBackStack()) {
					// Handled by details NavController
					return;
				}

				if (navControllerMain.getCurrentDestination() != null &&
						navControllerMain.popBackStack()) {
					// Handled by main NavController
					return;
				}

				// Nothing to pop — let system handle it (e.g. finish activity)
				setEnabled(false); // Temporarily disable to avoid infinite loop
				fragmentActivity.getOnBackPressedDispatcher().onBackPressed();
			}
		});
	}
}