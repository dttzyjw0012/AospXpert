package com.yhc.aospxpert.modpacks.systemui;

import static android.graphics.Color.BLACK;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.R;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.ResourceManager;
import com.yhc.aospxpert.modpacks.XPLauncher;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.SystemUtils;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;

public class PowerMenu extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	private ReflectedClass mLongPressActionInterface = null;
	private static boolean enablePowerMenuTheme = false;
	private static boolean advancedPowerMenu = false;

	public PowerMenu(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		advancedPowerMenu = Xprefs.getBoolean("advancedPowerMenu", false);

		enablePowerMenuTheme = Xprefs.getBoolean("LightQSPanel", false)
				&& Xprefs.getBoolean("enablePowerMenuTheme", false);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectedClass GlobalActionsDialogLiteClass = ReflectedClass.of("com.android.systemui.globalactions.GlobalActionsDialogLite");
		ReflectedClass PowerOptionsAction = ReflectedClass.of("com.android.systemui.globalactions.GlobalActionsDialogLite$PowerOptionsAction");
		mLongPressActionInterface = ReflectedClass.of("com.android.systemui.globalactions.GlobalActionsDialogLite$LongPressAction");

		PowerOptionsAction
				.afterConstruction()
				.run(param -> {
					if(!advancedPowerMenu) return;

					setObjectField(param.thisObject, "mMessageResId", 0);
					setObjectField(param.thisObject, "mMessage", getString(R.string.advanced_option_button_title));
				});

		GlobalActionsDialogLiteClass
				.after("createActionItems")
				.run(param -> {
					if(!advancedPowerMenu) return;


					//noinspection unchecked
					ArrayList<Object> mItems = (ArrayList<Object>) getObjectField(param.thisObject, "mItems");
					mItems.add(PowerOptionsAction.getClazz().getConstructors()[0].newInstance(param.thisObject));

					//noinspection unchecked
					ArrayList<Object> mPowerItems = (ArrayList<Object>) getObjectField(param.thisObject, "mPowerItems");

					mPowerItems.add(getAction(new BootloaderAction()));
					mPowerItems.add(getAction(new SoftRebootAction()));
					mPowerItems.add(getAction(new SystemUIRebootAction()));
				});
	}

	private String getString(int id)
	{
		return ResourceManager.modRes.getString(id);
	}

	private Object getAction(PXCustomAction action)
	{
		return Proxy.newProxyInstance(
				mLongPressActionInterface.getClazz().getClassLoader(),
				new Class[]{mLongPressActionInterface.getClazz()},
				action);
	}
	class BootloaderAction extends PXCustomAction
	{
		@Override
		protected boolean onLongPress() {
			return false;
		}

		@Override
		void onPress() {
			SystemUtils.restart("bootloader");
		}

		@Override
		String getText() {
			return getString(R.string.reboot_bootloader_title);
		}

		@SuppressLint("DiscouragedApi")
		@Override
		Drawable getIcon() {
			Resources res = mContext.getResources();
			return ResourcesCompat.getDrawable(res, res.getIdentifier("ic_restart", "drawable", "android"), mContext.getTheme());
		}
	}

	class SoftRebootAction extends PXCustomAction
	{
		@Override
		protected boolean onLongPress() {
			return false;
		}

		@Override
		void onPress() {
			SystemUtils.restart("android");
		}

		@Override
		String getText() {
			return getString(R.string.soft_reboot_title);
		}

		@SuppressLint("DiscouragedApi")
		@Override
		Drawable getIcon() {
			Resources res = mContext.getResources();
			return ResourcesCompat.getDrawable(res, res.getIdentifier("ic_restart", "drawable", "android"), mContext.getTheme());
		}
	}

	class SystemUIRebootAction extends PXCustomAction
	{
		@Override
		protected boolean onLongPress() {
			return false;
		}

		@Override
		void onPress() {
			SystemUtils.restart("systemUI");
		}

		@Override
		String getText() {
			return getString(R.string.restart_systemui_title);
		}

		@SuppressLint("DiscouragedApi")
		@Override
		Drawable getIcon() {
			Resources res = mContext.getResources();
			return ResourcesCompat.getDrawable(res, res.getIdentifier("ic_restart", "drawable", "android"), mContext.getTheme());
		}
	}

	abstract class PXCustomAction implements InvocationHandler {
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) {
			switch (method.getName())
			{
				case "create":
//					Context context = (Context) args[0];
//					View convertView = (View) args[1];
					ViewGroup parent = (ViewGroup) args[2];
					LayoutInflater layoutInflater = (LayoutInflater) args[3];
					Resources res = mContext.getResources();

					return create(parent, layoutInflater, res);

				case "onPress": //main action
					onPress();
					return null;
				case "onLongPress": //result: if longpress is handled by us
					return onLongPress();
				case "shouldShow": //used
					return true;

				//unused methods
				case "getLabelForAccessibility": //haven't seen usage
					return "";
				case "showDuringKeyguard": //didn't see usage
				case "showBeforeProvisioning": //didn't see usage
				case "shouldBeSeparated": //didn't see usage
					return false;
				case "isEnabled": //didn't see usage
					return true;
				case "getMessageResId": //not used
					return 0;
				case "getIcon": //not used
					return getIcon();
				case "getMessage": //not used
					return getText();
				//general object methods
				case "equals":
					return this.equals(args[0]);
				case "hashCode":
					return this.hashCode();
				case "toString":
					return this.toString();
			}
			return null;
		}

		private View create(ViewGroup parent, LayoutInflater layoutInflater, Resources res) {
			@SuppressLint("DiscouragedApi")
			View view = layoutInflater.inflate(res.getIdentifier("global_actions_grid_item_lite", "layout", mContext.getPackageName()), parent, false);

			@SuppressLint("DiscouragedApi")
			ImageView iconView = view.findViewById(res.getIdentifier("icon", "id", "android"));

			iconView.setImageDrawable(getIcon());
			iconView.setScaleType(ImageView.ScaleType.CENTER_CROP);

			@SuppressLint("DiscouragedApi")
			TextView messageView = view.findViewById(res.getIdentifier("message", "id", "android"));
			messageView.setSelected(true);
			messageView.setText(getText());

			if(enablePowerMenuTheme && !SystemUtils.isDarkMode())
			{
				messageView.setTextColor(BLACK);
			}

			view.setId(View.generateViewId());
			return view;
		}

		protected abstract boolean onLongPress();

		abstract void onPress();

		abstract String getText();

		abstract Drawable getIcon();
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}
