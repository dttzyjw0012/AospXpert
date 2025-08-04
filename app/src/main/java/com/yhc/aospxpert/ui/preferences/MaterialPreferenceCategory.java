package com.yhc.aospxpert.ui.preferences;

import static com.yhc.aospxpert.utils.MiscUtils.dpToPx;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceViewHolder;

import com.yhc.aospxpert.R;
import com.yhc.aospxpert.utils.PreferenceHelper;

public class MaterialPreferenceCategory extends PreferenceCategory {

	public MaterialPreferenceCategory(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		initResource();
	}

	public MaterialPreferenceCategory(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initResource();
	}

	public MaterialPreferenceCategory(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		initResource();
	}

	public MaterialPreferenceCategory(@NonNull Context context) {
		super(context);
		initResource();
	}

	private void initResource() {
		setLayoutResource(R.layout.custom_preference_category);
	}

	@Override
	public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
		super.onBindViewHolder(holder);

		holder.setDividerAllowedAbove(false);
		holder.setDividerAllowedBelow(false);

		if (getKey() == null || PreferenceHelper.isVisible(getKey())) {
			PreferenceGroup parent = getParent();
			ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();

			if (parent != null && holder.getBindingAdapterPosition() == 1 && parent.getPreference(0) instanceof IllustrationPreference) {
				layoutParams.topMargin = dpToPx(12);
				setMinHeight(true, holder.itemView);
			} else if (holder.getBindingAdapterPosition() == 0) {
				layoutParams.topMargin = dpToPx(12);
				setMinHeight(true, holder.itemView);
			} else {
				if (holder.getBindingAdapter() != null) {
					if (holder.getBindingAdapterPosition() == holder.getBindingAdapter().getItemCount() - 1) {
						layoutParams.bottomMargin = dpToPx(0);
					}
				}
				layoutParams.topMargin = 0;
				setMinHeight(false, holder.itemView);
			}

			holder.itemView.setLayoutParams(layoutParams);
		}
	}

	private void setMinHeight(boolean zero, View view) {
		if (zero) {
			view.setMinimumHeight(0);
		} else {
			TypedValue typedValue = new TypedValue();
			if (getContext().getTheme().resolveAttribute(android.R.attr.listPreferredItemHeight, typedValue, true)) {
				int minHeight = TypedValue.complexToDimensionPixelSize(typedValue.data, getContext().getResources().getDisplayMetrics());
				view.setMinimumHeight(minHeight);
			}
		}
	}
}