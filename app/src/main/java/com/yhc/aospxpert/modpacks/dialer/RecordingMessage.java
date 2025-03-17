package com.yhc.aospxpert.modpacks.dialer;

import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.SystemUtils;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;

@SuppressWarnings("RedundantThrows")
public class RecordingMessage extends XposedModPack {
	public static final String listenPackage = Constants.DIALER_PACKAGE;

	private static boolean removeRecodingMessage = false;

	public RecordingMessage(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;

		if (Key.length > 0 && Key[0].equals("DialerRemoveRecordMessage")) {
			SystemUtils.killSelf();
		}
		removeRecodingMessage = Xprefs.getBoolean("DialerRemoveRecordMessage", false);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@SuppressLint("DiscouragedApi")
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		int call_recording_starting_voice = mContext.getResources().getIdentifier("call_recording_starting_voice", "string", mContext.getPackageName());
		int call_recording_ending_voice = mContext.getResources().getIdentifier("call_recording_ending_voice", "string", mContext.getPackageName());

		ReflectedClass.of(Resources.class)
				.before("getString")
				.run(param -> {
					if (removeRecodingMessage
							&& (param.args[0].equals(call_recording_starting_voice) || param.args[0].equals(call_recording_ending_voice))) {
						param.setResult("");
					}
				});
	}
}