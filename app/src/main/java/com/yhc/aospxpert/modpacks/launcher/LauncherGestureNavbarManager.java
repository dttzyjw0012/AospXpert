package com.yhc.aospxpert.modpacks.launcher;

import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;

import java.util.Arrays;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.SystemUtils;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;

/**
 * @noinspection RedundantThrows
 */
public class LauncherGestureNavbarManager extends XposedModPack {
	private static final String listenPackage = Constants.LAUNCHER_PACKAGE;

	private static boolean navPillColorAccent = false;
	private static float widthFactor = 1f;
	private static int GesPillHeightFactor = 100;

	private boolean mColorReplaced = false;
	private int mStashedHandleLightColor;
	private int mStashedHandleDarkColor;
	private boolean mIsHooked = false;

	public LauncherGestureNavbarManager(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		navPillColorAccent = Xprefs.getBoolean("navPillColorAccent", false);

		widthFactor = Xprefs.getSliderInt("GesPillWidthModPos", 50) * .02f;
		GesPillHeightFactor = Xprefs.getSliderInt("GesPillHeightFactor", 100);

		if (Xprefs.getBoolean("HideNavbar", false)) {
			widthFactor = 0f;
		}

		if (mIsHooked && Key.length > 0 && Arrays.asList(
				"GesPillWidthModPos",
				"GesPillHeightFactor",
				"HideNavbar").contains(Key[0])) {
			SystemUtils.doubleToggleDarkMode();
		}
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectedClass StashedHandleViewClass = ReflectedClass.ofIfPossible("com.android.launcher3.taskbar.StashedHandleView");

		if (StashedHandleViewClass.getClazz() == null) return; //It's an older version

		mIsHooked = true;

		ReflectedClass StashedHandleViewControllerClass = ReflectedClass.of("com.android.launcher3.taskbar.StashedHandleViewController");

		StashedHandleViewControllerClass
				.afterConstruction()
				.run(param -> setAdditionalInstanceField(param.thisObject, "OriginalStashedHandleHeight", getObjectField(param.thisObject, "mStashedHandleHeight")));

		StashedHandleViewControllerClass
				.before("init")
				.run(param ->
						setObjectField(param.thisObject,
								"mStashedHandleHeight",
								Math.round(
										(int) getAdditionalInstanceField(
												param.thisObject,
												"OriginalStashedHandleHeight")
												* GesPillHeightFactor / 100f)));

		StashedHandleViewControllerClass
				.after("init")
				.run(param ->
						setObjectField(param.thisObject,
								"mStashedHandleWidth",
								Math.round(widthFactor * getIntField(param.thisObject, "mStashedHandleWidth"))));


		StashedHandleViewClass
				.afterConstruction()
				.run(param -> {
					mStashedHandleLightColor = (int) getObjectField(param.thisObject, "mStashedHandleLightColor");
					mStashedHandleDarkColor = (int) getObjectField(param.thisObject, "mStashedHandleDarkColor");
				});


		StashedHandleViewClass
				.before("updateHandleColor")
				.run(param -> {
					if (navPillColorAccent || mColorReplaced) {
						setObjectField(param.thisObject, "mStashedHandleLightColor", (navPillColorAccent) ? mContext.getResources().getColor(android.R.color.system_accent1_200, mContext.getTheme()) : mStashedHandleLightColor);
						setObjectField(param.thisObject, "mStashedHandleDarkColor", (navPillColorAccent) ? mContext.getResources().getColor(android.R.color.system_accent1_600, mContext.getTheme()) : mStashedHandleDarkColor);
						mColorReplaced = true;
					}
				});
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}
}