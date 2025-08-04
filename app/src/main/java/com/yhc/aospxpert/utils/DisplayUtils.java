package com.yhc.aospxpert.utils;

import com.yhc.aospxpert.AospXpert;

public class DisplayUtils {
	public static boolean isTablet() {
		return AospXpert.get().getResources().getConfiguration().smallestScreenWidthDp >= 600;
	}
}