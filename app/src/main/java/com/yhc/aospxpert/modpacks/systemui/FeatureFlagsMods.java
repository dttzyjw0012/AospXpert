package com.yhc.aospxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;

import java.util.regex.Pattern;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.XPLauncher;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;

@SuppressWarnings("RedundantThrows")
public class FeatureFlagsMods extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

//	public static final String CLIPBOARD_OVERLAY_SHOW_ACTIONS = "clipboard_overlay_show_actions";
//	public static final String NAMESPACE_SYSTEMUI = "systemui";

	private static final int SIGNAL_DEFAULT = 0;
	@SuppressWarnings("unused")
	private static final int SIGNAL_FORCE_LTE = 1;
	private static final int SIGNAL_FORCE_4G = 2;

	public static int SBLTEIcon = SIGNAL_DEFAULT;

	private static boolean EnableClipboardSmartActions = false;

	public FeatureFlagsMods(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;

		SBLTEIcon = Integer.parseInt(Xprefs.getString(
				"LTE4GIconMod",
				String.valueOf(SIGNAL_DEFAULT)));

		EnableClipboardSmartActions = Xprefs.getBoolean("EnableClipboardSmartActions", false);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		if (!lpParam.packageName.equals(listenPackage)) return;

/*		ReflectedClass DeviceConfigClass = ReflectedClass.of("android.provider.DeviceConfig");

		hookAllMethods(DeviceConfigClass, "getBoolean", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(param.args[0].equals(NAMESPACE_SYSTEMUI) && param.args[1].equals(CLIPBOARD_OVERLAY_SHOW_ACTIONS))
				{
					param.setResult(EnableClipboardSmartActions);
				}
			}
		});*/
		//replaced with this:
		ReflectedClass ClipboardOverlayControllerClass = ReflectedClass.of("com.android.systemui.clipboardoverlay.ClipboardOverlayController");

		ClipboardOverlayControllerClass
				.before(Pattern.compile("setExpandedView.*"))
				.run(param -> {
					if(EnableClipboardSmartActions) {
						setObjectField(
								getObjectField(param.thisObject, "mClipboardModel"),
								"isRemote",
								true);
					}
				});

		ReflectedClass.of("com.android.settingslib.mobile.MobileMappings$Config")
				.after("readConfig")
				.run(param -> {
					if (SBLTEIcon == SIGNAL_DEFAULT) return;

					setObjectField(param.getResult(),
							"show4gForLte",
							SBLTEIcon == SIGNAL_FORCE_4G);
				});
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}
