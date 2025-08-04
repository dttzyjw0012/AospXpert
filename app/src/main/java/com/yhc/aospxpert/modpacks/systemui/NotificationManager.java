package com.yhc.aospxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;
import android.service.notification.StatusBarNotification;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.XPLauncher;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass.ReflectionConsumer;

@SuppressWarnings("RedundantThrows")
public class NotificationManager extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	private Object HeadsUpManager = null;

	private static int HeadupAutoDismissNotificationDecay = -1;
	private boolean DisableOngoingNotifDismiss = false;

	public NotificationManager(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		HeadupAutoDismissNotificationDecay = Xprefs.getSliderInt( "HeadupAutoDismissNotificationDecay", -1);
		DisableOngoingNotifDismiss = Xprefs.getBoolean("DisableOngoingNotifDismiss", false);
		try {
			applyDurations();
		} catch (Throwable ignored) {}
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectionConsumer headsupFinder = param -> {
			HeadsUpManager = param.thisObject;

			applyDurations();
		};

		try
		{ //A15 QPR2
			ReflectedClass HeadsUpManagerClass = ReflectedClass.of("com.android.systemui.statusbar.notification.headsup.HeadsUpManagerImpl");
			HeadsUpManagerClass.afterConstruction().run(headsupFinder);
		}
		catch (Throwable ignored){}

		try { //A15 QPR1 and older
			ReflectedClass HeadsUpManagerClass = ReflectedClass.of("com.android.systemui.statusbar.policy.HeadsUpManager");
			HeadsUpManagerClass.afterConstruction().run(headsupFinder); //interface in 14QPR2, class in older
		} catch (Throwable ignored){}

		try //A14 QPR2
		{
			ReflectedClass BaseHeadsUpManagerClass = ReflectedClass.of("com.android.systemui.statusbar.policy.BaseHeadsUpManager");
			BaseHeadsUpManagerClass.afterConstruction().run(headsupFinder);
		}
		catch (Throwable ignored){}

		ReflectedClass.of(StatusBarNotification.class)
				.after("isNonDismissable")
				.run(param -> {
					if(DisableOngoingNotifDismiss) {
						param.setResult((boolean) param.getResult() || ((StatusBarNotification) param.thisObject).isOngoing());
					}
				});
	}

	private void applyDurations() {
		if(HeadsUpManager != null && HeadupAutoDismissNotificationDecay > 0)
		{
			try { //A16
				setObjectField(HeadsUpManager, "mMinimumDisplayTimeDefault", Math.round(HeadupAutoDismissNotificationDecay / 2.5f));
			}
			catch (Throwable ignored) { //Older
				setObjectField(HeadsUpManager, "mMinimumDisplayTime", Math.round(HeadupAutoDismissNotificationDecay / 2.5f));
			}


			try //A14 QPR2B3
			{
				setObjectField(HeadsUpManager, "mAutoDismissTime", HeadupAutoDismissNotificationDecay);
			}
			catch (Throwable ignored) //Older
			{
				setObjectField(HeadsUpManager, "mAutoDismissNotificationDecay", HeadupAutoDismissNotificationDecay);
			}
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}