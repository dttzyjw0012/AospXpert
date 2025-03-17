package com.yhc.aospxpert.ui.fragments.iconpack;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import com.yhc.aospxpert.R;
import com.yhc.aospxpert.databinding.FragmentIconPackListBinding;
import com.yhc.aospxpert.ui.adapter.IconPackAdapter;
import com.yhc.aospxpert.ui.adapter.ItemChangedListener;
import com.yhc.aospxpert.ui.fragments.BaseFragment;
import com.yhc.aospxpert.utils.IconPackUtil;

public class IconPackListFragment extends BaseFragment implements IconPackUtil.IconPackQueryListener {

	private IconPackUtil mIconPackUtil;
	private FragmentIconPackListBinding binding;
	private IconPackAdapter mAdapter;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentIconPackListBinding.inflate(inflater, container, false);

		mIconPackUtil = IconPackUtil.getInstance(requireContext());
		mIconPackUtil.addListener(this);

		return binding.getRoot();
	}

	@Override
	public String getTitle() {
		return getString(R.string.icon_packs_title);
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
	}

	@Override
	public void onIconPacksLoaded(IconPackUtil.ResourceMapping mapping, IconPackUtil.IconPackMapping packMapping) {
		List<IconPackUtil.IconPack> iconPacks = new ArrayList<>(packMapping.keySet());
		iconPacks.sort((o1, o2) -> {
			int nameComparison = o1.mName.compareTo(o2.mName);
			if (nameComparison == 0) {
				return o1.mPackageName.compareTo(o2.mPackageName);
			}
			return nameComparison;
		});
		new Handler(Looper.getMainLooper()).post(this::requestFabVisibility);
		mAdapter = new IconPackAdapter(mIconPackUtil, iconPacks, packMapping, mItemChangedListener);
		binding.recyclerView.post(() -> binding.recyclerView.setAdapter(mAdapter));
	}

	private final ItemChangedListener mItemChangedListener = () -> new Handler(Looper.getMainLooper()).post(this::requestFabVisibility);

	@Override
	public void onDestroy() {
		mIconPackUtil.removeListener(this);
		super.onDestroy();
	}

	public boolean isFabVisible() {
		return mIconPackUtil.isAnythingEnabled();
	}

	private void requestFabVisibility() {
		if (getParentFragment() instanceof IconPackFragment iconPackFragment) {
			iconPackFragment.requestFabVisibility();
		}
	}

	public void query(String mSearchQuery) {
		if (mAdapter == null) return;
		mAdapter.filter(mSearchQuery);
	}
}
