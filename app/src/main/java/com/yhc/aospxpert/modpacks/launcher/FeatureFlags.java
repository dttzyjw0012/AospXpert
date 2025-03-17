package com.yhc.aospxpert.modpacks.launcher;

import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;
import android.graphics.drawable.AdaptiveIconDrawable;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.GoogleMonochromeIconFactory;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;

@SuppressWarnings("RedundantThrows")
public class FeatureFlags extends XposedModPack {
	private static final String listenPackage = Constants.LAUNCHER_PACKAGE;
	private static boolean ForceThemedLauncherIcons = false;
	private int mIconBitmapSize;

	public FeatureFlags(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		ForceThemedLauncherIcons = Xprefs.getBoolean("ForceThemedLauncherIcons", false);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		try {
			ReflectedClass BaseIconFactoryClass = ReflectedClass.of("com.android.launcher3.icons.BaseIconFactory");

			BaseIconFactoryClass
					.afterConstruction()
					.run(param -> mIconBitmapSize = getIntField(param.thisObject, "mIconBitmapSize"));

			ReflectedClass.of(AdaptiveIconDrawable.class)
					.after("getMonochrome")
					.run(param -> {
						try {
							if(param.getResult() == null && ForceThemedLauncherIcons)
							{
								if(new Throwable().getStackTrace()[4].getMethodName().toLowerCase().contains("override")) //It's from com.android.launcher3.icons.IconProvider.getIconWithOverrides. Monochrome is included
								{
									return;
								}

								GoogleMonochromeIconFactory mono = (GoogleMonochromeIconFactory) getAdditionalInstanceField(param.thisObject, "mMonoFactoryPX");
								if(mono == null)
								{
									mono = new GoogleMonochromeIconFactory((AdaptiveIconDrawable) param.thisObject, mIconBitmapSize);
									setAdditionalInstanceField(param.thisObject, "mMonoFactoryPX", mono);
								}
								param.setResult(mono);
							}
						}
						catch (Throwable ignored){}
					});
		}
		catch (Throwable ignored){} //Android 13
	}
}