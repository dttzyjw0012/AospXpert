package com.yhc.aospxpert.ui.preferences;

import static com.yhc.aospxpert.ui.preferences.Utils.setBackgroundResource;
import static com.yhc.aospxpert.ui.preferences.Utils.setFirstAndLastItemMargin;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceViewHolder;

import com.yhc.aospxpert.R;
import com.yhc.rangesliderpreference.RangeSliderPreference;

public class MaterialRangeSliderPreference extends RangeSliderPreference {

	public MaterialRangeSliderPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		initResource();
	}
	@Override
	public void onBindViewHolder(@NonNull PreferenceViewHolder holder)
	{
		super.onBindViewHolder(holder);

		holder.setDividerAllowedAbove(false);
		holder.setDividerAllowedBelow(false);

		setFirstAndLastItemMargin(holder);
		setBackgroundResource(this, holder);
	}

	private void initResource() {
		setLayoutResource(R.layout.custom_preference_range_slider);
	}
}
