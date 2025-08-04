package com.yhc.aospxpert.ui.preferences;

import static com.yhc.aospxpert.ui.preferences.Utils.setBackgroundResource;
import static com.yhc.aospxpert.ui.preferences.Utils.setFirstAndLastItemMargin;
import static com.yhc.aospxpert.utils.MiscUtils.dpToPx;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceViewHolder;

import com.yhc.aospxpert.R;

public class MaterialEditTextPreference extends EditTextPreference {

	public MaterialEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		initResource();
	}

	public MaterialEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initResource();
	}

	public MaterialEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		initResource();
	}

	public MaterialEditTextPreference(@NonNull Context context) {
		super(context);
		initResource();
	}

	private void initResource() {
		setLayoutResource(R.layout.custom_preference_edit_text);
	}

	@Override
	public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
		super.onBindViewHolder(holder);

		setFirstAndLastItemMargin(holder);
		setBackgroundResource(this, holder);
	}
}
