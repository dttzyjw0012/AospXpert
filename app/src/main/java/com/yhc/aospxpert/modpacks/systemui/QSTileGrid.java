package com.yhc.aospxpert.modpacks.systemui;

import static android.view.View.TEXT_ALIGNMENT_CENTER;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findMethodExactIfExists;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.XPLauncher;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.SystemUtils;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass.ReflectionConsumer;

@SuppressWarnings("RedundantThrows")
public class QSTileGrid extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	private static final int NOT_SET = 0;
	private static final int QS_COL_NOT_SET = 1;
	private static final int QQS_NOT_SET = 4;

	private static int QSRowQty = NOT_SET;
	private static int QSColQty = QS_COL_NOT_SET;
	private static int QQSTileQty = QQS_NOT_SET;

	private static int QSRowQtyL = NOT_SET;
	private static int QSColQtyL = QS_COL_NOT_SET;
	private static int QQSTileQtyL = QQS_NOT_SET;

	private static Float labelSize = null, secondaryLabelSize = null;
	private static int labelSizeUnit = -1, secondaryLabelSizeUnit = -1;

	private static float QSLabelScaleFactor = 1, QSSecondaryLabelScaleFactor = 1;
	protected static boolean QSHapticEnabled = false;
	private static boolean VerticalQSTile = false;

	private int updateFontSizeMethodType = 0;

	public QSTileGrid(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;

		VerticalQSTile = Xprefs.getBoolean("VerticalQSTile", false);

		if(Key.length > 0 && Key[0].equals("VerticalQSTile"))
		{
			SystemUtils.doubleToggleDarkMode();
		}

		QSHapticEnabled = Xprefs.getBoolean("QSHapticEnabled", false);

		QSRowQty = Xprefs.getSliderInt( "QSRowQty", NOT_SET);
		QSColQty = Xprefs.getSliderInt( "QSColQty", QS_COL_NOT_SET);
		if(QSColQty < QS_COL_NOT_SET) QSColQty = QS_COL_NOT_SET;
		QQSTileQty = Xprefs.getSliderInt( "QQSTileQty", QQS_NOT_SET);

		QSRowQtyL = Xprefs.getSliderInt( "QSRowQtyL", NOT_SET);
		QSColQtyL = Xprefs.getSliderInt( "QSColQtyL", QS_COL_NOT_SET);
		if(QSColQtyL < QS_COL_NOT_SET) QSColQtyL = QS_COL_NOT_SET;
		QQSTileQtyL = Xprefs.getSliderInt( "QQSTileQtyL", QQS_NOT_SET);

		QSLabelScaleFactor = (Xprefs.getSliderFloat( "QSLabelScaleFactor", 0) + 100) / 100f;
		QSSecondaryLabelScaleFactor = (Xprefs.getSliderFloat( "QSSecondaryLabelScaleFactor", 0) + 100) / 100f;

		if (Key.length > 0 && (Key[0].equals("QSRowQty") || Key[0].equals("QSColQty") || Key[0].equals("QQSTileQty") || Key[0].equals("QSRowQtyL") || Key[0].equals("QSColQtyL") || Key[0].equals("QQSTileQtyL"))) {
			SystemUtils.doubleToggleDarkMode();
		}
	}

	@SuppressLint("DiscouragedApi")
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		if (!lpParam.packageName.equals(listenPackage)) return;

		ReflectedClass QSTileViewImplClass = ReflectedClass.of("com.android.systemui.qs.tileimpl.QSTileViewImpl");
		ReflectedClass FontSizeUtilsClass = ReflectedClass.of("com.android.systemui.FontSizeUtils");
		ReflectedClass QSTileImplClass = ReflectedClass.of("com.android.systemui.qs.tileimpl.QSTileImpl");
		ReflectedClass QSFactoryImplClass = ReflectedClass.of("com.android.systemui.qs.tileimpl.QSFactoryImpl");
		ReflectedClass QuickQSPanelControllerClass = ReflectedClass.of("com.android.systemui.qs.QuickQSPanelController");
		ReflectedClass QuickQSPanelClass =ReflectedClass.of("com.android.systemui.qs.QuickQSPanel");
		ReflectedClass TileAdapterClass = ReflectedClass.of("com.android.systemui.qs.customize.TileAdapter");
		ReflectedClass SideLabelTileLayoutClass = ReflectedClass.of("com.android.systemui.qs.SideLabelTileLayout");

		ReflectedClass TileLayoutClass = ReflectedClass.ofIfPossible("com.android.systemui.qs.TileLayout");
		if(TileLayoutClass.getClazz() == null) //new versions have merged tile layout to sidelable
		{
			TileLayoutClass = SideLabelTileLayoutClass;
		}

		if(findMethodExactIfExists(FontSizeUtilsClass.getClazz(),
				"updateFontSize",
				TextView.class, int.class)
				!= null)
		{
			updateFontSizeMethodType = 1;
		}


		final int tileLayoutHookSize = TileLayoutClass
				.after("updateResources")
				.run(param -> {
					if(getQSCols() != QS_COL_NOT_SET || getQSRows() != NOT_SET)
					{
						param.setResult(updateTileLayoutResources(param.thisObject));
					}
				}).size();

		SideLabelTileLayoutClass
				.after("updateResources")
				.run(param -> {
					Resources res = mContext.getResources();
					int QSRows = getQSRows();
					if(QSRows == NOT_SET)
					{
						QSRows = res.getInteger(res.getIdentifier("quick_settings_max_rows", "integer", mContext.getPackageName()));
					}
					setObjectField(param.thisObject, "mMaxAllowedRows", Math.max(1, QSRows));

					if(tileLayoutHookSize == 0) {
						if (getQSCols() != QS_COL_NOT_SET || getQSRows() != NOT_SET) {
							param.setResult(updateTileLayoutResources(param.thisObject));
						}
					}
				});

		TileAdapterClass
				.afterConstruction()
				.run(param -> {
					if(getQSCols() != QS_COL_NOT_SET)
					{
						setObjectField(param.thisObject, "mNumColumns", getQSCols());
					}
				});

		QuickQSPanelClass
				.afterConstruction()
				.run(param -> {
					int maxTiles = getQQSMaxTiles();
					if(maxTiles != QQS_NOT_SET)
						setObjectField(param.thisObject, "mMaxTiles", getQQSMaxTiles());
				});

		QuickQSPanelControllerClass
				.before("onConfigurationChanged")
				.run(param -> {
					int maxTiles = getQQSMaxTiles();
					if(maxTiles != QQS_NOT_SET)
					{
						param.setResult(null); //replacing the original method now
						if(maxTiles != getIntField(getObjectField(param.thisObject, "mView"), "mMaxTiles"))
						{
							setObjectField(getObjectField(param.thisObject, "mView"), "mMaxTiles", maxTiles);
							callMethod(param.thisObject, "setTiles");
						}
						callMethod(param.thisObject, "updateMediaExpansion");
					}
				});

		try {
			if(ReflectedClass.ofIfPossible("com.android.systemui.qs.tiles.WifiTile").getClazz() == null)
				Xprefs
						.edit()
						.putBoolean("InternetTileModEnabled", false)
						.apply();
		}
		catch (Throwable ignored){}

		//used to enable dual wifi/cell tiles for 13
		QSFactoryImplClass
				.before("createTile")
				.run(param -> {
					if(param.args[0].equals("wifi_AospXpert"))
					{
						param.args[0] = "wifi";
					}
					if(param.args[0].equals("cell_AospXpert"))
					{
						param.args[0] = "cell";
					}
				});

		ReflectionConsumer vibrateCallback = param -> {
			if (QSHapticEnabled) SystemUtils.vibrate(VibrationEffect.EFFECT_CLICK, VibrationAttributes.USAGE_TOUCH);
		};

		QSTileImplClass
				.after("click")
				.run(vibrateCallback);

		QSTileImplClass
				.after("longClick")
				.run(vibrateCallback);

		QSTileViewImplClass
				.before("onLayout")
				.run(this::setLabelSizes);

		QSTileViewImplClass
				.after("onConfigurationChanged")
				.run(param -> {
					if(VerticalQSTile)
						fixPaddingVerticalLayout((LinearLayout) param.thisObject);
				});

		QSTileViewImplClass
				.afterConstruction()
				.run(param -> {
					try {
						if(VerticalQSTile) {
							LinearLayout thisQSTileView = (LinearLayout) param.thisObject;

							thisQSTileView.setGravity(Gravity.CENTER);
							thisQSTileView.setOrientation(LinearLayout.VERTICAL);

							((TextView) getObjectField(param.thisObject, "label"))
									.setTextAlignment(TEXT_ALIGNMENT_CENTER);
							((TextView) getObjectField(param.thisObject, "secondaryLabel"))
									.setTextAlignment(TEXT_ALIGNMENT_CENTER);

							LinearLayout horizontalLinearLayout = new LinearLayout(mContext);
							horizontalLinearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

							LinearLayout labelContainer = (LinearLayout) getObjectField(param.thisObject, "labelContainer");
							thisQSTileView.removeView(labelContainer);
							horizontalLinearLayout.addView(labelContainer);

							labelContainer.setGravity(Gravity.CENTER_HORIZONTAL);

							thisQSTileView.removeView((View) getObjectField(param.thisObject, "sideView"));

							fixPaddingVerticalLayout(thisQSTileView);

							thisQSTileView.addView(horizontalLinearLayout);
						}

						if (labelSize == null) { //we need initial font sizes
							updateFontSize(FontSizeUtilsClass,
									getObjectField(param.thisObject, "label"),
									mContext.getResources().getIdentifier("qs_tile_text_size", "dimen", mContext.getPackageName()));

							updateFontSize(FontSizeUtilsClass,
									getObjectField(param.thisObject, "secondaryLabel"),
									mContext.getResources().getIdentifier("qs_tile_text_size", "dimen", mContext.getPackageName()));

							TextView label = (TextView) getObjectField(param.thisObject, "label");
							TextView secondaryLabel = (TextView) getObjectField(param.thisObject, "secondaryLabel");

							labelSizeUnit = label.getTextSizeUnit();
							labelSize = label.getTextSize();

							secondaryLabelSizeUnit = secondaryLabel.getTextSizeUnit();
							secondaryLabelSize = secondaryLabel.getTextSize();
						}
					} catch (Throwable ignored) {}
				});

		// when media is played, system reverts tile cols to default value of 2. handling it:
		TileLayoutClass
				.before("setMaxColumns")
				.run(param -> {
					Context context = (Context) getObjectField(param.thisObject, "mContext");
					boolean isLandscape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
					if (!isLandscape && QSColQty != QS_COL_NOT_SET) {
						param.args[0] = QSColQty;
					}
					else if (isLandscape && QSColQtyL != QS_COL_NOT_SET)
					{
						param.args[0] = QSColQtyL;
					}
				});
	}

	private void updateFontSize(ReflectedClass FontSizeUtilsClass, Object textView, int resId)
	{
		if(updateFontSizeMethodType == 1)
		{
			FontSizeUtilsClass.callStaticMethod(
					"updateFontSize",
					textView,
					resId);
		}
		else
		{
			FontSizeUtilsClass.callStaticMethod(
					"updateFontSize",
					resId,
					textView);
		}
	}

	@SuppressLint("DiscouragedApi")
	private boolean updateTileLayoutResources(Object thisObject) {
		final Resources res = mContext.getResources();
		int QSCols = getQSCols();
		if(QSCols == QS_COL_NOT_SET)
		{
			QSCols = res.getInteger(res.getIdentifier("quick_settings_num_columns", "integer", mContext.getPackageName()));
		}

		int QSRows = getQSRows();
		if(QSRows == NOT_SET)
		{
			QSRows = res.getInteger(res.getIdentifier("quick_settings_max_rows", "integer", mContext.getPackageName()));
		}

		setObjectField(thisObject, "mResourceColumns", Math.max(1, QSCols));
		setObjectField(thisObject, "mMaxAllowedRows", Math.max(1, QSRows));

		int oldColumns = getIntField(thisObject, "mColumns");
		int finalColumns = Math.min(Math.max(1, QSCols), getIntField(thisObject, "mMaxColumns"));
		setObjectField(thisObject, "mColumns", finalColumns);

		if (oldColumns != finalColumns) {
			((View)thisObject).requestLayout();
			return true;
		}
		return false;
	}

	private int getQQSMaxTiles() {
		return mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
				? QQSTileQty
				: QQSTileQtyL;
	}

	private int getQSCols() {
		return mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
				? QSColQty
				: QSColQtyL;
	}

	private int getQSRows() {
		return mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
				? QSRowQty
				: QSRowQtyL;
	}

	private void fixPaddingVerticalLayout(LinearLayout parent) {
		Resources res = mContext.getResources();

		@SuppressLint("DiscouragedApi") int padding = res.getDimensionPixelSize(
				res.getIdentifier(
						"qs_tile_padding",
						"dimen",
						mContext.getPackageName()
				)
		);

		parent.setPadding(padding,padding,padding,padding);

		((LinearLayout.LayoutParams) ((LinearLayout) getObjectField(parent, "labelContainer"))
				.getLayoutParams())
				.setMarginStart(0);
	}

	private void setLabelSizes(XC_MethodHook.MethodHookParam param) {
		try {
			if (QSLabelScaleFactor != 1) {
				((TextView) getObjectField(param.thisObject, "label")).setTextSize(labelSizeUnit, labelSize * QSLabelScaleFactor);
			}

			if (QSSecondaryLabelScaleFactor != 1) {
				((TextView) getObjectField(param.thisObject, "secondaryLabel")).setTextSize(secondaryLabelSizeUnit, secondaryLabelSize * QSSecondaryLabelScaleFactor);
			}
		} catch (Throwable ignored) {}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}