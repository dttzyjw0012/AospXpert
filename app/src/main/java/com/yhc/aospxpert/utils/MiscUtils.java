package com.yhc.aospxpert.utils;

import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.ColorInt;

import com.yhc.aospxpert.AOSPXpert;

public class MiscUtils {

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
}
