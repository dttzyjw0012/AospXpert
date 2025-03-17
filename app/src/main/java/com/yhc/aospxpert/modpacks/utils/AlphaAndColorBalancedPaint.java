package com.yhc.aospxpert.modpacks.utils;

import static com.yhc.aospxpert.modpacks.utils.toolkit.ColorUtils.setColorBoldness;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;

/** When setting a paint's color, alpha gets reset... naturally. So this is kind of paint that remembers its alpha and keeps it intact
 */
public class AlphaAndColorBalancedPaint extends Paint {
	public static final PathEffect DASH_PATH_EFFECT = new DashPathEffect(new float[]{3f, 2f}, 0f);
	public static final float DASH_PATH_EFFECT_BOLDNESS_FACTOR = 1.1f;

	int mAlpha = super.getAlpha();
	int mColor = super.getColor();
	public AlphaAndColorBalancedPaint(int flag) {
		super(flag);
	}

	@Override
	public void setAlpha(int alpha)
	{
		mAlpha = alpha;
		refreshColor();
	}

	@Override
	public void setColor(int color)
	{
		mColor = color;
		refreshColor();
	}

	private void refreshColor() {
		int color = DASH_PATH_EFFECT.equals(getPathEffect())
				? setColorBoldness(mColor, DASH_PATH_EFFECT_BOLDNESS_FACTOR)
				: mColor;

		super.setColor(
				Color.argb(combinedAlpha(mAlpha, Color.alpha(color)),
						Color.red(color),
						Color.green(color),
						Color.blue(color)));
	}

	private int combinedAlpha(int alpha1, int alpha2)
	{
		return Math.round((alpha1 / 255f) * (alpha2 / 255f) * 255f);
	}
}
