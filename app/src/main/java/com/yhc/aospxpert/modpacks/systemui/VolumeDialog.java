package com.yhc.aospxpert.modpacks.systemui;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getFloatField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.XPLauncher;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;

/** @noinspection RedundantThrows*/
@SuppressLint({"deprecation", "DiscouragedApi"})
public class VolumeDialog extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	private static final int DISMISS_REASON_TIMEOUT = 3;
	private static final int DISMISS = 2;

	private static int VolumeDialogTimeout = 3000;

	private static boolean VolumeDialogLevel = false;
	private final List<TextView> volumeTextViews = new ArrayList<>();

	public VolumeDialog(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		VolumeDialogTimeout = Xprefs.getSliderInt( "VolumeDialogTimeout", 3000);
		VolumeDialogLevel = Xprefs.getBoolean("VolumeDialogLevel", false);

		if (Key.length > 0 && Key[0].equals("VolumeDialogLevel")) {
			setVolumeTextViewsVisibility(VolumeDialogLevel ? VISIBLE : GONE);
		}
	}

	@SuppressLint("DefaultLocale")
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectedClass VolumeDialogImplClass = ReflectedClass.of("com.android.systemui.volume.VolumeDialogImpl");
		ReflectedClass AudioStreamStateClass = null;
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			AudioStreamStateClass = ReflectedClass.of("com.android.systemui.volume.panel.component.volume.slider.ui.viewmodel.AudioStreamSliderViewModel$State");
		}

		VolumeDialogImplClass
				.after("rescheduleTimeoutH")
				.run(param -> {
					if(VolumeDialogTimeout != 3000
							&& !getBooleanField(param.thisObject, "mHovering")
							&& getObjectField(param.thisObject, "mSafetyWarning") == null
							&& getObjectField(param.thisObject, "mODICaptionsTooltipView") == null)
					{
						Handler mHandler = (Handler) getObjectField(param.thisObject, "mHandler");
						mHandler.removeMessages(DISMISS);
						mHandler.sendMessageDelayed(mHandler.obtainMessage(DISMISS,DISMISS_REASON_TIMEOUT,0),VolumeDialogTimeout);
					}
				});

		VolumeDialogImplClass
				.after("initRow")
				.run(param -> {
					Resources res = mContext.getResources();
					TextView rowHeader = (TextView) getObjectField(param.args[0], "header");
					int volumeNumberId = res.getIdentifier("volume_number", "id", mContext.getPackageName());
					TextView existingVolumeNumber = ((ViewGroup) rowHeader.getParent()).findViewById(volumeNumberId);

					if (existingVolumeNumber != null) {
						if (!volumeTextViews.contains(existingVolumeNumber)) {
							volumeTextViews.add(existingVolumeNumber);
						}
						existingVolumeNumber.setVisibility(VolumeDialogLevel ? VISIBLE : GONE);
						return;
					}

					TextView volumeNumber = createVolumeTextView(volumeNumberId);
					((ViewGroup) rowHeader.getParent()).addView(volumeNumber, 0);
					volumeNumber.setVisibility(VolumeDialogLevel ? VISIBLE : GONE);
					volumeTextViews.add(volumeNumber);

					setObjectField(param.args[0], "number", ((View) getObjectField(param.args[0], "view")).findViewById(volumeNumberId));
				});


		VolumeDialogImplClass
				.after("updateVolumeRowH")
				.run(param -> {
					Resources res = mContext.getResources();
					int volumeNumberId = res.getIdentifier("volume_number", "id", mContext.getPackageName());
					TextView volumeNumber = ((View) getObjectField(param.args[0], "view")).findViewById(volumeNumberId);
					Object mState = getObjectField(param.thisObject, "mState");

					if (volumeNumber == null || mState == null) {
						return;
					}

					Object ss = callMethod(getObjectField(mState, "states"), "get", getObjectField(param.args[0], "stream"));

					if (ss == null) {
						return;
					}

					if (volumeNumber.getText().toString().isEmpty()) {
						volumeNumber.setText("0");
					}

					if (volumeNumber.getText().toString().endsWith("%")) {
						volumeNumber.setText(volumeNumber.getText().toString().subSequence(0, volumeNumber.getText().toString().length() - 1));
					}

					int levelMax = (int) getObjectField(ss, "levelMax");
					int level = (int) Math.ceil(Float.parseFloat(volumeNumber.getText().toString()) / levelMax * 100f);

					if (level > 100) {
						level = 100;
					} else if (level < 0) {
						level = 0;
					}

					volumeNumber.setText(String.format(Locale.getDefault(), "%d%%", level));
				});

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			AudioStreamStateClass.afterConstruction().run(param -> {
				if (!VolumeDialogLevel) return;

				float currentValue = getFloatField(param.thisObject, "value");
				float maxValue = getFloatField(
						getObjectField(
								param.thisObject,
								"valueRange"),
						"_endInclusive");

				float percentage = 100 * currentValue / maxValue;

				String label = (String) getObjectField(param.thisObject, "label");
				label = String.format("%s - %d%%", label, Math.round(percentage));
				setObjectField(param.thisObject, "label", label);
			});
		}
	}

	private void setVolumeTextViewsVisibility(int visibility) {
		Iterator<TextView> iterator = volumeTextViews.iterator();

		while (iterator.hasNext()) {
			TextView volumeTextView = iterator.next();

			if (volumeTextView != null && volumeTextView.getParent() != null) {
				volumeTextView.post(() -> volumeTextView.setVisibility(visibility));
			} else {
				iterator.remove();
			}
		}
	}

	private TextView createVolumeTextView(int viewId) {
		Resources res = mContext.getResources();

		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		lp.bottomMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, res.getDisplayMetrics());

		TextView volumeNumber = new TextView(mContext);
		volumeNumber.setLayoutParams(lp);
		volumeNumber.setId(viewId);
		volumeNumber.setGravity(Gravity.CENTER);
		volumeNumber.setTextSize(12f);
		volumeNumber.setTextColor(ContextCompat.getColor(mContext, res.getIdentifier("android:color/system_accent1_300", "color", mContext.getPackageName())));
		volumeNumber.setText(String.format(Locale.getDefault(), "%d%%", 0));

		return volumeNumber;
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}