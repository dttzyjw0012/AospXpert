package com.yhc.aospxpert.modpacks.systemui;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;
import static com.yhc.aospxpert.modpacks.utils.toolkit.ColorUtils.getColorAttrDefaultColor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.XPLauncher;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.SystemUtils;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass.ReflectionConsumer;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedMethod;

@SuppressWarnings("RedundantThrows")
public class ThemeManager_14 extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;
	private static boolean lightQSHeaderEnabled = false;
	private static boolean enablePowerMenuTheme = false;
	private static boolean brightnessThickTrackEnabled = false;
	private boolean isDark;
	private Integer colorInactive = null;

	private final int colorFadedBlack = applyAlpha(0.3f, BLACK); //30% opacity of black color

	private int colorUnavailable;
	private int colorActive;
	private int mScrimBehindTint = BLACK;
	private Object unlockedScrimState;
	private Object ShadeCarrierGroupController;
	private final ArrayList<Object> ModernShadeCarrierGroupMobileViews = new ArrayList<>();
	private static final int PM_LITE_BACKGROUND_CODE = 1;
	private ColorStateList mDefaultLabelActiveColor, mDefaultLabelInactiveColor;

	public ThemeManager_14(Context context) {
		super(context);
		if (!listensTo(context.getPackageName())) return;

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

			rebuildSysUI(true);
		}

		try {
			if (Key[0].equals("LightQSPanel")) {
				//Application of Light QS usually only needs a screen off/on. but some users get confused. Let's restart systemUI and get it over with
				//This has to happen AFTER overlays are applied. So we do it after update operations are done
				SystemUtils.killSelf();
			}
		} catch (Throwable ignored) {
		}
	}

	public void setLightQSHeader(boolean state) {
		if (lightQSHeaderEnabled != state) {
			lightQSHeaderEnabled = state;

			rebuildSysUI(true);
		}
	}

	@SuppressLint("DiscouragedApi")
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) {
		if (!lightQSHeaderEnabled)
			return; //light QS header pref update needs a systemui restart. so there's no point to load these if not enabled

		ReflectedClass QSTileViewImplClass = ReflectedClass.of("com.android.systemui.qs.tileimpl.QSTileViewImpl");
		ReflectedClass ScrimControllerClass = ReflectedClass.of("com.android.systemui.statusbar.phone.ScrimController");
		ReflectedClass QSPanelControllerClass = ReflectedClass.of("com.android.systemui.qs.QSPanelController");
		ReflectedClass ScrimStateEnum = ReflectedClass.of("com.android.systemui.statusbar.phone.ScrimState");
		ReflectedClass QSIconViewImplClass = ReflectedClass.of("com.android.systemui.qs.tileimpl.QSIconViewImpl");
		ReflectedClass CentralSurfacesImplClass = ReflectedClass.of("com.android.systemui.statusbar.phone.CentralSurfacesImpl");
		ReflectedClass GlobalActionsDialogLiteSinglePressActionClass = ReflectedClass.of("com.android.systemui.globalactions.GlobalActionsDialogLite$SinglePressAction");
		ReflectedClass GlobalActionsDialogLiteEmergencyActionClass = ReflectedClass.of("com.android.systemui.globalactions.GlobalActionsDialogLite$EmergencyAction");
		ReflectedClass GlobalActionsLayoutLiteClass = ReflectedClass.of("com.android.systemui.globalactions.GlobalActionsLayoutLite");
		ReflectedClass QSFooterViewClass = ReflectedClass.of("com.android.systemui.qs.QSFooterView");
		ReflectedClass BrightnessSliderViewClass = ReflectedClass.of("com.android.systemui.settings.brightness.BrightnessSliderView");
		ReflectedClass ShadeCarrierClass = ReflectedClass.of("com.android.systemui.shade.carrier.ShadeCarrier");
		ReflectedClass QSCustomizerClass = ReflectedClass.of("com.android.systemui.qs.customize.QSCustomizer");
		ReflectedClass BatteryStatusChipClass = ReflectedClass.of("com.android.systemui.statusbar.BatteryStatusChip");
		ReflectedClass QSContainerImplClass = ReflectedClass.of("com.android.systemui.qs.QSContainerImpl");
		ReflectedClass ShadeHeaderControllerClass = ReflectedClass.ofIfPossible("com.android.systemui.shade.ShadeHeaderController");

		try { //A15 early implementation of QS Footer actions - doesn't seem to be leading to final A15 release
			ReflectedClass FooterActionsViewBinderClass = ReflectedClass.of("com.android.systemui.qs.footer.ui.binder.FooterActionsViewBinder");
			ReflectedClass TextButtonViewHolderClass = ReflectedClass.of("com.android.systemui.qs.footer.ui.binder.TextButtonViewHolder");
			ReflectedClass NumberButtonViewHolderClass = ReflectedClass.of("com.android.systemui.qs.footer.ui.binder.NumberButtonViewHolder");

			NumberButtonViewHolderClass
					.afterConstruction()
					.run(param -> {
						if (!isDark) {
							((ImageView) getObjectField(param.thisObject
									, "newDot"))
									.setColorFilter(BLACK);

							((TextView) getObjectField(param.thisObject
									, "number"))
									.setTextColor(BLACK);
						}
					});

			TextButtonViewHolderClass
					.afterConstruction()
					.run(param -> {
						if (!isDark) {
							((ImageView) getObjectField(param.thisObject
									, "chevron"))
									.setColorFilter(BLACK);

							((ImageView) getObjectField(param.thisObject
									, "icon"))
									.setColorFilter(BLACK);

							((ImageView) getObjectField(param.thisObject
									, "newDot"))
									.setColorFilter(BLACK);

							((TextView) getObjectField(param.thisObject
									, "text"))
									.setTextColor(BLACK);
						}
					});

			FooterActionsViewBinderClass
					.after("bind")
					.run(param -> {
						if (!isDark) {
							LinearLayout view = (LinearLayout) param.args[0];
							view.setBackgroundColor(mScrimBehindTint);
							view.setElevation(0); //remove elevation shadow
						}
					});
		} catch (Throwable ignored) {
		}

		try { //A14 Compose implementation of QS Footer actions
			ReflectedClass FooterActionsButtonViewModelClass = ReflectedClass.of("com.android.systemui.qs.footer.ui.viewmodel.FooterActionsButtonViewModel");
			ReflectedClass FooterActionsViewModelClass = ReflectedClass.of("com.android.systemui.qs.footer.ui.viewmodel.FooterActionsViewModel");
//			ReflectedClass FooterActionsKtClass = ReflectedClass.of("com.android.systemui.qs.footer.ui.compose.FooterActionsKt");
			ReflectedClass ThemeColorKtClass = ReflectedClass.of("com.android.compose.theme.ColorKt");
			ReflectedClass ExpandableControllerImplClass = ReflectedClass.of("com.android.compose.animation.ExpandableControllerImpl");


			FooterActionsButtonViewModelClass
					.afterConstruction()
					.run(param -> { //A16 power button
						Resources res = mContext.getResources();
						if(getIntField(param.thisObject, "id") == res.getIdentifier("pm_lite", "id", mContext.getPackageName()))
						{
							setObjectField(param.thisObject, "backgroundColor", PM_LITE_BACKGROUND_CODE);
							setObjectField(param.thisObject, "iconTint", colorInactive);
						}
					});

			ExpandableControllerImplClass
					.beforeConstruction()
					.run(param -> {
						if (!isDark) {
							ReflectedClass GraphicsColorKtClass = ReflectedClass.of("androidx.compose.ui.graphics.ColorKt");
							param.args[1] = GraphicsColorKtClass.callStaticMethod("Color", BLACK);
						}
					});

			Parameter[] colorAttrsParams = ReflectedMethod.findMethod(ThemeColorKtClass.getClazz(), "colorAttr").getParameters();
			int residIndex = IntStream.range(0, colorAttrsParams.length).filter(i -> (colorAttrsParams[i].getType().equals(int.class))).findFirst().orElse(0);

			ThemeColorKtClass
					.before("colorAttr")
					.run(param -> {
						if (isDark) return;

						int code = (int) param.args[residIndex];

						int result = 0;

						if (code == PM_LITE_BACKGROUND_CODE) {
							result = colorActive;
						} else {
							try {
								//numberbutton text
								result = switch (mContext.getResources().getResourceName(code).split("/")[1]) {
									case "underSurface",
										 "onShadeActive",
										 "shadeInactive" -> colorInactive; //button backgrounds
									case "onShadeInactiveVariant" -> BLACK;
									default -> result;
								};
							} catch (Throwable ignored) {
							}
						}

						if (result != 0) {
							ReflectedClass GraphicsColorKtClass = ReflectedClass.of("androidx.compose.ui.graphics.ColorKt");
							param.setResult(GraphicsColorKtClass.callStaticMethod("Color", result));
						}
					});

			FooterActionsViewModelClass
					.afterConstruction()
					.run(param -> {
						if (isDark) return;

						//power button
						Object power = getObjectField(param.thisObject, "power");
						try { //A15 and lower. On 16 we directly set things on
							setObjectField(power, "iconTint", colorInactive);
							setObjectField(power, "backgroundColor", PM_LITE_BACKGROUND_CODE);
						}
						catch (Throwable ignored){}

						//settings button
						setObjectField(
								getObjectField(param.thisObject, "settings"),
								"iconTint",
								BLACK);

						//we must use the classes defined in the apk. using our own will fail
						ReflectedClass StateFlowImplClass = ReflectedClass.of("kotlinx.coroutines.flow.StateFlowImpl");
						ReflectedClass ReadonlyStateFlowClass = ReflectedClass.of("kotlinx.coroutines.flow.ReadonlyStateFlow");

						Object zeroAlphaFlow = StateFlowImplClass.getClazz().getConstructor(Object.class).newInstance(0f);
						setObjectField(param.thisObject, "backgroundAlpha", ReadonlyStateFlowClass.getClazz().getConstructors()[0].newInstance(zeroAlphaFlow));
					});
		} catch (Throwable ignored) {
		}

		try { //A14 ap11 onwards - modern implementation of mobile icons
			ReflectedClass ShadeCarrierGroupControllerClass = ReflectedClass.of("com.android.systemui.shade.carrier.ShadeCarrierGroupController");
			ReflectedClass MobileIconBinderClass = ReflectedClass.of("com.android.systemui.statusbar.pipeline.mobile.ui.binder.MobileIconBinder");

			ShadeCarrierGroupControllerClass
					.afterConstruction()
					.run(param -> ShadeCarrierGroupController = param.thisObject);

			MobileIconBinderClass
					.after("bind")
					.run(param -> {
						if (param.args[1].getClass().getName().contains("ShadeCarrierGroupMobileIconViewModel")) {
							ModernShadeCarrierGroupMobileViews.add(param.getResult());
							if (!isDark) {
								int textColor = getColorAttrDefaultColor(mContext, android.R.attr.textColorPrimary);
								setMobileIconTint(param.getResult(), textColor);
							}
						}
					});
		} catch (Throwable ignored) {
		}

		if (ShadeHeaderControllerClass.getClazz() == null) {
			ShadeHeaderControllerClass = ReflectedClass.of("com.android.systemui.shade.LargeScreenShadeHeaderController");
		}

		QSCustomizerClass
				.afterConstruction()
				.run(param -> {
					if (!isDark) {
						ViewGroup mainView = (ViewGroup) param.thisObject;
						for (int i = 0; i < mainView.getChildCount(); i++) {
							mainView.getChildAt(i).setBackgroundColor(mScrimBehindTint);
						}
					}
				});

		ShadeCarrierClass
				.after("updateState")
				.run(param -> {
					if (!isDark) {
						((ImageView) getObjectField(param.thisObject
								, "mMobileSignal"))
								.setImageTintList(
										ColorStateList.valueOf(BLACK)
								);
					}
				});

		QSFooterViewClass
				.after("onFinishInflate")
				.run(param -> {
					if (!isDark) {
						((TextView) getObjectField(param.thisObject
								, "mBuildText"))
								.setTextColor(BLACK);

						((ImageView) getObjectField(param.thisObject
								, "mEditButton"))
								.setColorFilter(BLACK);

						setObjectField(
								getObjectField(
										param.thisObject
										, "mPageIndicator")
								, "mTint"
								, ColorStateList.valueOf(BLACK));
					}
				});

		ReflectionConsumer updateResourcesHook = param -> {
			if (!isDark)
				((LinearLayout) getObjectField(param.thisObject, "roundedContainer"))
						.getBackground()
						.setTint(colorInactive);
		};

		BatteryStatusChipClass
				.afterConstruction()
				.run(updateResourcesHook);

		BatteryStatusChipClass
				.after("onConfigurationChanged")
				.run(updateResourcesHook);

		BrightnessSliderViewClass
				.after("onFinishInflate")
				.run(param -> {
					if (!isDark) {
						((LayerDrawable) callMethod(
								getObjectField(param.thisObject, "mSlider")
								, "getProgressDrawable"))
								.findDrawableByLayerId(android.R.id.background)
								.setTint(Color.GRAY); //setting brightness slider background to gray

						((GradientDrawable) getObjectField(param.thisObject
								, "mProgressDrawable"))
								.setColor(colorActive); //progress drawable


						LayerDrawable progress = (LayerDrawable) callMethod(getObjectField(param.thisObject, "mSlider"), "getProgressDrawable");
						DrawableWrapper progressSlider = (DrawableWrapper) progress
								.findDrawableByLayerId(android.R.id.progress);
						LayerDrawable actualProgressSlider = (LayerDrawable) progressSlider.getDrawable();
						@SuppressLint("DiscouragedApi")
						Drawable slider_icon = actualProgressSlider.findDrawableByLayerId(mContext.getResources().getIdentifier("slider_icon", "id", mContext.getPackageName()));
						slider_icon.setTint(WHITE); //progress icon
					}
				});

		//noinspection OptionalGetWithoutIsPresent
		unlockedScrimState = Arrays.stream(ScrimStateEnum.getClazz().getEnumConstants()).filter(c -> c.toString().equals("UNLOCKED")).findFirst().get();

		ReflectedClass.of(unlockedScrimState.getClass())
				.after("prepare")
				.run(param -> setObjectField(unlockedScrimState, "mBehindTint", mScrimBehindTint));

		QSPanelControllerClass
				.afterConstruction()
				.run(param -> calculateColors());

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
								Resources res = mContext.getResources();

								int textColor = getColorAttrDefaultColor(mContext, android.R.attr.textColorPrimary);

								((TextView) mView.findViewById(res.getIdentifier("clock", "id", mContext.getPackageName()))).setTextColor(textColor);
								((TextView) mView.findViewById(res.getIdentifier("date", "id", mContext.getPackageName()))).setTextColor(textColor);

								try { //A14 ap11
									callMethod(iconManager, "setTint", textColor, textColor);
								} catch (Throwable ignored) { //A14 older
									callMethod(iconManager, "setTint", textColor);
								}

								try { //A14 ap11
									ModernShadeCarrierGroupMobileViews.forEach(view -> setMobileIconTint(view, textColor));
									setModernSignalTextColor(textColor);
								} catch (Throwable ignored) {
								}

								for (int i = 1; i <= 3; i++) {
									String id = String.format("carrier%s", i);

									((TextView) getObjectField(mView.findViewById(res.getIdentifier(id, "id", mContext.getPackageName())), "mCarrierText")).setTextColor(textColor);
									((ImageView) getObjectField(mView.findViewById(res.getIdentifier(id, "id", mContext.getPackageName())), "mMobileSignal")).setImageTintList(ColorStateList.valueOf(textColor));
									((ImageView) getObjectField(mView.findViewById(res.getIdentifier(id, "id", mContext.getPackageName())), "mMobileRoaming")).setImageTintList(ColorStateList.valueOf(textColor));
								}

								callMethod(batteryIcon, "updateColors", textColor, textColor, textColor);
							});
				});

		QSContainerImplClass
				.after("updateResources")
				.run(param -> {
					if (!isDark) {
						try { //In case a compose implementation is in order, this block will fail
							Resources res = mContext.getResources();
							ViewGroup view = (ViewGroup) param.thisObject;

							@SuppressLint("DiscouragedApi")
							View settings_button_container = view.findViewById(res.getIdentifier("settings_button_container", "id", mContext.getPackageName()));
							settings_button_container.getBackground().setTint(colorInactive);

							//Power Button on QS Footer
							@SuppressLint("DiscouragedApi")
							ViewGroup powerButton = view.findViewById(res.getIdentifier("pm_lite", "id", mContext.getPackageName()));
							((ImageView) powerButton
									.getChildAt(0))
									.setColorFilter(colorInactive, PorterDuff.Mode.SRC_IN);
							powerButton.getBackground().setTint(colorActive);

							@SuppressLint("DiscouragedApi")
							ImageView icon = settings_button_container.findViewById(res.getIdentifier("icon", "id", mContext.getPackageName()));
							icon.setColorFilter(BLACK);

							((FrameLayout.LayoutParams)
									((ViewGroup) settings_button_container
											.getParent()
									).getLayoutParams()
							).setMarginEnd(0);


							ViewGroup parent = (ViewGroup) settings_button_container.getParent();
							for (int i = 0; i < 3; i++) //Security + Foreground services containers
							{
								parent.getChildAt(i).getBackground().setTint(colorInactive);
							}
						} catch (Throwable ignored) {
						}
					}
				});

		QSIconViewImplClass //composition makes it almost impossible to control icon color via reflection.
				.afterConstruction()
				.run(param -> {
					if(isDark) return;

					ViewGroup thisIconView = (ViewGroup) param.thisObject;

					ImageView originalIcon = (ImageView) getObjectField(param.thisObject, "mIcon");
					TintControlledImageView replacementIcon = new TintControlledImageView(originalIcon.getContext());

					replacementIcon.setImageDrawable(originalIcon.getDrawable());

					setObjectField(param.thisObject, "mIcon", replacementIcon);

					replacementIcon.setId(mContext.getResources().getIdentifier("icon", "id", mContext.getPackageName()));

					int index= thisIconView.indexOfChild(originalIcon);
					thisIconView.removeView(originalIcon);
					thisIconView.addView(replacementIcon, index);
				});

		CentralSurfacesImplClass
				.afterConstruction()
				.run(param -> new Thread(() -> {
					try {
						SystemUtils.threadSleep(5000);
						rebuildSysUI(true);
					} catch (Throwable ignored) {
					}
				}).start());


		//setting tile colors in light theme
		QSTileViewImplClass
				.afterConstruction()
				.run(param -> {
					if (isDark) return;

					mDefaultLabelActiveColor = ColorStateList.valueOf(getIntField(param.thisObject, "colorLabelActive"));
					mDefaultLabelInactiveColor = ColorStateList.valueOf(getIntField(param.thisObject, "colorSecondaryLabelInactive"));

					setObjectField(param.thisObject, "colorActive", colorActive);
					setObjectField(param.thisObject, "colorInactive", colorInactive);
					setObjectField(param.thisObject, "colorUnavailable", colorUnavailable);

					setObjectField(param.thisObject, "colorLabelActive", WHITE);
					setObjectField(param.thisObject, "colorSecondaryLabelActive", WHITE);

					setObjectField(param.thisObject, "colorLabelInactive", BLACK);
					setObjectField(param.thisObject, "colorSecondaryLabelInactive", BLACK);

					setObjectField(param.thisObject, "colorLabelInactive", BLACK);
					setObjectField(param.thisObject, "colorSecondaryLabelInactive", BLACK);
				});

		CentralSurfacesImplClass
				.after("updateTheme")
				.run(param -> rebuildSysUI(false));

		ScrimControllerClass
				.after("updateThemeColors")
				.run(param -> calculateColors());

		ScrimControllerClass
				.after(Pattern.compile("applyState.*"))
				.run(param -> {
					boolean mClipsQsScrim = (boolean) getObjectField(param.thisObject, "mClipsQsScrim");
					if (mClipsQsScrim) {
						setObjectField(param.thisObject, "mBehindTint", mScrimBehindTint);
					}
				});

		//region power menu aka GlobalActions
		GlobalActionsLayoutLiteClass
				.before("onLayout")
				.run(param -> {
					if (!enablePowerMenuTheme || isDark) return;

					((View) param.thisObject)
							.findViewById(android.R.id.list)
							.getBackground()
							.setTint(mScrimBehindTint); //Layout background color
				});

		GlobalActionsDialogLiteEmergencyActionClass
				.after("create")
				.run(param -> {
					if (!enablePowerMenuTheme || isDark) return;

					((TextView) ((View) param.getResult())
							.findViewById(android.R.id.message))
							.setTextColor(BLACK); //Emergency Text Color
				});

		GlobalActionsDialogLiteSinglePressActionClass
				.after("create")
				.run(param -> {
					if (!enablePowerMenuTheme || isDark) return;

					View itemView = (View) param.getResult();

					ImageView iconView = itemView.findViewById(android.R.id.icon);

					iconView
							.getDrawable()
							.setTint(colorInactive); //Icon color

					iconView
							.getBackground()
							.setTint(colorActive); //Button Color

					((TextView) itemView
							.findViewById(android.R.id.message))
							.setTextColor(BLACK); //Text Color
				});
		//endregion

	}

	private void setMobileIconTint(Object ModernStatusBarViewBinding, int textColor) {
		callMethod(ModernStatusBarViewBinding, "onIconTintChanged", textColor, textColor);
	}

	@SuppressLint("DiscouragedApi")
	private void setModernSignalTextColor(int textColor) {
		Resources res = mContext.getResources();

		for (View shadeCarrier : (View[]) getObjectField(ShadeCarrierGroupController, "mCarrierGroups")) {
			try {
				shadeCarrier = shadeCarrier.findViewById(res.getIdentifier("carrier_combo", "id", mContext.getPackageName()));
				((TextView) shadeCarrier.findViewById(res.getIdentifier("mobile_carrier_text", "id", mContext.getPackageName()))).setTextColor(textColor);
			} catch (Throwable ignored) {
			}
		}
	}

	private void rebuildSysUI(boolean force) {
		boolean isCurrentlyDark = isDarkMode();

		if (isCurrentlyDark == isDark && !force) return;

		isDark = isCurrentlyDark;

		calculateColors();

		XPLauncher.enqueueProxyCommand(proxy -> proxy.runCommand("cmd overlay disable com.google.android.systemui.gxoverlay; cmd overlay enable com.google.android.systemui.gxoverlay"));
	}

	@SuppressLint("DiscouragedApi")
	private void calculateColors() { //calculating dual-tone QS scrim color and tile colors
		mScrimBehindTint = mContext.getColor(
				isDark
						? android.R.color.system_neutral1_1000
						: android.R.color.system_neutral1_100);

		try {
			setObjectField(unlockedScrimState, "mBehindTint", mScrimBehindTint);
		} catch (Throwable ignored) {
		}

		if (!isDark) {
			colorActive = mContext.getColor(android.R.color.system_accent1_600);

			colorInactive = mContext.getColor(android.R.color.system_accent1_10);

			colorUnavailable = applyAlpha(0.3f, colorInactive); //30% opacity of inactive color
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

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	@SuppressLint("AppCompatCustomView")
	public class TintControlledImageView extends ImageView
	{
		public TintControlledImageView(Context context) {
			super(context);
		}

		public TintControlledImageView(Context context, @Nullable AttributeSet attrs) {
			super(context, attrs);
		}

		public TintControlledImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
			super(context, attrs, defStyleAttr);
		}

		@Override public void setImageTintList(ColorStateList tintList)
		{
			super.setImageTintList(getIconTintLightMode(tintList));
		}

		private ColorStateList getIconTintLightMode(ColorStateList tintList) {
			if(tintList.equals(mDefaultLabelActiveColor))
			{
				return ColorStateList.valueOf(colorInactive);
			}
			else if(tintList.equals(mDefaultLabelInactiveColor))
			{
				return ColorStateList.valueOf(BLACK);
			}
			return ColorStateList.valueOf(colorFadedBlack);
		}
	}
}