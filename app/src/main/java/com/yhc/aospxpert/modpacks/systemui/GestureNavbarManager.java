package com.yhc.aospxpert.modpacks.systemui;

import static android.view.MotionEvent.ACTION_DOWN;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getFloatField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.concurrent.atomic.AtomicInteger;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.XPLauncher;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.launcher.TaskbarActivator;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;

@SuppressWarnings("RedundantThrows")
public class GestureNavbarManager extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	//region Back gesture
	private static float backGestureHeightFractionLeft = 1f; // % of screen height. can be anything between 0 to 1
	private static float backGestureHeightFractionRight = 1f; // % of screen height. can be anything between 0 to 1
	private static boolean leftEnabled = true;
	private static boolean rightEnabled = true;
	float initialBackX = 0;

	Object EdgeBackGestureHandler;
	//endregion

	//region pill size
	private static int GesPillHeightFactor = 100;
	private static float widthFactor = 1f;

	private Object mNavigationBarInflaterView = null;
	//endregion

	//region pill color
	private boolean colorReplaced = false;
	private static boolean navPillColorAccent = false;
	private static final int mLightColor = 0xEBFFFFFF, mDarkColor = 0x99000000; //original navbar colors
	//endregion

	public GestureNavbarManager(Context context) {
		super(context);
	}

	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;

		//region Back gesture
		leftEnabled = Xprefs.getBoolean("BackFromLeft", true);
		rightEnabled = Xprefs.getBoolean("BackFromRight", true);
		backGestureHeightFractionLeft = Xprefs.getSliderInt( "BackLeftHeight", 100) / 100f;
		backGestureHeightFractionRight = Xprefs.getSliderInt( "BackRightHeight", 100) / 100f;
		//endregion

		//region pill size
		widthFactor = Xprefs.getSliderInt( "GesPillWidthModPos", 50) * .02f;
		GesPillHeightFactor = Xprefs.getSliderInt( "GesPillHeightFactor", 100);

		int taskbarMode = TaskbarActivator.TASKBAR_DEFAULT;
		String taskbarModeStr = Xprefs.getString("taskBarMode", "0");
		try {
			taskbarMode = Integer.parseInt(taskbarModeStr);
		} catch (Exception ignored) {
		}

		if (taskbarMode == TaskbarActivator.TASKBAR_ON || Xprefs.getBoolean("HideNavbar", false)) {
			widthFactor = 0f;
		}

		if (Key.length > 0) {
			refreshNavbar();
		}
		//endregion

		//region pill color
		navPillColorAccent = Xprefs.getBoolean("navPillColorAccent", false);
		//endregion
	}

	//region pill size
	private void refreshNavbar() {
		try {
			callMethod(mNavigationBarInflaterView, "onFinishInflate");
		} catch (Throwable ignored) {}
	}
	//endregion

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		if (!lpParam.packageName.equals(listenPackage)) return;

		ReflectedClass NavigationHandleClass = ReflectedClass.of("com.android.systemui.navigationbar.gestural.NavigationHandle");
		ReflectedClass EdgeBackGestureHandlerClass = ReflectedClass.ofIfPossible("com.android.systemui.navigationbar.gestural.EdgeBackGestureHandler");
		ReflectedClass NavigationBarEdgePanelClass = ReflectedClass.ofIfPossible("com.android.systemui.navigationbar.gestural.NavigationBarEdgePanel");
		ReflectedClass BackPanelControllerClass = ReflectedClass.of("com.android.systemui.navigationbar.gestural.BackPanelController");

		ReflectedClass NavigationBarInflaterViewClass = ReflectedClass.ofIfPossible("com.android.systemui.navigationbar.views.NavigationBarInflaterView");
		if(NavigationBarInflaterViewClass.getClazz() == null)
		{
			NavigationBarInflaterViewClass = ReflectedClass.ofIfPossible("com.android.systemui.navigationbar.NavigationBarInflaterView");
		}


		//region back gesture
		//A14
		EdgeBackGestureHandlerClass
				.afterConstruction()
				.run(param -> EdgeBackGestureHandler = param.thisObject);

		BackPanelControllerClass
				.before("onMotionEvent")
				.run(param -> {
					MotionEvent ev = (MotionEvent) param.args[0];

					if(ev.getActionMasked() == ACTION_DOWN) //down action is enough. once gesture is refused it won't accept further actions
					{
						if(notWithinInsets(ev.getX(),
								ev.getY(),
								(Point) getObjectField(EdgeBackGestureHandler, "mDisplaySize"),
								getFloatField(EdgeBackGestureHandler, "mBottomGestureHeight")))
						{
							setObjectField(EdgeBackGestureHandler, "mAllowGesture", false); //act like the gesture was not good enough
							param.setResult(null); //and stop the current method too
						}
					}
				});

		//Android 13
		NavigationBarEdgePanelClass
				.before("onMotionEvent")
				.run(param -> {
					MotionEvent event = (MotionEvent) param.args[0];
					if(event.getAction() == ACTION_DOWN)
					{
						initialBackX = event.getX();
					}
					if (notWithinInsets(initialBackX, event.getY(), (Point) getObjectField(param.thisObject, "mDisplaySize"), 0)) {
						//event.setAction(MotionEvent.ACTION_CANCEL);
						param.setResult(null);
					}
				});
		//endregion

		//region pill color
		NavigationHandleClass
				.before("setDarkIntensity")
				.run(param -> {
					if (navPillColorAccent || colorReplaced) {
						setObjectField(param.thisObject, "mLightColor", (navPillColorAccent) ? mContext.getResources().getColor(android.R.color.system_accent1_200, mContext.getTheme()) : mLightColor);
						setObjectField(param.thisObject, "mDarkColor", (navPillColorAccent) ? mContext.getResources().getColor(android.R.color.system_accent1_600, mContext.getTheme()) : mDarkColor);
						colorReplaced = true;
					}
				});
		//endregion

		//region pill size
		NavigationHandleClass
				.before("setVertical")
				.run(param -> {
					if (widthFactor != 1f) {
						View result = (View) param.thisObject;
						ViewGroup.LayoutParams resultLayoutParams = result.getLayoutParams();
						int originalWidth;
						try {
							originalWidth = (int) getAdditionalInstanceField(param.thisObject, "originalWidth");
						} catch (Throwable ignored) {
							originalWidth = resultLayoutParams.width;
							setAdditionalInstanceField(param.thisObject, "originalWidth", originalWidth);
						}

						resultLayoutParams.width = Math.round(originalWidth * widthFactor);
					}
				});

		AtomicInteger mRadius = new AtomicInteger(0);

		NavigationHandleClass
				.before("onDraw")
				.run(param -> {
					if (GesPillHeightFactor != 100) {
						mRadius.set(Math.round((float) getObjectField(param.thisObject, "mRadius")));

						setObjectField(param.thisObject, "mRadius", Math.round(mRadius.get() * GesPillHeightFactor / 100f));
					}
				});

		NavigationHandleClass
				.after("onDraw")
				.run(param -> {
					if (mRadius.get() > 0) {
						setObjectField(param.thisObject, "mRadius", mRadius.get() * 1f);
					}
				});

		NavigationBarInflaterViewClass
				.afterConstruction()
				.run(param -> {
					mNavigationBarInflaterView = param.thisObject;
					refreshNavbar();
				});

		NavigationBarInflaterViewClass
				.after("createView")
				.run(param -> {
					if (widthFactor != 1f) {
						String button = (String) callMethod(param.thisObject, "extractButton", param.args[0]);
						if (!button.equals("home_handle")) return;

						View result = (View) param.getResult();
						ViewGroup.LayoutParams resultLayoutParams = result.getLayoutParams();
						resultLayoutParams.width = Math.round(resultLayoutParams.width * widthFactor);
						result.setLayoutParams(resultLayoutParams);
					}
				});
		//endregion

	}

	//region Back gesture
	private boolean notWithinInsets(float x, float y, Point mDisplaySize, float mBottomGestureHeight) {
		boolean isLeftSide = x < (mDisplaySize.x / 3f);
		if ((isLeftSide && !leftEnabled)
				|| (!isLeftSide && !rightEnabled)) {
			return true;
		}

		int mEdgeHeight = isLeftSide ?
				Math.round(mDisplaySize.y * backGestureHeightFractionLeft) :
				Math.round(mDisplaySize.y * backGestureHeightFractionRight);

		return mEdgeHeight != 0
				&& y < (mDisplaySize.y
				- mBottomGestureHeight
				- mEdgeHeight);
	}
	//endregion

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}