package com.yhc.aospxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.XPLauncher;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;

@SuppressWarnings("RedundantThrows")
public class FingerprintWhileDozing extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;
	private static boolean fingerprintWhileDozing = true;

	public FingerprintWhileDozing(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		fingerprintWhileDozing = Xprefs.getBoolean("fingerprintWhileDozing", true);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		if (!lpParam.packageName.equals(listenPackage)) return;

		ReflectedClass KeyguardUpdateMonitorClass = ReflectedClass.of("com.android.keyguard.KeyguardUpdateMonitor");

		KeyguardUpdateMonitorClass
				.before("shouldListenForFingerprint")
				.run(param -> {
					if (fingerprintWhileDozing) return;

					if(!getBooleanField(param.thisObject, "mDeviceInteractive"))
					{
						param.setResult(false);
					}
				});
	}
}