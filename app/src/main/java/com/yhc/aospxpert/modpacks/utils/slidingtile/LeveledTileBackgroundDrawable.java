package com.yhc.aospxpert.modpacks.utils.slidingtile;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

public class LeveledTileBackgroundDrawable extends LayerDrawable {
	private final LeveledTileIndicatorDrawable mPCTLayer;

	public LeveledTileBackgroundDrawable(Context context, Drawable lowerLayer) {
		super(new Drawable[]{lowerLayer});

		mPCTLayer = new LeveledTileIndicatorDrawable(context);
		mPCTLayer.setAlpha(64);
		addLayer(mPCTLayer);
	}

	public void setLevelTint(int tint)
	{
		mPCTLayer.setTint(tint);
	}

	public void setPct(int pct)
	{
		mPCTLayer.setPct(pct);
	}
}
