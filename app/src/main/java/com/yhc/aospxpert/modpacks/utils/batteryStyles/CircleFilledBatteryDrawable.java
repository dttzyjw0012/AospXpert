package com.yhc.aospxpert.modpacks.utils.batteryStyles;

import static android.graphics.Color.WHITE;
import static java.lang.Math.round;
import static com.yhc.aospxpert.modpacks.systemui.BatteryDataProvider.getCurrentLevel;
import static com.yhc.aospxpert.modpacks.systemui.BatteryDataProvider.isBatteryDefender;
import static com.yhc.aospxpert.modpacks.systemui.BatteryDataProvider.isCharging;
import static com.yhc.aospxpert.modpacks.systemui.BatteryDataProvider.isFastCharging;
import static com.yhc.aospxpert.modpacks.systemui.BatteryDataProvider.isPowerSaving;
import static com.yhc.aospxpert.modpacks.utils.toolkit.ColorUtils.getColorAttrDefaultColor;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.yhc.aospxpert.R;
import com.yhc.aospxpert.modpacks.ResourceManager;
import com.yhc.aospxpert.modpacks.utils.AlphaAndColorBalancedPaint;

public class CircleFilledBatteryDrawable extends BatteryDrawable {
	private static final int INTRINSIC_DIMENSION = 45;
	private final ValueAnimator mLevelAlphaAnimator;
	private final Context mContext;
	private boolean mChargingAnimationEnabled = true;
	private int mDimension = INTRINSIC_DIMENSION;
	private final Rect mPadding = new Rect();
	private int mFGColor = WHITE;
	private int mBGColor = WHITE;
	private int mAlpha = 255;
	private static int[] mShadeColors = null;
	private static float[] mShadeLevels = null;
	private final int mPowerSaveColor;
	@SuppressLint("DiscouragedApi")
	public CircleFilledBatteryDrawable(Context context) {
		super();
		mContext = context;

		mPowerSaveColor = getColorAttrDefaultColor(context, android.R.attr.colorError);

		mLevelAlphaAnimator = ValueAnimator.ofInt(255, 255, 255, 45);

		mLevelAlphaAnimator.setDuration(2000);
		mLevelAlphaAnimator.setInterpolator(new FastOutSlowInInterpolator());
		mLevelAlphaAnimator.setRepeatMode(ValueAnimator.REVERSE);
		mLevelAlphaAnimator.setRepeatCount(ValueAnimator.INFINITE);

		mLevelAlphaAnimator.addUpdateListener(valueAnimator -> invalidateSelf());
	}

	public CircleFilledBatteryDrawable(Context context, int frameColor) {
		this(context);
		mBGColor = frameColor;
	}

	@Override
	public int getIntrinsicHeight() {
		return INTRINSIC_DIMENSION;
	}

	@Override
	public int getIntrinsicWidth() {
		return INTRINSIC_DIMENSION;
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		Paint basePaint = new AlphaAndColorBalancedPaint(Paint.ANTI_ALIAS_FLAG);
		basePaint.setAlpha(round(80f * (mAlpha / 255f)));

		basePaint.setColor(mBGColor);

		Paint levelPaint = new AlphaAndColorBalancedPaint(Paint.ANTI_ALIAS_FLAG);

		float centerX = mDimension / 2f + mPadding.left;
		float centerY = mDimension / 2f + mPadding.top;

		float baseRadius = mDimension / 2f;

		float levelRadius = baseRadius * getCurrentLevel() / 100f;

		try {
			setLevelBasedColor(levelPaint, centerX, centerY, baseRadius);
		} catch (Throwable t) {
			levelPaint.setColor(Color.BLACK);
		}

		if(isCharging() && getCurrentLevel() < 100)
		{
			if(!mLevelAlphaAnimator.isStarted() && mChargingAnimationEnabled)
				mLevelAlphaAnimator.start();

			levelPaint.setAlpha(round(
					(mChargingAnimationEnabled
						? (int) mLevelAlphaAnimator.getAnimatedValue()
						: 255)
					* mAlpha/255f));
		}
		else
		{
			if (mLevelAlphaAnimator.isStarted())
				mLevelAlphaAnimator.end();

			levelPaint.setAlpha(mAlpha);
		}

		canvas.drawCircle(centerX, centerY, baseRadius, basePaint);
		canvas.drawCircle(centerY, centerY, levelRadius, levelPaint);

		if(isBatteryDefender())
		{
			Drawable defenderIcon = ResourcesCompat.getDrawable(ResourceManager.modRes, R.drawable.ic_battery_defender, mContext.getTheme());
			//noinspection DataFlowIssue
			defenderIcon.setBounds(new Rect(Math.round(centerX - baseRadius/2.2f), Math.round(centerY - baseRadius/2.2f), Math.round(centerX + baseRadius/2.2f), Math.round(centerY + baseRadius/2.2f)));
			defenderIcon.setTint(invertColor(mFGColor));
			defenderIcon.setAlpha(mAlpha);
			defenderIcon.draw(canvas);
		}
	}

	private void setLevelBasedColor(Paint paint, float cx, float cy, float baseRadius) {
		int singleColor = mFGColor;

		paint.setShader(null);
		if (isFastCharging() && showFastCharging && getCurrentLevel() < 100) {
			paint.setColor(fastChargingColor);
			return;
		} else if (isCharging() && showCharging && getCurrentLevel() < 100) {
			paint.setColor(chargingColor);
			return;
		} else if (isPowerSaving()) {
			paint.setColor(mPowerSaveColor);
			return;
		} else if (isBatteryDefender()) {
			paint.setColor(singleColor);
			return;
		}

		if (!colorful || mShadeColors == null) {
			for (int i = 0; i < batteryLevels.size(); i++) {
				if (getCurrentLevel() <= batteryLevels.get(i)) {
					if (transitColors && i > 0) {
						float range = batteryLevels.get(i) - batteryLevels.get(i - 1);
						float currentPos = getCurrentLevel() - batteryLevels.get(i - 1);
						float ratio = currentPos / range;
						singleColor = ColorUtils.blendARGB(batteryColors[i - 1], batteryColors[i], ratio);
					} else {
						singleColor = batteryColors[i];
					}
					break;
				}
			}
			paint.setColor(singleColor);
		} else {
			RadialGradient shader = new RadialGradient(cx, cy, baseRadius, mShadeColors, mShadeLevels, Shader.TileMode.CLAMP);
			paint.setShader(shader);
		}
	}

	@Override
	public void setAlpha(int alpha) {
		mAlpha = alpha;
	}

	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter) {
	}

	@Override
	public void setBounds(@NonNull Rect bounds) {
		super.setBounds(bounds);
		mDimension = Math.max((bounds.height() - mPadding.height()), (bounds.width() - mPadding.width()));
		invalidateSelf();
	}


	@Override
	public int getOpacity() {
		return PixelFormat.UNKNOWN;
	}

	@Override
	public void setShowPercent(boolean showPercent) { //not applicable
	}

	@Override
	public void setColors(int fgColor, int bgColor, int singleToneColor) {
		mFGColor = fgColor;
		mBGColor = bgColor;
		invalidateSelf();
	}

	@Override
	public void setChargingAnimationEnabled(boolean enabled) {
		mChargingAnimationEnabled = enabled;
	}

	public int invertColor(int color) {
		int red = 255 - Color.red(color);
		int green = 255 - Color.green(color);
		int blue = 255 - Color.blue(color);

		return Color.rgb(red, green, blue);
	}

	@Override
	public void onColorsUpdated() {
		if (batteryColors == null || batteryLevels.isEmpty()) return;

		mShadeColors = new int[batteryLevels.size() * 2 + 2];

		mShadeLevels = new float[mShadeColors.length];
		float prev = 0;
		for (int i = 0; i < batteryLevels.size(); i++) {
			float rangeLength = batteryLevels.get(i) - prev;
			mShadeLevels[2 * i] = (prev + rangeLength * .3f) / 100;
			mShadeColors[2 * i] = batteryColors[i];

			mShadeLevels[2 * i + 1] = (batteryLevels.get(i) - rangeLength * .3f) / 100;
			mShadeColors[2 * i + 1] = batteryColors[i];

			prev = batteryLevels.get(i);
		}

		mShadeLevels[mShadeLevels.length - 2] = (batteryLevels.get(batteryLevels.size() - 1) + (100 - batteryLevels.get(batteryLevels.size() - 1)) * .3f) / 100;
		mShadeColors[mShadeColors.length - 2] = Color.GREEN;
		mShadeLevels[mShadeLevels.length - 1] = 1f;
		mShadeColors[mShadeColors.length - 1] = Color.GREEN;

		invalidateSelf();
	}
}