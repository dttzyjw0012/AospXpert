package com.yhc.aospxpert.modpacks.systemui;

import static android.graphics.Color.BLACK;
import static android.graphics.Paint.Style.FILL;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;
import static com.yhc.aospxpert.modpacks.utils.toolkit.ColorUtils.getColorAttrDefaultColor;
import static com.yhc.aospxpert.modpacks.utils.toolkit.OverlayTools.setOverlay;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;

import java.util.Arrays;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.XPLauncher;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.SystemUtils;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;

@SuppressWarnings("RedundantThrows")
public class ThemeManager_13 extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;
	public static final int STATE_ACTIVE = 2;
	private static boolean lightQSHeaderEnabled = false;
	private static boolean enablePowerMenuTheme = false;
	private static boolean brightnessThickTrackEnabled = false;
	private boolean isDark;
	private Integer colorInactive = null;
	private int colorUnavailable;
	private int colorActive;
	private Drawable lightFooterShape = null;
	private Object mClockViewQSHeader;
	private int mScrimBehindTint = BLACK;
	private Object unlockedScrimState;

	public ThemeManager_13(Context context) {
		super(context);
		if (!listensTo(context.getPackageName())) return;

		lightFooterShape = getFooterShape(context);
		isDark = isDarkMode();
	}

	@Override
	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;

		enablePowerMenuTheme = Xprefs.getBoolean("enablePowerMenuTheme", false);
		setLightQSHeader(Xprefs.getBoolean("LightQSPanel", false));
		boolean newbrightnessThickTrackEnabled = Xprefs.getBoolean("BSThickTrackOverlay", false);
		if (newbrightnessThickTrackEnabled != brightnessThickTrackEnabled) {
			brightnessThickTrackEnabled = newbrightnessThickTrackEnabled;

			applyOverlays(true);
		}

		try
		{
			if(Key[0].equals("LightQSPanel")) {
				//Application of Light QS usually only needs a screen off/on. but some users get confused. Let's restart systemUI and get it over with
				//This has to happen AFTER overlays are applied. So we do it after update operations are done
				SystemUtils.killSelf();
			}
		}
		catch (Throwable ignored){}
	}

	public void setLightQSHeader(boolean state) {
		if (lightQSHeaderEnabled != state) {
			lightQSHeaderEnabled = state;

			applyOverlays(true);
		}
	}

	@SuppressLint("DiscouragedApi")
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) {
		if (!lpParam.packageName.equals(listenPackage)) return;

		ReflectedClass QSTileViewImplClass = ReflectedClass.of("com.android.systemui.qs.tileimpl.QSTileViewImpl");
		ReflectedClass ScrimControllerClass = ReflectedClass.of("com.android.systemui.statusbar.phone.ScrimController");
		ReflectedClass QSPanelControllerClass = ReflectedClass.of("com.android.systemui.qs.QSPanelController");
		ReflectedClass ScrimStateEnum = ReflectedClass.of("com.android.systemui.statusbar.phone.ScrimState");
		ReflectedClass QSIconViewImplClass = ReflectedClass.of("com.android.systemui.qs.tileimpl.QSIconViewImpl");
		ReflectedClass CentralSurfacesImplClass = ReflectedClass.of("com.android.systemui.statusbar.phone.CentralSurfacesImpl");
		ReflectedClass ClockClass = ReflectedClass.of("com.android.systemui.statusbar.policy.Clock");
		ReflectedClass QuickStatusBarHeaderClass = ReflectedClass.of("com.android.systemui.qs.QuickStatusBarHeader");
		ReflectedClass BrightnessSliderViewClass = ReflectedClass.of("com.android.systemui.settings.brightness.BrightnessSliderView");
		ReflectedClass GlobalActionsDialogLiteSinglePressActionClass = ReflectedClass.of("com.android.systemui.globalactions.GlobalActionsDialogLite$SinglePressAction");
		ReflectedClass GlobalActionsDialogLiteEmergencyActionClass = ReflectedClass.of("com.android.systemui.globalactions.GlobalActionsDialogLite$EmergencyAction");
		ReflectedClass GlobalActionsLayoutLiteClass = ReflectedClass.of("com.android.systemui.globalactions.GlobalActionsLayoutLite");

		try {
			ReflectedClass BatteryStatusChipClass = ReflectedClass.of("com.android.systemui.statusbar.BatteryStatusChip");

			BatteryStatusChipClass
					.after("updateResources")
					.run(param -> {
						if (lightQSHeaderEnabled && !isDark)
							((LinearLayout) getObjectField(param.thisObject, "roundedContainer"))
									.getBackground()
									.setTint(colorActive);
					});
		}
		catch (Throwable ignored){} //Android 13 or something

		BrightnessSliderViewClass
				.after("onFinishInflate")
				.run(param -> {
					if(!lightQSHeaderEnabled) return;

					((LayerDrawable) callMethod(
							getObjectField(param.thisObject, "mSlider")
							,"getProgressDrawable"))
							.findDrawableByLayerId(android.R.id.background)
							.setTint(Color.GRAY);
				});

		//noinspection OptionalGetWithoutIsPresent
		unlockedScrimState = Arrays.stream(ScrimStateEnum.getClazz().getEnumConstants()).filter(c -> c.toString().equals("UNLOCKED")).findFirst().get();

		ReflectedClass.of(unlockedScrimState.getClass())
				.after("prepare")
				.run(param -> {
					if(!lightQSHeaderEnabled) return;

					setObjectField(unlockedScrimState, "mBehindTint", mScrimBehindTint);
				});

		QSPanelControllerClass
				.afterConstruction()
				.run(param -> calculateColors());

		try { //13QPR1
			ReflectedClass QSSecurityFooterClass = ReflectedClass.of("com.android.systemui.qs.QSSecurityFooter");
			ReflectedClass QSFgsManagerFooterClass = ReflectedClass.of("com.android.systemui.qs.QSFgsManagerFooter");
			ReflectedClass FooterActionsControllerClass = ReflectedClass.of("com.android.systemui.qs.FooterActionsController");

			QSFgsManagerFooterClass
					.afterConstruction()
					.run(param -> {
						if (!isDark && lightQSHeaderEnabled) {
							((View) getObjectField(param.thisObject, "mNumberContainer"))
									.getBackground()
									.setTint(colorInactive);
							((View) getObjectField(param.thisObject, "mTextContainer"))
									.setBackground(lightFooterShape.getConstantState().newDrawable().mutate()); //original has to be copied to new object otherwise will get affected by view changes
						}
					});

			QSSecurityFooterClass
					.afterConstruction()
					.run(param -> {
						if (!isDark && lightQSHeaderEnabled) {
							((View) getObjectField(param.thisObject, "mView")).setBackground(lightFooterShape.getConstantState().newDrawable().mutate());
						}
					});

			FooterActionsControllerClass
					.afterConstruction()
					.run(param -> {
						if (!isDark && lightQSHeaderEnabled) {
							Resources res = mContext.getResources();
							ViewGroup view = (ViewGroup) param.args[0];

							view.findViewById(res.getIdentifier("multi_user_switch", "id", mContext.getPackageName())).getBackground().setTint(colorInactive);

							View settings_button_container = view.findViewById(res.getIdentifier("settings_button_container", "id", mContext.getPackageName()));
							settings_button_container.getBackground().setTint(colorInactive);

							((LinearLayout.LayoutParams)
									((ViewGroup) settings_button_container
											.getParent()
									).getLayoutParams()
							).setMarginEnd(0);
						}
					});

			//White QS Clock bug - doesn't seem applicable on 13QPR3 and 14
			QuickStatusBarHeaderClass
					.after("onFinishInflate")
					.run(param -> {
						View thisView = (View) param.thisObject;
						Resources res = mContext.getResources();

						mClockViewQSHeader = thisView.findViewById(res.getIdentifier("clock", "id", mContext.getPackageName()));
					});

			//White QS Clock bug - doesn't seem applicable on 13QPR3 and 14
			ClockClass
					.after("onColorsChanged")
					.run(param -> {
						if(lightQSHeaderEnabled && isDark)
						{
							((TextView)mClockViewQSHeader).setTextColor(Color.BLACK);
						}
					});

		}catch (Throwable ignored){ //13QPR2&3
			//QPR3
			ReflectedClass QSContainerImplClass = ReflectedClass.of("com.android.systemui.qs.QSContainerImpl");
			ReflectedClass ShadeHeaderControllerClass = ReflectedClass.ofIfPossible("com.android.systemui.shade.ShadeHeaderController");
			if(ShadeHeaderControllerClass.getClazz() == null) //13QPR2
			{
				ShadeHeaderControllerClass = ReflectedClass.of("com.android.systemui.shade.LargeScreenShadeHeaderController");
			}

			ShadeHeaderControllerClass
					.after("onInit")
					.run(param -> {
						View mView = (View) getObjectField(param.thisObject, "mView");

						Object iconManager = getObjectField(param.thisObject, "iconManager");
						Object batteryIcon = getObjectField(param.thisObject, "batteryIcon");
						Object configurationControllerListener = getObjectField(param.thisObject, "configurationControllerListener");
						ReflectedClass.of(configurationControllerListener.getClass())
								.after("onConfigChanged")
								.run(param1 -> {
									if (!lightQSHeaderEnabled) return;

									int textColor = getColorAttrDefaultColor(mContext, android.R.attr.textColorPrimary);

									((TextView) mView.findViewById(mContext.getResources().getIdentifier("clock", "id", mContext.getPackageName()))).setTextColor(textColor);
									((TextView) mView.findViewById(mContext.getResources().getIdentifier("date", "id", mContext.getPackageName()))).setTextColor(textColor);

									callMethod(iconManager, "setTint", textColor);

									for(int i = 1; i <= 3; i ++) {
										String id = String.format("carrier%s", i);

										((TextView) getObjectField(mView.findViewById(mContext.getResources().getIdentifier(id, "id", mContext.getPackageName())), "mCarrierText")).setTextColor(textColor);
										((ImageView) getObjectField(mView.findViewById(mContext.getResources().getIdentifier(id, "id", mContext.getPackageName())), "mMobileSignal")).setImageTintList(ColorStateList.valueOf(textColor));
										((ImageView) getObjectField(mView.findViewById(mContext.getResources().getIdentifier(id, "id", mContext.getPackageName())), "mMobileRoaming")).setImageTintList(ColorStateList.valueOf(textColor));
									}

									callMethod(batteryIcon, "updateColors", textColor, textColor, textColor);
								});
					});

			QSContainerImplClass
					.after("updateResources")
					.run(param -> {
						if (!isDark && lightQSHeaderEnabled) {
							Resources res = mContext.getResources();
							ViewGroup view = (ViewGroup) param.thisObject;

							View settings_button_container = view.findViewById(res.getIdentifier("settings_button_container", "id", mContext.getPackageName()));
							settings_button_container.getBackground().setTint(colorInactive);

							//Power Button on QS Footer
							((ImageView) ((ViewGroup)view.findViewById(res.getIdentifier("pm_lite", "id", mContext.getPackageName())))
									.getChildAt(0))
									.setColorFilter(colorInactive, PorterDuff.Mode.SRC_IN);

							ImageView icon = settings_button_container.findViewById(res.getIdentifier("icon", "id", mContext.getPackageName()));
							icon.setImageTintList(ColorStateList.valueOf(Color.BLACK));

							((FrameLayout.LayoutParams)
									((ViewGroup) settings_button_container
											.getParent()
									).getLayoutParams()
							).setMarginEnd(0);

							ViewGroup parent = (ViewGroup) settings_button_container.getParent();
							for(int i = 0; i < 3; i++) //Security + Foreground services containers
							{
								//noinspection DataFlowIssue
								parent.getChildAt(i).setBackground(lightFooterShape.getConstantState().newDrawable().mutate());
							}
						}
					});
		}

		QSIconViewImplClass
				.after("updateIcon")
				.run(param -> {
					if(lightQSHeaderEnabled
							&& !isDark
							&& getIntField(param.args[1], "state") == STATE_ACTIVE)
					{
						((ImageView) param.args[0])
								.setImageTintList(
										ColorStateList
												.valueOf(colorInactive));
					}
				});

		CentralSurfacesImplClass
				.afterConstruction()
				.run(param -> applyOverlays(true));

		QSTileViewImplClass
				.afterConstruction()
				.run(param -> {
					if (!lightQSHeaderEnabled || isDark) return;

					setObjectField(param.thisObject, "colorActive", colorActive);
					setObjectField(param.thisObject, "colorInactive", colorInactive);
					setObjectField(param.thisObject, "colorUnavailable", colorUnavailable);

					setObjectField(param.thisObject, "colorLabelActive", Color.WHITE);
				});

		CentralSurfacesImplClass
				.after("updateTheme")
				.run(param -> applyOverlays(false));

		ScrimControllerClass
				.after("updateThemeColors")
				.run(param -> calculateColors());

		ScrimControllerClass
				.after("applyState")
				.run(param -> {
					if (!lightQSHeaderEnabled) return;

					boolean mClipsQsScrim = (boolean) getObjectField(param.thisObject, "mClipsQsScrim");
					if (mClipsQsScrim) {
						setObjectField(param.thisObject, "mBehindTint", mScrimBehindTint);
					}
				});

		//region power menu aka GlobalActions
		GlobalActionsLayoutLiteClass
				.before("onLayout")
				.run(param -> {
					if(!enablePowerMenuTheme || isDark) return;

					((View) param.thisObject)
							.findViewById(android.R.id.list)
							.getBackground()
							.setTint(mScrimBehindTint); //Layout background color
				});

		GlobalActionsDialogLiteEmergencyActionClass
				.after("create")
				.run(param -> {
					if(!enablePowerMenuTheme || isDark) return;

					((TextView) ((View) param.getResult())
							.findViewById(android.R.id.message))
							.setTextColor(BLACK); //Emergency Text Color
				});

		GlobalActionsDialogLiteSinglePressActionClass
				.after("create")
				.run(param -> {
					if(!enablePowerMenuTheme || isDark) return;

					View itemView = (View) param.getResult();

					ImageView iconView = itemView.findViewById(android.R.id.icon);

					iconView
							.getDrawable()
							.setTint(colorInactive); //Icon color

					iconView
							.getBackground()
							.setTint(colorActive); //Button Color

					((TextView)itemView
							.findViewById(android.R.id.message))
							.setTextColor(BLACK); //Text Color
				});
		//endregion
	}

	private void applyOverlays(boolean force) {
		boolean isCurrentlyDark = isDarkMode();

		if (isCurrentlyDark == isDark && !force) return;

		isDark = isCurrentlyDark;

		calculateColors();

		new Thread(() -> {
			try
			{
				setOverlay("QSLightThemeOverlay", false, true, false);
				setOverlay("QSLightThemeBSTOverlay", false, false, false);

				SystemUtils.threadSleep(50);

				if (lightQSHeaderEnabled && !isCurrentlyDark)
				{
					setOverlay("QSLightThemeOverlay", !brightnessThickTrackEnabled, true, false);
					setOverlay("QSLightThemeBSTOverlay", brightnessThickTrackEnabled, false, false);
				}
			}
			catch (Throwable ignored){}
		}).start();
	}

	@SuppressLint("DiscouragedApi")
	private void calculateColors() { //calculating dual-tone QS scrim color and tile colors
		if(!lightQSHeaderEnabled) return;

		mScrimBehindTint =  mContext.getColor(
				isDark
					? android.R.color.system_neutral1_1000
					: android.R.color.system_neutral1_100);

		setObjectField(unlockedScrimState, "mBehindTint", mScrimBehindTint);

		if (!isDark) {
			colorActive = mContext.getColor(android.R.color.system_accent1_600);

			colorInactive = mContext.getColor(android.R.color.system_accent1_10);

			colorUnavailable = applyAlpha(0.3f, colorInactive); //30% opacity of inactive color

			lightFooterShape.setTint(colorInactive);
		}
	}

	private boolean isDarkMode() {
		return SystemUtils.isDarkMode();
	}

	@ColorInt
	public static int applyAlpha(float alpha, int inputColor) {
		alpha *= Color.alpha(inputColor);
		return Color.argb((int) (alpha), Color.red(inputColor), Color.green(inputColor),
				Color.blue(inputColor));
	}

	private static Drawable getFooterShape(Context context) {
		Resources res = context.getResources();
		@SuppressLint("DiscouragedApi") int radius = res.getDimensionPixelSize(res.getIdentifier("qs_security_footer_corner_radius", "dimen", context.getPackageName()));
		float[] radiusF = new float[8];
		for (int i = 0; i < 8; i++) {
			radiusF[i] = radius;
		}
		final ShapeDrawable result = new ShapeDrawable(new RoundRectShape(radiusF, null, null));
		result.getPaint().setStyle(FILL);
		return result;
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}