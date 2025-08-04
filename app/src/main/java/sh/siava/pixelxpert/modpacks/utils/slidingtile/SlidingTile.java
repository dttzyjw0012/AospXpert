package com.yhc.aospxpert.modpacks.utils.slidingtile;

import static android.os.VibrationAttributes.USAGE_TOUCH;
import static android.os.VibrationEffect.EFFECT_CLICK;
import static android.service.quicksettings.Tile.STATE_ACTIVE;
import static android.service.quicksettings.Tile.STATE_UNAVAILABLE;
import static android.view.View.GONE;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;
import static com.yhc.aospxpert.modpacks.systemui.QSTileGrid.isQSHapticEnabled;
import static com.yhc.aospxpert.modpacks.utils.SystemUtils.isDarkMode;
import static com.yhc.aospxpert.modpacks.utils.SystemUtils.vibrate;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.XPLauncher;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;

public abstract class SlidingTile extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	private boolean lightQSHeaderEnabled;
	public static final int STATE_NO_CHANGE = -1;
	private int currentLevel = 50;
	private List<WeakReference<Object>> QSPCBs = new ArrayList<>();
	private List<WeakReference<Object>> controlledTiles = new ArrayList<>();
	private boolean mIsSliding = false;
	private int mState = STATE_UNAVAILABLE;
	private Object mLastState;

	public abstract int getInitialValue();
	public abstract int getInitialState();
	public abstract int clampToLevelSteps(int value);

	/**
	 * Here we get the chance to save prefs if we want
	 */
	public abstract void saveCurrentState(int currentState, int currentValue);
	public abstract void handleClick(int currentValue);
	public abstract boolean isStateControlledByMod();
	/**
	 * If isStateControlledByMod==true, result must be STATE_ACTIVE or STATE_INACTIVE.
	 * If isStateControlledByMod==false, result must be STATE_NO_CHANGE
	 */
	public abstract int handleValueChange(int newValue);
	public abstract String getTargetSpec();
	public abstract String getTextForLevel(int currentPct);

	/**
	 *
	 * @return true if mod is enabled, false if not
	 */
	public abstract boolean shallControlTiles();
	public abstract void init();


	public SlidingTile(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		lightQSHeaderEnabled = Xprefs.getBoolean("LightQSPanel", false);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectedClass QSTileViewImplClass = ReflectedClass.of("com.android.systemui.qs.tileimpl.QSTileViewImpl");
		ReflectedClass QSPanelControllerBaseClass = ReflectedClass.of("com.android.systemui.qs.QSPanelControllerBase");

		QSTileViewImplClass.after("onConfigurationChanged").run(param -> {
			if (weControl(param.thisObject)) {
				updateTileView((View) param.thisObject);
			}
		});

		QSPanelControllerBaseClass
				.after("setTiles")
				.run(param ->
				{
					addQSPCB(param.thisObject);
					if(shallControlTiles()) {
						((ArrayList<?>) getObjectField(param.thisObject, "mRecords")).forEach(record ->
						{
							Object tile = getObjectField(record, "tile");
							if(mLastState == null)
							{
								mLastState = getObjectField(tile, "mState");
							}

							if (getTargetSpec().equals(getObjectField(tile, "mTileSpec"))) {
								View tileView = (View) getObjectField(record, "tileView");

								addTile(tileView);
								setInitialState(tileView);
							}
						});
					}
				});


		QSTileViewImplClass
				.before("handleStateChanged")
				.run(param -> {
					if(weControl(param.thisObject))
					{
						if(isStateControlledByMod()) {
							setObjectField(param.args[0], "state", mState);
						}
						else
						{
							mState = getIntField(param.args[0], "state");
						}
					}
				});

		QSTileViewImplClass
				.after("handleStateChanged")
				.run(param -> {
					if (weControl(param.thisObject)) {
						mLastState = param.args[0];

						updateTileView((LinearLayout) param.thisObject);
					}
				});

		init();
	}

	private void addQSPCB(Object thisObject) {
		List<WeakReference<Object>> newList = new ArrayList<>();
		boolean found = false;
		for(WeakReference<Object> QSPCBRef : QSPCBs) {
			Object QSPCB = QSPCBRef.get();
			if(QSPCB != null) {
				newList.add(QSPCBRef);
				if (thisObject == QSPCB)
				{
					found = true;
				}
			}
		}
		if(!found)
			newList.add(new WeakReference<>(thisObject));
		QSPCBs = newList;
	}

	private boolean weControl(Object tileView) {
		if(!shallControlTiles())
			return false;

		for(WeakReference<Object> tileViewRef : controlledTiles) {
			if(tileView.equals(tileViewRef.get()))
				return true;
		}
		return false;
	}

	public void refreshAllTiles() {
		for(WeakReference<Object> QSPCBRef : QSPCBs) {
			Object QSPCB = QSPCBRef.get();
			if(QSPCB != null) {
				callMethod(QSPCB, "setTiles");
			}
		}

		forEachTile(tileView ->
				callMethod(tileView, "onConfigurationChanged", mContext.getResources().getConfiguration()));
	}

	private void addTile(View tileView) {
		cleanupTheList();
		controlledTiles.add(new WeakReference<>(tileView));
	}

	private void cleanupTheList() {
		List<WeakReference<Object>> newMap = new ArrayList<>();

		controlledTiles.forEach(tileView -> {
			if(tileView.get() != null)
			{
				newMap.add(tileView);
			}
		});

		controlledTiles = newMap;
	}

	private void setInitialState(View tileView) {
		int initialState = getInitialState();

		setLevelFor(tileView, getInitialValue());

		if(initialState != STATE_NO_CHANGE)
		{
			mState = initialState;
			setObjectField(mLastState, "state", initialState);
			callMethod(tileView, "handleStateChanged", mLastState);
		}
		else
		{
			updateTileView(tileView);
		}
	}

	private void setTouchListenerFor(View tileView) {
		tileView.setOnTouchListener(new View.OnTouchListener() {
			float initX = 0;
			float initPct = 0;

			@SuppressLint({"DiscouragedApi", "ClickableViewAccessibility"})
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				if(!shallControlTiles())
					return false;

				switch (motionEvent.getAction()) {
					case MotionEvent.ACTION_DOWN: {
						initX = motionEvent.getX();
						initPct = initX / view.getWidth();
						return true;
					}

					case MotionEvent.ACTION_MOVE: {
						float deltaMove = Math.abs(initX - motionEvent.getX()) / view.getWidth();

						if (deltaMove > .03) {
							int newPct = clampToLevelSteps(round(max(min((motionEvent.getX() / view.getWidth()), 1), 0) * 100f));

							if (newPct != currentLevel) {
								setLevelFor(tileView,newPct);

								view.getParent().requestDisallowInterceptTouchEvent(true);
								mIsSliding = true;

								notifyValueChanged();
							}
						}
						return true;
					}

					case MotionEvent.ACTION_UP: {
						if (mIsSliding) {
							mIsSliding = false;
							saveCurrentState(mState, currentLevel);
						} else {
							if (isQSHapticEnabled())
								vibrate(EFFECT_CLICK, USAGE_TOUCH);
							handleClick(currentLevel);
						}
						return true;
					}
				}
				return true;
			}
		});
	}

	public void notifyValueChanged(int newValue) {
		currentLevel = newValue;
		notifyValueChanged();
	}
	private void notifyValueChanged() {
		int newState = handleValueChange(currentLevel);

		if(newState != STATE_NO_CHANGE && mState != newState)
		{
			mState = newState;
			setObjectField(mLastState, "state", newState);
			forEachTile(tileView ->
					callMethod(tileView, "handleStateChanged", mLastState));
		}
		else
		{
			forEachTile(this::updateTileView);
		}
	}

	private void forEachTile(Consumer<View> consumer) {
		controlledTiles.forEach(tileView -> {
			View view = (View) tileView.get();
			if (view != null) {
				consumer.accept(view);
			}
		});
	}

	private void updateTileView(View tileView) {
		try { //don't crash systemui if failed

			setLevelFor(tileView, currentLevel);
			setLabelFor(tileView);

			setTouchListenerFor(tileView);

			//We don't need the chevron icon on the right side
			((View) getObjectField(tileView, "chevronView"))
					.setVisibility(GONE);
		} catch (Throwable ignored) {}
	}

	private void setLabelFor(View tileView) {
		TextView label = (TextView) getObjectField(tileView, "label");
		String newLabel = getTextForLevel(currentLevel);
		label.setText(newLabel);
	}

	private void setTileBackgroundOf(View tileView) {
		LeveledTileBackgroundDrawable layerDrawable;
		try { //A14 AP11
			layerDrawable = new LeveledTileBackgroundDrawable(mContext,(Drawable) getObjectField(tileView, "backgroundDrawable"));
		} catch (Throwable ignored) { //Older
			layerDrawable = new LeveledTileBackgroundDrawable(mContext,(Drawable) getObjectField(tileView, "colorBackgroundDrawable"));
		}
		if (layerDrawable == null) return; //something is wrong

		setBackgroundStateOf(layerDrawable);
		tileView.setBackground(layerDrawable);
	}

	private void setBackgroundStateOf(LeveledTileBackgroundDrawable layerDrawable) {
		layerDrawable.setLevelTint((isDarkMode() || !lightQSHeaderEnabled) && mState != STATE_ACTIVE
				? Color.WHITE
				: Color.BLACK);
		layerDrawable.setPct(currentLevel);
	}

	private void setLevelFor(View tileView, int newVal) {
		Drawable background = tileView.getBackground();
		currentLevel = newVal;

		if(!(background instanceof LeveledTileBackgroundDrawable)) {
			setTileBackgroundOf(tileView);
		}
		else
		{
			setBackgroundStateOf((LeveledTileBackgroundDrawable) tileView.getBackground());
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}
