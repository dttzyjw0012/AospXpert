package com.yhc.aospxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;
import static com.yhc.aospxpert.modpacks.utils.toolkit.ObjectTools.tryParseInt;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.core.content.res.ResourcesCompat;

import java.util.Collection;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.R;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.ResourceManager;
import com.yhc.aospxpert.modpacks.XPLauncher;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;

@SuppressWarnings("RedundantThrows")
public class NotificationExpander extends XposedModPack {
	private static final int DEFAULT = 0;
	private static final int EXPAND_ALWAYS = 1;
	/** @noinspection unused*/
	private static final int COLLAPSE_ALWAYS = 2;

	public static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	public static boolean notificationExpandallHookEnabled = true;
	public static boolean notificationExpandallEnabled = false;
	private static int notificationDefaultExpansion = DEFAULT;
	private Button ExpandBtn, CollapseBtn;
	private FrameLayout FooterView;
	private FrameLayout BtnLayout;
	private static int fh = 0;
	private Object Scroller;
	private Object NotifCollection = null;

	public NotificationExpander(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		notificationExpandallEnabled = Xprefs.getBoolean("notificationExpandallEnabled", false);
		notificationExpandallHookEnabled = Xprefs.getBoolean("notificationExpandallHookEnabled", true);
		notificationDefaultExpansion = tryParseInt(Xprefs.getString("notificationDefaultExpansion", "0"), 0);

		if (Key.length > 0 && Key[0].equals("notificationExpandallEnabled")) {
			updateFooterBtn();
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		if (!listenPackage.equals(lpParam.packageName) || !notificationExpandallHookEnabled) return;

		ReflectedClass NotificationStackScrollLayoutClass = ReflectedClass.of("com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout");
		ReflectedClass FooterViewButtonClass = ReflectedClass.of("com.android.systemui.statusbar.notification.row.FooterViewButton");
		ReflectedClass NotifCollectionClass = ReflectedClass.ofIfPossible("com.android.systemui.statusbar.notification.collection.NotifCollection");
		ReflectedClass NotificationPanelViewControllerClass = ReflectedClass.of("com.android.systemui.shade.NotificationPanelViewController");
		ReflectedClass FooterViewClass;


		try { //14AP11
			FooterViewClass = ReflectedClass.of("com.android.systemui.statusbar.notification.footer.ui.view.FooterView");
		}
		catch (Throwable ignored) //Older
		{
			FooterViewClass = ReflectedClass.of("com.android.systemui.statusbar.notification.row.FooterView");
		}

		//region default notification state
		NotificationPanelViewControllerClass
				.before("notifyExpandingStarted")
				.run(param -> {
					if(notificationDefaultExpansion != DEFAULT)
						expandAll(notificationDefaultExpansion == EXPAND_ALWAYS);
				});
		//endregion

		//Notification Footer, where shortcuts should live
		FooterViewClass
				.after("onFinishInflate")
				.run(param -> {
					FooterView = (FrameLayout) param.thisObject;

					BtnLayout = new FrameLayout(mContext);
					FrameLayout.LayoutParams BtnFrameParams = new FrameLayout.LayoutParams(Math.round(fh * 2.5f), fh);
					BtnFrameParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
					BtnLayout.setLayoutParams(BtnFrameParams);

					ExpandBtn = (Button) FooterViewButtonClass.getClazz().getConstructor(Context.class).newInstance(mContext);
					BtnLayout.addView(ExpandBtn);

					FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(fh, fh);
					layoutParams.gravity = Gravity.START | Gravity.BOTTOM;
					ExpandBtn.setLayoutParams(layoutParams);

					ExpandBtn.setOnClickListener(v -> expandAll(true));

					CollapseBtn = (Button) FooterViewButtonClass.getClazz().getConstructor(Context.class).newInstance(mContext);
					BtnLayout.addView(CollapseBtn);

					FrameLayout.LayoutParams lpc = new FrameLayout.LayoutParams(fh, fh);
					lpc.gravity = Gravity.END | Gravity.BOTTOM;
					CollapseBtn.setLayoutParams(lpc);

					CollapseBtn.setOnClickListener(v -> expandAll(false));

					updateFooterBtn();
					FooterView.addView(BtnLayout);
				});

		//theme changed
		FooterViewClass
				.after("updateColors")
				.run(param -> updateFooterBtn());

		//grab notification container manager
		if (NotifCollectionClass.getClazz() != null) {
			NotifCollectionClass
					.afterConstruction()
					.run(param -> NotifCollection = param.thisObject);
		}

		//grab notification scroll page
		NotificationStackScrollLayoutClass
				.afterConstruction()
				.run(param -> Scroller = param.thisObject);
	}

	private void updateFooterBtn() {
		if(FooterView == null) return; //Footer not inflated yet

		View mClearAllButton = (View) getObjectField(FooterView, "mClearAllButton"); //A13

		int fh = mClearAllButton.getLayoutParams().height;

		if (fh > 0) { //sometimes it's zero. we don't need that
			NotificationExpander.fh = fh;
		}
		int tc = (int) callMethod(mClearAllButton, "getCurrentTextColor");

		Drawable expandArrows = ResourcesCompat.getDrawable(ResourceManager.modRes, R.drawable.ic_expand, mContext.getTheme());
		expandArrows.setTint(tc);
		ExpandBtn.setBackground(expandArrows);

		Drawable collapseArrows = ResourcesCompat.getDrawable(ResourceManager.modRes, R.drawable.ic_collapse, mContext.getTheme());
		collapseArrows.setTint(tc);
		CollapseBtn.setBackground(collapseArrows);

		BtnLayout.setVisibility(notificationExpandallEnabled ? View.VISIBLE : View.GONE);
	}

	public void expandAll(boolean expand) {
		if (NotifCollection == null) return;

		if (!expand) {
			callMethod(
					Scroller,
					"setOwnScrollY",
					/* pisition */0,
					/* animate */ true);
		}

		Collection<Object> entries;
		//noinspection unchecked
		entries = (Collection<Object>) getObjectField(NotifCollection, "mReadOnlyNotificationSet");
		for (Object entry : entries.toArray()) {
			Object row = getObjectField(entry, "row");
			if (row != null) {
				setRowExpansion(row, expand);
			}
		}

	}

	private void setRowExpansion(Object row, boolean expand) {
		callMethod(row, "setUserExpanded", expand, true);
	}
}
