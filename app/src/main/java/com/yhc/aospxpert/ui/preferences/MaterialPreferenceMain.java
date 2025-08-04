package com.yhc.aospxpert.ui.preferences;

import static com.yhc.aospxpert.ui.preferences.Utils.setBackgroundResource;
import static com.yhc.aospxpert.ui.preferences.Utils.setFirstAndLastItemMargin;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceViewHolder;

import java.util.ArrayList;
import java.util.List;

import com.yhc.aospxpert.R;
import com.yhc.aospxpert.utils.PreferenceHelper;

public class MaterialPreferenceMain extends Preference {

	public MaterialPreferenceMain(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		initResource();
	}

	public MaterialPreferenceMain(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initResource();
	}

	public MaterialPreferenceMain(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		initResource();
	}

	public MaterialPreferenceMain(@NonNull Context context) {
		super(context);
		initResource();
	}

	private void initResource() {
		setLayoutResource(R.layout.custom_preference_main);
	}

	@Override
	public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
		super.onBindViewHolder(holder);

		setFirstAndLastItemMargin(holder);
		setBackgroundResource(this, holder);
		setIconColor(this, holder);
	}

	private void setIconColor(Preference preference, PreferenceViewHolder holder) {
		List<Preference> visiblePreferences = new ArrayList<>();

		PreferenceGroup screen = preference.getPreferenceManager().getPreferenceScreen();

		collectVisiblePreferences(screen, visiblePreferences);

		int position = visiblePreferences.indexOf(preference);

		if (position == -1) return;

		Pair<Integer, Integer> colors = getColors(position);

		ImageView iconView = (ImageView) holder.findViewById(android.R.id.icon);
		iconView.getBackground().setTintList(ColorStateList.valueOf(getContext().getColor(colors.first)));
		iconView.getDrawable().setTintList(ColorStateList.valueOf(getContext().getColor(colors.second)));
	}

	private void collectVisiblePreferences(PreferenceGroup group, List<Preference> outList) {
		for (int i = 0; i < group.getPreferenceCount(); i++) {
			Preference pref = group.getPreference(i);

			if (pref instanceof PreferenceGroup) {
				collectVisiblePreferences((PreferenceGroup) pref, outList);
			} else if (pref.getKey() != null &&
					PreferenceHelper.isVisible(pref.getKey()) &&
					pref instanceof MaterialPreferenceMain) {
				outList.add(pref);
			}
		}
	}

	private Pair<Integer, Integer> getColors(int index) {
		return switch (index % 10) {
			case 0 ->
					new Pair<>(R.color.main_preference_color_1, R.color.main_preference_on_color_1);
			case 1 ->
					new Pair<>(R.color.main_preference_color_2, R.color.main_preference_on_color_2);
			case 2 ->
					new Pair<>(R.color.main_preference_color_3, R.color.main_preference_on_color_3);
			case 3 ->
					new Pair<>(R.color.main_preference_color_4, R.color.main_preference_on_color_4);
			case 4 ->
					new Pair<>(R.color.main_preference_color_5, R.color.main_preference_on_color_5);
			case 5 ->
					new Pair<>(R.color.main_preference_color_6, R.color.main_preference_on_color_6);
			case 6 ->
					new Pair<>(R.color.main_preference_color_7, R.color.main_preference_on_color_7);
			case 7 ->
					new Pair<>(R.color.main_preference_color_8, R.color.main_preference_on_color_8);
			case 8 ->
					new Pair<>(R.color.main_preference_color_9, R.color.main_preference_on_color_9);
			default ->
					new Pair<>(R.color.main_preference_color_10, R.color.main_preference_on_color_10);
		};
	}
}