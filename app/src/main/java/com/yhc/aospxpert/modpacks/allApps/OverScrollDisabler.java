package com.yhc.aospxpert.modpacks.allApps;

import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;
import android.view.View;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;

@SuppressWarnings("RedundantThrows")
public class OverScrollDisabler extends XposedModPack {
	private static boolean disableOverScroll = false;

	public OverScrollDisabler(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		disableOverScroll = Xprefs.getBoolean("disableOverScroll", false);
	}

	@Override
	public boolean listensTo(String packageName) {
		return true;
	} //This mod is compatible with every package

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {

		ReflectedClass ViewClass = ReflectedClass.of(View.class);

		if (disableOverScroll) {
			ViewClass
					.before("setOverScrollMode")
					.run(param -> {
						setObjectField(param.thisObject, "mOverScrollMode", View.OVER_SCROLL_NEVER);
						param.setResult(null);
					});
		}
	}
}