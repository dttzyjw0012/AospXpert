package com.yhc.aospxpert.ui.preferences;

import static com.yhc.aospxpert.ui.preferences.Utils.setBackgroundResource;
import static com.yhc.aospxpert.ui.preferences.Utils.setFirstAndLastItemMargin;
import static com.yhc.aospxpert.utils.MiscUtils.dpToPx;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.PreferenceViewHolder;

import com.yhc.aospxpert.R;

public class MaterialMultiSelectListPreference extends MultiSelectListPreference {

    public MaterialMultiSelectListPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initResource();
    }

    public MaterialMultiSelectListPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initResource();
    }

    public MaterialMultiSelectListPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initResource();
    }

    public MaterialMultiSelectListPreference(@NonNull Context context) {
        super(context);
        initResource();
    }

    private void initResource() {
        setLayoutResource(R.layout.custom_preference_list);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        setFirstAndLastItemMargin(holder);
        setBackgroundResource(this, holder);
    }
}
