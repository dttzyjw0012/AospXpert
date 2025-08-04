package com.yhc.aospxpert.modpacks.launcher;

import static de.robv.android.xposed.XposedBridge.invokeOriginalMethod;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findFieldIfExists;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static com.yhc.aospxpert.modpacks.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.app.TaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Process;
import android.os.UserHandle;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.BuildConfig;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.SystemUtils;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass.ReflectionConsumer;

/**
 * @noinspection RedundantCast, JavaReflectionMemberAccess
 */
@SuppressWarnings("RedundantThrows")
public class TaskbarActivator extends XposedModPack {
	private static final String listenPackage = Constants.LAUNCHER_PACKAGE;

	public static final int TASKBAR_DEFAULT = 0;
	public static final int TASKBAR_ON = 1;
	/**
	 * @noinspection unused
	 */
	public static final int TASKBAR_OFF = 2;

	private static int taskbarMode = 0;
	private final TaskbarViews mTaskBarViews = new TaskbarViews();
	private static int numShownHotseatIcons = 0;
	private int UID = 0;
	private Object recentTasksList;
	private static boolean TaskbarAsRecents = false;
	private static boolean TaskbarTransient = false;
	private static boolean TaskbarOnLauncher = false;
	private static boolean GoogleRecents = false;
	private boolean refreshing = false;
	private static float taskbarHeightOverride = 1f;
	private static float TaskbarRadiusOverride = 1f;

	private Object model;
	String mTasksFieldName = null; // in case the code was obfuscated
	boolean mTasksIsList = false;
	private Object TaskbarModelCallbacks;
	private int mItemsLength = 0;
	private int mUpdateHotseatParams = 2;
	private String mUpdateItemsMethodName = "updateHotseatItems";

	private boolean ThreeButtonLayoutMod;
	private String ThreeButtonLeft, ThreeButtonCenter, ThreeButtonRight;
	private boolean mIsA16Plus = false;

	public TaskbarActivator(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {

		//3button nav has moved to taskbar since 15QPR2
		ThreeButtonLayoutMod = Xprefs.getBoolean("ThreeButtonLayoutMod", false);
		ThreeButtonLeft = Xprefs.getString("ThreeButtonLeft", "back").replace("recent", "recent_apps");
		ThreeButtonCenter = Xprefs.getString("ThreeButtonCenter", "home").replace("recent", "recent_apps");
		ThreeButtonRight = Xprefs.getString("ThreeButtonRight", "recent").replace("recent", "recent_apps");

		boolean noToggle = false;
		try
		{
			//noinspection ResultOfMethodCallIgnored
			Set.of(ThreeButtonLeft, ThreeButtonCenter, ThreeButtonRight);
		}
		catch (Throwable ignored){
			noToggle = true;
			ThreeButtonLayoutMod = false;
		}

		List<String> darkToggleKeys = Arrays.asList(
				"ThreeButtonLayoutMod",
				"ThreeButtonLeft",
				"ThreeButtonCenter",
				"ThreeButtonRight");

		if (Key.length > 0 && !noToggle && darkToggleKeys.contains(Key[0])) {
			SystemUtils.doubleToggleDarkMode();
		}


		List<String> restartKeys = Arrays.asList(
				"taskBarMode",
				"TaskbarAsRecents",
				"TaskbarTransient",
				"taskbarHeightOverride",
				"TaskbarRadiusOverride",
				"TaskbarHideAllAppsIcon",
				"EnableGoogleRecents");

		if (Key.length > 0 && restartKeys.contains(Key[0])) {
			SystemUtils.killSelf();
		}

		taskbarMode = Integer.parseInt(Xprefs.getString("taskBarMode", String.valueOf(TASKBAR_DEFAULT)));

		TaskbarAsRecents = Xprefs.getBoolean("TaskbarAsRecents", false);

		TaskbarRadiusOverride = Xprefs.getSliderFloat("TaskbarRadiusOverride", 1f);

		taskbarHeightOverride = Xprefs.getSliderFloat("taskbarHeightOverride", 100f) / 100f;

		taskbarMode = Integer.parseInt(Xprefs.getString("taskBarMode", "0"));

		TaskbarTransient = Xprefs.getBoolean("TaskbarTransient", false);

		TaskbarOnLauncher = Xprefs.getBoolean("TaskbarOnLauncher", false);

		GoogleRecents = Xprefs.getBoolean("EnableGoogleRecents", false);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@SuppressLint("DiscouragedApi")
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectedClass DeviceProfileClass = ReflectedClass.of("com.android.launcher3.DeviceProfile");
		ReflectedClass TaskbarActivityContextClass = ReflectedClass.of("com.android.launcher3.taskbar.TaskbarActivityContext");
		ReflectedClass LauncherModelClass = ReflectedClass.of("com.android.launcher3.LauncherModel");
		ReflectedClass BaseActivityClass = ReflectedClass.of("com.android.launcher3.BaseActivity");
		ReflectedClass DisplayControllerClass = ReflectedClass.of("com.android.launcher3.util.DisplayController");
		ReflectedClass DisplayControllerInfoClass = ReflectedClass.of("com.android.launcher3.util.DisplayController$Info");
		ReflectedClass StateControllerClass = ReflectedClass.of("com.android.launcher3.taskbar.TaskbarLauncherStateController");
		ReflectedClass AbstractNavButtonLayoutterClass = ReflectedClass.of("com.android.launcher3.taskbar.navbutton.AbstractNavButtonLayoutter");
		ReflectedClass RecentAppsControllerClass = ReflectedClass.of("com.android.launcher3.taskbar.TaskbarRecentAppsController");

		mIsA16Plus = !RecentAppsControllerClass.findMethods(Pattern.compile("computeShownRecentTasks")).isEmpty();

		//3 button nav order on A15+
		AbstractNavButtonLayoutterClass
				.afterConstruction()
				.run(param -> {
					if(!ThreeButtonLayoutMod) return;

					ViewGroup navButtonContainer = (ViewGroup) getObjectField(param.thisObject, "navButtonContainer");
					Resources res = mContext.getResources();

					int backButtonId = res.getIdentifier( ThreeButtonLeft, "id", mContext.getPackageName());
					int centerButtonId = res.getIdentifier( ThreeButtonCenter, "id", mContext.getPackageName());
					int rightButtonId = res.getIdentifier( ThreeButtonRight, "id", mContext.getPackageName());

					setObjectField(param.thisObject, "backButton", navButtonContainer.findViewById(backButtonId));
					setObjectField(param.thisObject, "homeButton", navButtonContainer.findViewById(centerButtonId));
					setObjectField(param.thisObject, "recentsButton", navButtonContainer.findViewById(rightButtonId));
				});

		//enable taskbar
		DisplayControllerInfoClass
				.before("isTablet")
				.run(param -> {
					if (taskbarMode == TASKBAR_DEFAULT) return;

					param.setResult(taskbarMode == TASKBAR_ON);
				});

		//auto hide
		DisplayControllerClass
				.before("isTransientTaskbar")
				.run(param -> {
					if (taskbarMode == TASKBAR_ON)
						param.setResult(TaskbarTransient);
				});

		LauncherModelClass
				.afterConstruction()
				.run(param -> model = param.thisObject);

		BaseActivityClass
				.after("onResume")
				.run(param -> {
					if (taskbarMode == TASKBAR_ON && model != null) {
						XposedHelpers.callMethod(model, "onAppIconChanged", BuildConfig.APPLICATION_ID, UserHandle.getUserHandleForUid(0));
					}
				});

		//hide on home screen
		StateControllerClass
				.before("isInLauncher")
				.run(param -> {
					if (TaskbarOnLauncher) {
						param.setResult(false);
					}
				});


		//region taskbar corner radius
		ReflectionConsumer cornerRadiusConsumer = param -> {
			if (taskbarMode == TASKBAR_ON && TaskbarRadiusOverride != 1f) {
				param.setResult(
						Math.round((int) param.getResult() * TaskbarRadiusOverride));
			}
		};

		TaskbarActivityContextClass.after("getLeftCornerRadius").run(cornerRadiusConsumer);
		TaskbarActivityContextClass.after("getRightCornerRadius").run(cornerRadiusConsumer);
		//endregion

		//region taskbar size
		String taskbarHeightField = findFieldIfExists(DeviceProfileClass.getClazz(), "taskbarSize") != null
				? "taskbarSize" //pre 13 QPR3
				: "taskbarHeight"; //13 QPR3

		String stashedTaskbarHeightField = findFieldIfExists(DeviceProfileClass.getClazz(), "stashedTaskbarSize") != null
				? "stashedTaskbarSize" //pre 13 QPR3
				: "stashedTaskbarHeight"; //13 QPR3
		//endregion

		//region recentbar
		DeviceProfileClass
				.afterConstruction()
				.run(param -> {
					if (taskbarMode == TASKBAR_DEFAULT) return;

					boolean taskbarEnabled = taskbarMode == TASKBAR_ON;

//				setObjectField(param.thisObject, "isTaskbarPresent", taskbarEnabled);

					if (taskbarEnabled) {
						numShownHotseatIcons = getIntField(param.thisObject, "numShownHotseatIcons");

						Resources res = mContext.getResources();

						setObjectField(param.thisObject, taskbarHeightField, res.getDimensionPixelSize(res.getIdentifier("taskbar_size", "dimen", mContext.getPackageName())));
						setObjectField(param.thisObject, stashedTaskbarHeightField, res.getDimensionPixelSize(res.getIdentifier("taskbar_stashed_size", "dimen", mContext.getPackageName())));

						if (taskbarHeightOverride != 1f) {
							setObjectField(param.thisObject, taskbarHeightField, Math.round(getIntField(param.thisObject, taskbarHeightField) * taskbarHeightOverride));
						}
					}
				});


		RecentAppsControllerClass.afterConstruction().run(param -> {
			if (GoogleRecents || (taskbarMode == TASKBAR_ON && TaskbarAsRecents && mIsA16Plus)) { //on 16+ we use the builtin recent tasks
				RecentAppsControllerClass.findMethods(
						Pattern.compile("setCanShowRecentApps")).stream().findFirst().get()
						.invoke(param.thisObject, true);
			}
		});

		//A16+
		RecentAppsControllerClass
				.before("computeShownRecentTasks")
				.run(param -> {
					if(taskbarMode == TASKBAR_ON && TaskbarAsRecents) {
						List<?> allRecentTasks = (List<?>) getObjectField(param.thisObject, "allRecentTasks");

						if (allRecentTasks.size() < 2) //there's nothing to show as recent
						{
							return;
						}

						List<?> shownHotseatItems = (List<?>) getObjectField(param.thisObject, "shownHotseatItems");
						if (!shownHotseatItems.isEmpty()) {
							shownHotseatItems.clear();
						}

						//we control the list ourselves to remove all suggestions
						param.setResult(allRecentTasks.subList(Math.max(0, allRecentTasks.size() - numShownHotseatIcons - 1), Math.max(allRecentTasks.size() - 1, 0)));
					}
				});

		if(mIsA16Plus) return; //from this point on, only A15- devices will be targeted

		ReflectedClass RecentTasksListClass = ReflectedClass.of("com.android.quickstep.RecentTasksList");
		ReflectedClass AppInfoClass = ReflectedClass.of("com.android.launcher3.model.data.AppInfo");
		ReflectedClass TaskbarViewClass = ReflectedClass.of("com.android.launcher3.taskbar.TaskbarView");
		ReflectedClass ItemInfoClass = ReflectedClass.of("com.android.launcher3.model.data.ItemInfo");
		ReflectedClass ActivityManagerWrapperClass = ReflectedClass.of("com.android.systemui.shared.system.ActivityManagerWrapper");
		ReflectedClass TaskbarModelCallbacksClass = ReflectedClass.of("com.android.launcher3.taskbar.TaskbarModelCallbacks");
		Method commitItemsToUIMethod = findMethodExact(TaskbarModelCallbacksClass.getClazz(), "commitItemsToUI");

		UID = (int) callMethod(Process.myUserHandle(), "getIdentifier");

		View.OnClickListener listener = view -> {
			try {
				int id = (int) getAdditionalInstanceField(view.getTag(), "taskId");
				callMethod(
						getStaticObjectField(ActivityManagerWrapperClass.getClazz(), "sInstance"),
						"startActivityFromRecents",
						id,
						null);
			} catch (Throwable ignored) {}
		};

		TaskbarViewClass
				.after("setClickAndLongClickListenersForIcon")
				.run(param -> {
					//Icon must be launched from recents
					if (taskbarMode == TASKBAR_ON
							&& TaskbarAsRecents
							&& mItemsLength > 0)
						((View) param.args[0]).setOnClickListener(listener);
				});

		TaskbarViewClass
				.afterConstruction()
				.run(param -> mTaskBarViews.add((ViewGroup) param.thisObject));

		Method updateHotseatItemsMethod = XposedHelpers.findMethodExact(
				TaskbarViewClass.getClazz(),
				"updateHotseatItems",
				"com.android.launcher3.model.data.ItemInfo[]"
		);
		mUpdateHotseatParams = updateHotseatItemsMethod == null ? 2 : 1;

		RecentTasksListClass
				.afterConstruction()
				.run(param -> recentTasksList = param.thisObject);

		RecentTasksListClass
				.before("onRecentTasksChanged")
				.run(param -> {
					if (taskbarMode != TASKBAR_ON
							|| !TaskbarAsRecents
							|| refreshing
							|| mTaskBarViews.isEmpty())
						return;
					new Thread(() -> {
						refreshing = true;
						SystemUtils.threadSleep(100);

						mTaskBarViews.forEach(taskBarView -> taskBarView.post(() -> {
							try {
								Object mSysUiProxy = getObjectField(param.thisObject, "mSysUiProxy");

								ArrayList<?> recentTaskList = (ArrayList<?>) callMethod(
										mSysUiProxy,
										"getRecentTasks",
										numShownHotseatIcons + 1,
										UID);

								if (mTasksFieldName == null) {
									for (Field f : recentTaskList.get(0).getClass().getDeclaredFields()) {
										if (f.getType().getName().contains("RecentTaskInfo")) {
											mTasksFieldName = f.getName();
										}
									}
								}
								if (mTasksFieldName == null) {
									for (Field f : recentTaskList.get(0).getClass().getDeclaredFields()) {
										if (f.getType().getName().contains("List"))
										{
											//noinspection unchecked
											List<Object> list = (List<Object>) f.get(recentTaskList.get(0));
											if(list != null && findFieldIfExists(list.get(0).getClass(), "isFocused") != null) {
												mTasksFieldName = f.getName();
												mTasksIsList = true;
											}
										}
									}
								}

								recentTaskList.removeIf(r ->
										(boolean) getObjectField(
												mTasksIsList
												? ((List<?>) getObjectField(r, mTasksFieldName)).get(0)
												: ((Object[]) getObjectField(r, mTasksFieldName))[0],
												"isFocused"
										)
								);

								if (recentTaskList.size() > numShownHotseatIcons)
									recentTaskList.remove(recentTaskList.size() - 1);


								Object[] itemInfos = (Object[]) Array.newInstance(
										ItemInfoClass.getClazz(),
										Math.min(numShownHotseatIcons, recentTaskList.size()));

								int prevItemsLength = mItemsLength;
								mItemsLength = itemInfos.length;
								if (mItemsLength == 0) {
									invokeOriginalMethod(commitItemsToUIMethod, TaskbarModelCallbacks, null);
									return;
								} else if (prevItemsLength == 0 && mItemsLength == 1) {
									taskBarView.removeAllViews(); //moving from suggested apps to recent apps. old ones are not valid anymore
								}

								for (int i = 0; i < itemInfos.length; i++) {
									TaskInfo taskInfo = mTasksIsList
											? (TaskInfo) ((List<?>) getObjectField(recentTaskList.get(i), mTasksFieldName)).get(0)
											: (TaskInfo) ((Object[]) getObjectField(recentTaskList.get(i), mTasksFieldName))[0];

									// noinspection ,JavaReflectionMemberAccess
									itemInfos[i] = AppInfoClass.getClazz().getConstructor(ComponentName.class, CharSequence.class, UserHandle.class, Intent.class)
											.newInstance(
													(ComponentName) getObjectField(taskInfo, "realActivity"),
													"",
													UserHandle.class.getConstructor(int.class).newInstance(getIntField(taskInfo, "userId")),
													(Intent) getObjectField(taskInfo, "baseIntent"));

									setAdditionalInstanceField(itemInfos[i], "taskId", taskInfo.taskId);
								}

								if (mUpdateHotseatParams == 2) //A15QPR1
								{
									callMethod(taskBarView, mUpdateItemsMethodName, itemInfos, new ArrayList<>());
								} else { //Older
									callMethod(taskBarView, mUpdateItemsMethodName, new Object[]{itemInfos});
								}

								int firstAppIcon = 2;
								int startPoint = taskBarView.getChildAt(firstAppIcon).getClass().getName().endsWith("SearchDelegateView") ? firstAppIcon + 1 : firstAppIcon;

								for (int i = 0; i < itemInfos.length; i++) {
									View iconView = taskBarView.getChildAt(i + startPoint);

									try {
										if (getAdditionalInstanceField(iconView, "taskId")
												.equals(getAdditionalInstanceField(itemInfos[itemInfos.length - i - 1], "taskId")))
											continue;
									} catch (Throwable ignored) {}

									setAdditionalInstanceField(iconView, "taskId", getAdditionalInstanceField(itemInfos[itemInfos.length - i - 1], "taskId"));
									callMethod(iconView, "applyFromApplicationInfo", itemInfos[itemInfos.length - i - 1]);
								}
							} catch (Throwable ignored) {}
						}));
						refreshing = false;
					}).start();
				});

		TaskbarModelCallbacksClass
				.afterConstruction()
				.run(param -> TaskbarModelCallbacks = param.thisObject);

		TaskbarModelCallbacksClass
				.before("commitItemsToUI")
				.run(param -> {
					if (taskbarMode != TASKBAR_ON || !TaskbarAsRecents) return;

					mTaskBarViews.forEach(taskBarView -> {
						if (taskBarView.getChildCount() == 0 && recentTasksList != null) {
							callMethod(recentTasksList, "onRecentTasksChanged");
						}
					});
					param.setResult(null);
				});
		//endregion
	}

	static class TaskbarViews {
		public List<WeakReference<ViewGroup>> mViews = new ArrayList<>();

		public void add(ViewGroup view) {
			cleanup();
			mViews.add(new WeakReference<>(view));
		}

		private void cleanup() {
			List<WeakReference<ViewGroup>> clean = new ArrayList<>();

			for (WeakReference<ViewGroup> ref : mViews) {
				if (ref.get() != null) {
					clean.add(ref);
				}
			}
			mViews = clean;
		}

		public void forEach(Consumer<ViewGroup> action) {
			for (WeakReference<ViewGroup> ref : mViews) {
				ViewGroup thisOne = ref.get();
				if (thisOne != null) {
					try {
						action.accept(thisOne);
					} catch (Throwable ignored) {}
				}
			}
		}

		public boolean isEmpty() {
			cleanup();
			return mViews.isEmpty();
		}
	}
}