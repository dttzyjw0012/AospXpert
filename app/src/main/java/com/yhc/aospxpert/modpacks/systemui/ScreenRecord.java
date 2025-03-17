package com.yhc.aospxpert.modpacks.systemui;

import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;

import java.lang.reflect.Method;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.XPLauncher;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;

@SuppressWarnings("RedundantThrows")
public class ScreenRecord extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	private static boolean InsecureScreenRecord = false;

	public ScreenRecord(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		InsecureScreenRecord = Xprefs.getBoolean("InsecureScreenRecord", false);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		if (!lpParam.packageName.equals(listenPackage)) return;

		ReflectedClass.of(MediaProjection.class)
				.before("createVirtualDisplay")
				.run(param -> {
					if(InsecureScreenRecord
							&& ((Method) param.method).getParameterCount() == 8)
					{
						int flags = (int) param.args[4];
						flags |= DisplayManager.VIRTUAL_DISPLAY_FLAG_SECURE;
						param.args[4] = flags;
					}
				});
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}
