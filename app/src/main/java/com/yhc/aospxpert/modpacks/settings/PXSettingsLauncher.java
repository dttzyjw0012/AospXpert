package com.yhc.aospxpert.modpacks.settings;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.res.ResourcesCompat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.BuildConfig;
import com.yhc.aospxpert.R;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.ResourceManager;
import com.yhc.aospxpert.modpacks.XPLauncher;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;

@SuppressWarnings("RedundantThrows")
public class PXSettingsLauncher extends XposedModPack {
	private static final String listenPackage = Constants.SETTINGS_PACKAGE;

	private static boolean PXInSettings = true;

	private boolean mNewSettings = true;

	private boolean mExpressiveTheme = false;

	public PXSettingsLauncher(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		PXInSettings = Xprefs.getBoolean("PXInSettings", true);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectedClass HomepagePreferenceClass = ReflectedClass.of("com.android.settings.widget.HomepagePreference");
		ReflectedClass TopLevelSettingsClass = ReflectedClass.of("com.android.settings.homepage.TopLevelSettings");
		ReflectedClass OnPreferenceClickListenerInterface = ReflectedClass.of("androidx.preference.Preference$OnPreferenceClickListener");

		ReflectedClass PreferenceCategoryClass = ReflectedClass.ofIfPossible("androidx.preference.PreferenceCategory");
		ReflectedClass PreferenceManagerClass = ReflectedClass.ofIfPossible("androidx.preference.PreferenceManager");

		try { //A16 expressive theme needs a different icon of PX
			ReflectedClass SettingsThemeHelperClass = ReflectedClass.of("com.android.settingslib.widget.SettingsThemeHelper");
			mExpressiveTheme = (boolean) SettingsThemeHelperClass.callStaticMethod("isExpressiveTheme", mContext);
		}
		catch (Throwable ignored){}

		TopLevelSettingsClass
				.after("getPreferenceScreenResId")
				.run(param -> {
					@SuppressLint("DiscouragedApi")
					int oldResName = mContext.getResources().getIdentifier("top_level_settings", "xml", mContext.getPackageName());

					if (param.getResult().equals(oldResName)) {
						mNewSettings = false;
					}
				});

		TopLevelSettingsClass
				.before("onCreateAdapter")
				.run(param -> {
					if (PXInSettings) {
						Object PXPreference = HomepagePreferenceClass.getClazz().getConstructor(Context.class).newInstance(mContext);

						callMethod(PXPreference, "setIcon",
								ResourcesCompat.getDrawable(ResourceManager.modRes,
										mExpressiveTheme
												? R.mipmap.ic_launcher
												: R.drawable.ic_notification_foreground,
										mContext.getTheme()));
						callMethod(PXPreference, "setTitle", ResourceManager.modRes.getString(R.string.app_name));

						Object onClickListener = Proxy.newProxyInstance(
								OnPreferenceClickListenerInterface.getClazz().getClassLoader(),
								new Class[]{OnPreferenceClickListenerInterface.getClazz()},
								new PXClickListener());

						setObjectField(PXPreference, "mOnClickListener", onClickListener);

						if (mNewSettings) {
							callMethod(PXPreference, "setSummary", ResourceManager.modRes.getString(R.string.xposed_desc));
							Object PXPreferenceCategory = PreferenceCategoryClass.getClazz().getConstructor(Context.class).newInstance(mContext);

							setObjectField(PXPreferenceCategory,
									"mPreferenceManager",
									PreferenceManagerClass.getClazz().getConstructor(Context.class).newInstance(mContext));
							callMethod(PXPreferenceCategory, "setOrder", 9999);

							@SuppressLint("DiscouragedApi")
							int layoutID = mContext.getResources().getIdentifier(
									"settingslib_preference_category_no_title",
									"layout",
									mContext.getPackageName());

							if (layoutID != 0)
								callMethod(PXPreferenceCategory, "setLayoutResource", layoutID);

							callMethod(PXPreferenceCategory, "addPreference", PXPreference);

							callMethod(param.args[0], "addPreference", PXPreferenceCategory);
						} else {
							callMethod(PXPreference, "setOrder", 9999);
							callMethod(param.args[0], "addPreference", PXPreference);
						}
					}
				});
	}

	class PXClickListener implements InvocationHandler {
		/**
		 * @noinspection SuspiciousInvocationHandlerImplementation
		 */
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(BuildConfig.APPLICATION_ID);
			mContext.startActivity(intent);

			return true;
		}
	}
}