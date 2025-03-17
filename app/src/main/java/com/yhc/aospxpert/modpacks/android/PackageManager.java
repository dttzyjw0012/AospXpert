package com.yhc.aospxpert.modpacks.android;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;

import java.util.Timer;
import java.util.TimerTask;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.BuildConfig;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;

/**
 * @noinspection RedundantThrows
 */
public class PackageManager extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_FRAMEWORK_PACKAGE;

	private static final int AUTO_DISABLE_MINUTES = 5;
	private static final String ALLOW_SIGNATURE_PREF = "PM_AllowMismatchedSignature";
	private static final String ALLOW_DOWNGRADE_PREF = "PM_AllowDowngrade";

	public static final int PERMISSION = 4;
	private static final int PERMISSION_GRANTED = 0;

	private static boolean PM_AllowMismatchedSignature = false;
	private static boolean PM_AllowDowngrade = false;

	public PackageManager(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		PM_AllowMismatchedSignature = Xprefs.getBoolean(ALLOW_SIGNATURE_PREF, false);
		PM_AllowDowngrade = Xprefs.getBoolean(ALLOW_DOWNGRADE_PREF, false);

		if (PM_AllowDowngrade || PM_AllowMismatchedSignature) {
			if (Key.length == 0) {
				disablePMMods();
			} else if (Key[0].equals(ALLOW_SIGNATURE_PREF) || Key[0].equals(ALLOW_DOWNGRADE_PREF)) {
				new Timer().schedule(new TimerTask() {
										 @Override
										 public void run() {
											 disablePMMods();
										 }
									 },
						AUTO_DISABLE_MINUTES * 60000);
			}
		}
	}

	private void disablePMMods() {
		Xprefs.edit()
				.putBoolean(ALLOW_SIGNATURE_PREF, false)
				.putBoolean(ALLOW_DOWNGRADE_PREF, false)
				.apply();
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		try {
			ReflectedClass InstallPackageHelperClass = ReflectedClass.of("com.android.server.pm.InstallPackageHelper");
			ReflectedClass PackageManagerServiceUtilsClass = ReflectedClass.of("com.android.server.pm.PackageManagerServiceUtils");
			ReflectedClass SigningDetailsClass = ReflectedClass.of("android.content.pm.SigningDetails");

			try {
				ReflectedClass ActivityManagerServiceClass = ReflectedClass.of("com.android.server.am.ActivityManagerService");

				ActivityManagerServiceClass
						.before("checkBroadcastFromSystem")
						.run(param -> {
							String action = ((Intent) param.args[0]).getAction();

							if (action.startsWith(BuildConfig.APPLICATION_ID + ".ACTION")) {
								param.setResult(null);
							}
						});

				//Granting pixel launcher permission to force stop apps
				ActivityManagerServiceClass
						.before("checkCallingPermission")
						.run(param -> {
							try {
								if ("android.permission.FORCE_STOP_PACKAGES".equals(param.args[0])) {
									if (Constants.LAUNCHER_PACKAGE.equals(
											callMethod(
													getObjectField(param.thisObject, "mInternal"),
													"getPackageNameByPid",
													Binder.getCallingPid()))) {
										param.setResult(PERMISSION_GRANTED);
									}
								}
							} catch (Throwable ignored) {
							}
						});

			} catch (Throwable ignored) {
			}

			PackageManagerServiceUtilsClass
					.before("checkDowngrade")
					.run(param -> {
						if (PM_AllowDowngrade) {
							param.setResult(null);
						}
					});

			SigningDetailsClass
					.before("checkCapability")
					.run(param -> {
						if (PM_AllowMismatchedSignature && !param.args[1].equals(PERMISSION)) {
							param.setResult(true);
						}
					});

			PackageManagerServiceUtilsClass
					.before("verifySignatures")
					.run(param -> {
						try {
							if (PM_AllowMismatchedSignature &&
									callMethod(
											callMethod(param.args[0], "getSigningDetails"),
											"getSignatures"
									) != null) {
								param.setResult(true);
							}
						} catch (Throwable ignored) {
						}
					});

			InstallPackageHelperClass
					.before("doesSignatureMatchForPermissions")
					.run(param -> {
						try {
							if (PM_AllowMismatchedSignature
									&& callMethod(param.args[1], "getPackageName").equals(param.args[0])
									&& ((String) callMethod(param.args[1], "getBaseApkPath")).startsWith("/data")) {
								param.setResult(true);
							}
						} catch (Throwable ignored) {
						}
					});
		} catch (Throwable ignored) {
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

}
