package com.yhc.aospxpert.modpacks.utils.toolkit;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;

import androidx.annotation.ColorInt;

public class ColorUtils {
	@ColorInt
	public static int getColorAttrDefaultColor(Context context, int attr) {
		return getColorAttrDefaultColor(context, attr, 0);
	}

	/**
	 * Get color styled attribute {@code attr}, default to {@code defValue} if not found.
	 */
	@ColorInt
	public static int getColorAttrDefaultColor(Context context, int attr, @ColorInt int defValue) {
		try(TypedArray ta = context.obtainStyledAttributes(new int[]{attr})) {
			@ColorInt int colorAccent = ta.getColor(0, defValue);
			return colorAccent;
		}
	}

	public static ColorStateList getColorAttr(Context context, int attr) {
		try(TypedArray ta = context.obtainStyledAttributes(new int[]{attr})) {
			//noinspection ReassignedVariable
			ColorStateList stateList = null;
			try {
				stateList = ta.getColorStateList(0);
			}
			catch (Throwable ignored){}
			return stateList;
		}
	}

	/** @noinspection unused*/
	public static int compositeAlpha(int foregroundAlpha, int backgroundAlpha) {
		return 0xFF - (((0xFF - backgroundAlpha) * (0xFF - foregroundAlpha)) / 0xFF);
	}

	public static int setColorBoldness(int color, float factor) {
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		hsv[2] = Math.min(hsv[2] * factor, 1.0f); // Increase the V (value) component
		return Color.HSVToColor(hsv);
	}
}
