package com.yhc.aospxpert.modpacks.android;

import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;
import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.view.DisplayCutout;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.XPLauncher;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;


//We are playing in system framework. should be extra cautious..... many try-catchs, still not enough!
@SuppressWarnings("RedundantThrows")
public class StatusbarSize extends XposedModPack {
	private final List<String> listenPacks = new ArrayList<>();

	private static final int BOUNDS_POSITION_TOP = 1;

	static int sizeFactor = 100; // % of normal
	static boolean noCutoutEnabled = true;
	int currentHeight = 0;
	boolean edited = false; //if we touched it once during this instance, we'll have to continue setting it even if it's the original value
	private boolean mForceApplyHeight = false;

	public StatusbarSize(Context context) {
		super(context);
		listenPacks.add(Constants.SYSTEM_FRAMEWORK_PACKAGE);
		listenPacks.add(Constants.SYSTEM_UI_PACKAGE);
	}

	@SuppressLint({"DiscouragedApi", "InternalInsetResource"})
	@Override
	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;

		noCutoutEnabled = Xprefs.getBoolean("noCutoutEnabled", false);

		mForceApplyHeight = Xprefs.getBoolean("allScreenRotations", false) //Particularly used for rotation Status bar
				|| noCutoutEnabled
				|| Xprefs.getBoolean("systemIconsMultiRow", false)
				|| Xprefs.getBoolean("notificationAreaMultiRow", false);

		sizeFactor = Xprefs.getSliderInt("statusbarHeightFactor", 100);
		if (sizeFactor != 100 || edited || mForceApplyHeight) {
			Configuration conf = new Configuration();
			conf.updateFrom(mContext.getResources().getConfiguration());

			conf.orientation = Configuration.ORIENTATION_PORTRAIT;
			Context portraitContext = mContext.createConfigurationContext(conf);

			currentHeight = Math.round(
					portraitContext.getResources().getDimensionPixelSize(
							portraitContext.getResources().getIdentifier(
									"status_bar_height",
									"dimen",
									"android")
					)
							* sizeFactor
							/ 100f);
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPacks.contains(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		try {
			try {
				ReflectedClass WmDisplayCutoutClass = ReflectedClass.of("com.android.server.wm.utils.WmDisplayCutout");
				ReflectedClass DisplayCutoutClass = ReflectedClass.of("android.view.DisplayCutout");

				Object NO_CUTOUT = getStaticObjectField(DisplayCutoutClass.getClazz(), "NO_CUTOUT");

				WmDisplayCutoutClass
						.before("getDisplayCutout")
						.run(param -> {
							if (noCutoutEnabled) {
								param.setResult(NO_CUTOUT);
							}
						});

				WmDisplayCutoutClass
						.after("getDisplayCutout")
						.run(param -> {
							if (sizeFactor >= 100 && !edited) return;

							DisplayCutout displayCutout = (DisplayCutout) param.getResult();

							Rect boundTop = ((Rect[]) getObjectField(
									getObjectField(
											displayCutout,
											"mBounds"),
									"mRects")
							)[BOUNDS_POSITION_TOP];
							boundTop.bottom = Math.min(boundTop.bottom, currentHeight);

							Rect mSafeInsets = (Rect) getObjectField(
									displayCutout,
									"mSafeInsets");
							mSafeInsets.top = Math.min(mSafeInsets.top, currentHeight);
						});
			} catch (Throwable ignored) {
			}

			ReflectedClass.ReflectionConsumer resizedResultConsumer = param -> {
				try {
					if (sizeFactor == 100 && !edited && !mForceApplyHeight) return;
					edited = true;
					param.setResult(currentHeight);
				} catch (Throwable ignored) {
				}
			};

			try {
				ReflectedClass SystemBarUtilsClass = ReflectedClass.of("com.android.internal.policy.SystemBarUtils");

				SystemBarUtilsClass.before("getStatusBarHeight").run(resizedResultConsumer);
				SystemBarUtilsClass.before("getStatusBarHeightForRotation").run(resizedResultConsumer);
			} catch (Throwable ignored) {
			}
		} catch (Throwable ignored) {
		}
	}
}
