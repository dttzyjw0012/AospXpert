package com.yhc.aospxpert.modpacks;

import android.content.Context;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;

public abstract class XposedModPack {
	protected Context mContext;

	public XposedModPack(Context context) {
		mContext = context;
	}

	public abstract void updatePrefs(String... Key);
	public final void handleLoadPackageInternal(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable
	{
		ReflectedClass.setDefaultClassloader(lpParam.classLoader);
		handleLoadPackage(lpParam);
	}

	public abstract void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable;

	public abstract boolean listensTo(String packageName);
}