package com.yhc.aospxpert.ui.fragments.iconpack;

import static com.yhc.aospxpert.utils.MiscUtils.dpToPx;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.transition.Slide;
import androidx.transition.TransitionManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import com.yhc.aospxpert.R;
import com.yhc.aospxpert.databinding.FragmentIconPackBinding;
import com.yhc.aospxpert.ui.fragments.BaseFragment;
import com.yhc.aospxpert.utils.IconPackUtil;

public class IconPackFragment extends BaseFragment implements IconPackUtil.IconPackQueryListener {

	private IconPackUtil mIconPackUtil;
	private FragmentIconPackBinding binding;

	// Search
	private String mSearchQuery = "";

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		menu.clear();
		inflater.inflate(R.menu.icon_pack_toolbar_menu, menu);

		// Menu Items
		MenuItem mSearchItem = menu.findItem(R.id.icon_pack_search);
		mSearchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
			@Override
			public boolean onMenuItemActionExpand(@NonNull MenuItem menuItem) {
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(@NonNull MenuItem menuItem) {
				TransitionManager.beginDelayedTransition(requireActivity().findViewById(R.id.toolbar), new Slide(Gravity.START));
				mSearchQuery = "";
				submitQuery();
				return true;
			}
		});
		SearchView mSearchView = (SearchView) mSearchItem.getActionView();


		assert mSearchView != null;
		mSearchView.setQueryHint(getString(R.string.searchpreference_search));
		mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				mSearchQuery = query;
				submitQuery();
				return false;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				mSearchQuery = newText;
				submitQuery();
				return false;
			}
		});
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		binding = FragmentIconPackBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		mIconPackUtil = IconPackUtil.getInstance(requireContext());
		mIconPackUtil.addListener(this);

		IconPackCollectionAdapter fragmentCollectionAdapter = new IconPackCollectionAdapter(this);
		binding.pager.setAdapter(fragmentCollectionAdapter);
		String[] mTabTitles = {getString(R.string.icon_pack_selection_title), getString(R.string.icon_pack_customization_title)};
		new TabLayoutMediator(binding.tabLayout, binding.pager,
				(tab, position) -> tab.setText(mTabTitles[position])
		).attach();
		binding.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
			@Override
			public void onPageSelected(int position) {
				super.onPageSelected(position);
				binding.resetButton.setVisibility(isFabVisible() ? View.VISIBLE : View.GONE);
				submitQuery();
			}
		});
		binding.resetButton.setOnClickListener(v -> resetIconPacks());

		ViewGroup tabs = (ViewGroup) binding.tabLayout.getChildAt(0);
		int tabCount = tabs.getChildCount();
		Log.i("IconPackFragment", "tabCount: " + tabCount);
		for (int i = 0; i < tabCount; i++) {
			View tab = tabs.getChildAt(i);
			LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) tab.getLayoutParams();
			if (i != 0) layoutParams.setMarginStart(dpToPx(6));
			if (i != tabCount - 1) layoutParams.setMarginEnd(dpToPx(6));
			tab.setLayoutParams(layoutParams);
			tab.requestLayout();
		}
		binding.tabLayout.requestLayout();
	}

	@Override
	public void onIconPacksLoaded(IconPackUtil.ResourceMapping mapping, IconPackUtil.IconPackMapping packMapping) {
		new Handler(Looper.getMainLooper()).post(() -> {
			binding.loadingIndicator.setVisibility(View.GONE);

			boolean iconPackAvailable = packMapping != null && !packMapping.isEmpty();
			binding.tabLayout.setVisibility(iconPackAvailable ? View.VISIBLE : View.GONE);
			binding.pager.setVisibility(iconPackAvailable ? View.VISIBLE : View.GONE);
			binding.noPacksLayout.setVisibility(iconPackAvailable ? View.GONE : View.VISIBLE);
		});
	}

	private void submitQuery() {
		Fragment currentFragment = getCurrentFragment();
		if (currentFragment instanceof IconPackListFragment iconPackListFragment) {
			iconPackListFragment.query(mSearchQuery);
		} else if (currentFragment instanceof IconPackCustomizationFragment iconPackCustomizationFragment) {
			iconPackCustomizationFragment.query(mSearchQuery);
		}
	}

	private boolean isFabVisible() {
		Fragment currentFragment = getCurrentFragment();
		if (currentFragment instanceof IconPackListFragment iconPackListFragment) {
			return iconPackListFragment.isFabVisible();
		} else if (currentFragment instanceof IconPackCustomizationFragment iconPackCustomizationFragment) {
			return iconPackCustomizationFragment.isFabVisible();
		}
		return false;
	}

	public void requestFabVisibility() {
		binding.resetButton.setVisibility(isFabVisible() ? View.VISIBLE : View.GONE);
	}

	private Fragment getCurrentFragment() {
		int currentItem = binding.pager.getCurrentItem();
		String fragmentTag = "f" + currentItem;
		return getChildFragmentManager().findFragmentByTag(fragmentTag);
	}

	private void resetIconPacks() {
		IconPackUtil iconPackUtil = IconPackUtil.getInstance(requireContext());
		for (IconPackUtil.IconPack iconPack : iconPackUtil.mIconPackMapping.getIconPacks()) {
			iconPackUtil.disable(iconPack);
		}
		iconPackUtil.queryIconPacks(false);
	}

	@Override
	public String getTitle() {
		return getString(R.string.icon_packs_title);
	}

	private static class IconPackCollectionAdapter extends FragmentStateAdapter {

		public IconPackCollectionAdapter(Fragment fragment) {
			super(fragment);
		}

		@NonNull
		@Override
		public Fragment createFragment(int position) {
			Fragment fragment;
			if (position == 0) {
				fragment = new IconPackListFragment();
			} else {
				fragment = new IconPackCustomizationFragment();
			}
			return fragment;
		}

		@Override
		public int getItemCount() {
			return 2;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		mIconPackUtil.queryIconPacks(true);
	}

}
