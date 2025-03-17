package com.yhc.aospxpert.ui.adapter;

import static com.yhc.aospxpert.utils.MiscUtils.getColorFromAttribute;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import com.yhc.aospxpert.AOSPXpert;
import com.yhc.aospxpert.R;
import com.yhc.aospxpert.databinding.ViewItemIconPackBinding;
import com.yhc.aospxpert.databinding.ViewRecyclerIconsBinding;
import com.yhc.aospxpert.utils.IconPackUtil;

public class IconPackAdapter extends RecyclerView.Adapter<IconPackAdapter.ViewHolder> {

	private final String TAG = IconPackAdapter.class.getSimpleName();
	private final List<IconPackUtil.IconPack> mPacks;
	private final List<IconPackUtil.IconPack> mFilteredPacks;
	private final IconPackUtil.IconPackMapping mPacksMapping;
	private final IconPackUtil mPackUtil;
	private final ItemChangedListener mItemChangedListener;

	public IconPackAdapter(IconPackUtil iconPackUtil, List<IconPackUtil.IconPack> packs, IconPackUtil.IconPackMapping packMapping, ItemChangedListener itemChangedListener) {
		mPacks = packs;
		mFilteredPacks = new ArrayList<>(packs);
		mPackUtil = iconPackUtil;
		mPacksMapping = packMapping;
		mItemChangedListener = itemChangedListener;
	}

	@Override
	public int getItemViewType(int position) {
		if (mFilteredPacks.size() == 1) {
			return VIEW_TYPE_SINGLE;
		} else if (position == 0) {
			return VIEW_TYPE_TOP;
		} else if (position == mFilteredPacks.size() - 1) {
			return VIEW_TYPE_BOTTOM;
		} else {
			return VIEW_TYPE_MIDDLE;
		}
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		ViewItemIconPackBinding binding = ViewItemIconPackBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
		return new ViewHolder(binding);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		IconPackUtil.IconPack pack = mFilteredPacks.get(position);

		setLayoutBackground(holder, position, mPackUtil.getEnabledState(pack));

		holder.bind(pack, Objects.requireNonNull(mPacksMapping.get(pack)), mPackUtil.getEnabledState(pack));
	}

	private void setLayoutBackground(ViewHolder holder, int position, int enabledState) {
		LayerDrawable cardBackground = switch (getItemViewType(position)) {
			case VIEW_TYPE_TOP ->
					(LayerDrawable) ResourcesCompat.getDrawable(holder.itemView.getResources(), R.drawable.container_top_selected, null);
			case VIEW_TYPE_MIDDLE ->
					(LayerDrawable) ResourcesCompat.getDrawable(holder.itemView.getResources(), R.drawable.container_mid_selected, null);
			case VIEW_TYPE_BOTTOM ->
					(LayerDrawable) ResourcesCompat.getDrawable(holder.itemView.getResources(), R.drawable.container_bottom_selected, null);
			default ->
					(LayerDrawable) ResourcesCompat.getDrawable(holder.itemView.getResources(), R.drawable.container_single_selected, null);
		};

		if (cardBackground != null) {
			Drawable drawable = cardBackground.getDrawable(1);
			switch (enabledState) {
				case IconPackUtil.ENABLED_FULL:
					drawable.setTint(getColorFromAttribute(holder.itemView.getContext(), R.attr.colorPrimary));
					break;
				case IconPackUtil.ENABLED_PARTIAL:
					drawable.setTint(getColorWithHalfOpacity(getColorFromAttribute(holder.itemView.getContext(), R.attr.colorPrimary)));
					break;
				default:
					drawable.setTint(getColorFromAttribute(holder.itemView.getContext(), isNightMode(holder.itemView.getContext()) ? R.attr.colorSurfaceBright : R.attr.colorSurface));
					break;
			}
			drawable.mutate();

			holder.itemView.setBackground(cardBackground);
		}
	}

	private boolean isNightMode(Context context) {
		Configuration config = context.getResources().getConfiguration();
		return (config.uiMode & Configuration.UI_MODE_NIGHT_MASK)
				== Configuration.UI_MODE_NIGHT_YES;
	}

	@Override
	public int getItemCount() {
		return mFilteredPacks.size();
	}

	@SuppressLint("NotifyDataSetChanged")
    public void filter(String text) {
		mFilteredPacks.clear();
		String filterText = text != null ? text : "";
		for (IconPackUtil.IconPack pack : mPacks) {
			if (pack.mName.toLowerCase().contains(filterText.toLowerCase()) || pack.mAuthor.toLowerCase().contains(filterText.toLowerCase())) {
				mFilteredPacks.add(pack);
			}
		}
		mFilteredPacks.sort((o1, o2) -> {
			int nameComparison = o1.mName.compareTo(o2.mName);
			if (nameComparison == 0) {
				return o1.mPackageName.compareTo(o2.mPackageName);
			}
			return nameComparison;
		});
		notifyDataSetChanged();
	}


	public class ViewHolder extends RecyclerView.ViewHolder {

		private final ViewItemIconPackBinding binding;

		public ViewHolder(ViewItemIconPackBinding itemView) {
			super(itemView.getRoot());
			binding = itemView;
		}

		@SuppressLint("NonConstantResourceId")
		public void bind(IconPackUtil.IconPack pack, HashMap<String, ArrayList<IconPackUtil.ReplacementIcon>> replacementMapping, int enabledState) {
			Context context = itemView.getContext();
			String name = pack.mName.isEmpty() ? AOSPXpert.get().getString(R.string.icon_pack_unknown_name) : pack.mName;
			String author = pack.mAuthor.isEmpty() ?
					AOSPXpert.get().getString(R.string.icon_pack_unknown_author) :
					String.format(AOSPXpert.get().getString(R.string.icon_pack_author), pack.mAuthor);

			if (mPackUtil.getEnabledState(pack) == IconPackUtil.ENABLED_PARTIAL) {
				String partiallyEnabledText = AOSPXpert.get().getString(R.string.partially_enabled);
				String fullText = name + " " + partiallyEnabledText;
				SpannableString spannableString = new SpannableString(fullText);

				int start = fullText.indexOf(partiallyEnabledText);
				int end = start + partiallyEnabledText.length();

				TypedValue typedValue = new TypedValue();
				int colorPrimary;
				try (TypedArray typedArray = context.obtainStyledAttributes(typedValue.data, new int[]{R.attr.colorPrimary})) {
					colorPrimary = typedArray.getColor(0, 0);
				}

				spannableString.setSpan(new ForegroundColorSpan(colorPrimary), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

				binding.packName.setText(spannableString);
			} else {
				binding.packName.setText(name);
			}
			binding.packAuthor.setText(author);

			ImageView[] iconViews = {binding.icon1, binding.icon2, binding.icon3, binding.icon4,
					binding.icon5, binding.icon6, binding.icon7, binding.icon8};

			fillIcons(iconViews, pack);

			binding.container.setOnClickListener(v -> {
				Log.d(TAG, "bind: " + pack.mName + " | " + mPackUtil.getEnabledState(pack));
				handleStateChange(pack, mPackUtil.getEnabledState(pack));
			});

			PopupMenu popupMenu = new PopupMenu(context, binding.container, Gravity.END);
			MenuInflater inflater = popupMenu.getMenuInflater();
			inflater.inflate(R.menu.icon_pack_item_menu, popupMenu.getMenu());

			popupMenu.setOnMenuItemClickListener(item -> {
				switch (item.getItemId()) {
					case R.id.app_info -> {
						Intent intent = new Intent();
						intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
						Uri uri = Uri.fromParts("package", pack.mPackageName, null);
						intent.setData(uri);
						context.startActivity(intent);
					}
					case R.id.show_icons -> showIconDialog(pack);
				}

				return true;
			});

			binding.container.setOnLongClickListener(v -> {
				popupMenu.show();
				return true;
			});
		}

		private void fillIcons(ImageView[] iconViews, IconPackUtil.IconPack pack) {
			List<Drawable> drawables = pack.getPackDrawables(itemView.getContext());
			for(int i = 0; i < Math.min(drawables.size(), iconViews.length); i++)
			{
				iconViews[i].setImageDrawable(drawables.get(i));
			}
		}

		private void showIconDialog(IconPackUtil.IconPack iconPack) {
			MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(itemView.getContext());
			builder.setTitle(iconPack.mName);
			ViewRecyclerIconsBinding dialogBinding = ViewRecyclerIconsBinding.inflate(LayoutInflater.from(itemView.getContext()));
			GridLayoutManager gridLayout = new GridLayoutManager(itemView.getContext(), 4);
			dialogBinding.recyclerIcons.setLayoutManager(gridLayout);
			dialogBinding.recyclerIcons.setAdapter(new IconAdapter(mPackUtil, iconPack));
			builder.setView(dialogBinding.getRoot());
			builder.show();

		}

		/**
		 * Handle the state change of the icon pack
		 * ENABLED ==> DISABLED
		 * DISABLED ==> ENABLED
		 * PARTIALLY ==> DISABLED
		 * @param pack The {@link IconPackUtil.IconPack} to change the state of
		 * @param packState The current state of the icon pack {@link IconPackUtil#ENABLED_FULL}
		 */
		private void handleStateChange(IconPackUtil.IconPack pack, int packState) {
			switch(packState) {
				case IconPackUtil.ENABLED_FULL:
				case IconPackUtil.ENABLED_PARTIAL:
					mPackUtil.disable(pack);
					break;
				case IconPackUtil.DISABLED:
					mPackUtil.enable(pack);
					break;
			}

			for(int i = 0; i < mFilteredPacks.size(); i++) {
				notifyItemChanged(i);
			}

			if (mItemChangedListener != null) {
				mItemChangedListener.onItemChanged();
			}
		}

	}

	private int getColorWithHalfOpacity(int color) {
		int red = Color.red(color);
		int green = Color.green(color);
		int blue = Color.blue(color);
		int alpha = (int) (255 * 0.5);

		return Color.argb(alpha, red, green, blue);
	}

	public final int VIEW_TYPE_SINGLE = 0;
	public final int VIEW_TYPE_TOP = 1;
	public final int VIEW_TYPE_MIDDLE = 2;
	public final int VIEW_TYPE_BOTTOM = 3;
}
