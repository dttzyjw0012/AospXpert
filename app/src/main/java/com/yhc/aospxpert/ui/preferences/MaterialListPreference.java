package com.yhc.aospxpert.ui.preferences;

import static com.yhc.aospxpert.utils.MiscUtils.dpToPx;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceViewHolder;

import com.yhc.aospxpert.R;

public class MaterialListPreference extends ListPreference {

	public MaterialListPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		initResource();
	}

	public MaterialListPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initResource();
	}

	public MaterialListPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		initResource();
	}

	public MaterialListPreference(@NonNull Context context) {
		super(context);
		initResource();
	}

	private void initResource() {
		setLayoutResource(R.layout.custom_preference_list);
	}

	@Override
	public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
		super.onBindViewHolder(holder);

		if (holder.getBindingAdapterPosition() == 0) {
			ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
			layoutParams.topMargin = dpToPx(12);
			holder.itemView.setLayoutParams(layoutParams);
		} else {
			if (holder.getBindingAdapter() != null) {
				if (holder.getBindingAdapterPosition() == holder.getBindingAdapter().getItemCount() - 1) {
					ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
					layoutParams.bottomMargin = dpToPx(0);
					holder.itemView.setLayoutParams(layoutParams);
				}
			}
		}
	}
}
