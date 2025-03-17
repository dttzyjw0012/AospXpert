package com.yhc.aospxpert.modpacks.ksu;

import static de.robv.android.xposed.XposedHelpers.callMethod;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.topjohnwu.superuser.Shell;

import org.objenesis.ObjenesisHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.BuildConfig;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.SystemUtils;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;

/**
 * @noinspection RedundantThrows
 */
public class KSUInjector extends XposedModPack {
	private static final String listenPackage = Constants.KSU_PACKAGE;

	public KSUInjector(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {

	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectedClass MainActivityClass = ReflectedClass.of("me.weishu.kernelsu.ui.MainActivity");
		ReflectedClass NativesClass = ReflectedClass.of("me.weishu.kernelsu.Natives");
		ReflectedClass ProfileClass = ReflectedClass.of("me.weishu.kernelsu.Natives$Profile");

		MainActivityClass
				.after("onCreate")
				.run(param -> {
					Intent launchIntent = ((Activity) param.thisObject).getIntent();
					if (launchIntent.hasExtra(Constants.PX_ROOT_EXTRA)) {
						new Thread(() -> {
							try {
								Object nativeObject = ObjenesisHelper.newInstance(NativesClass.getClazz());
								int[] rootUIDs = (int[]) callMethod(nativeObject, "getAllowList");

								PackageManager packageManager = mContext.getPackageManager();
								int ownUID = packageManager.getPackageUid(BuildConfig.APPLICATION_ID, PackageManager.GET_ACTIVITIES);

								boolean haveRoot = Arrays.stream(rootUIDs).anyMatch(uid -> uid == ownUID);

								if (!haveRoot) {
									Object ownRootProfile = ProfileClass.getClazz().getConstructor(String.class, int.class, boolean.class, boolean.class, String.class, int.class, int.class, List.class, List.class, String.class, int.class, boolean.class, boolean.class, String.class)
											.newInstance(BuildConfig.APPLICATION_ID, ownUID, true, true, null, 0, 0, new ArrayList<>(), new ArrayList<>(), "u:r:su:s0", 0, true, true, "");

									callMethod(nativeObject, "setAppProfile", ownRootProfile);

									restartPX(launchIntent.hasExtra("launchApp"));
								}
								Thread.sleep(2000);
								SystemUtils.killSelf();
							} catch (Throwable ignored) {
							}
						}).start();
					}
				});
	}

	private void restartPX(boolean launch) throws InterruptedException {
		Shell.cmd("killall " + BuildConfig.APPLICATION_ID).exec();

		if (launch) {
			Thread.sleep(1000);
			mContext.startActivity(
					mContext
							.getPackageManager()
							.getLaunchIntentForPackage(BuildConfig.APPLICATION_ID)
							.putExtra("FromKSU", 1));
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}
}