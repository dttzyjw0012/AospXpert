package com.yhc.aospxpert.utils;

import com.yhc.aospxpert.AOSPXpert;

public class DisplayUtils {
	public static boolean isTablet() {
		return AOSPXpert.get().getResources().getConfiguration().smallestScreenWidthDp >= 600;
	}
}