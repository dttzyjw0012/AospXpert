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
import java.util.function.Consumer;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.yhc.aospxpert.BuildConfig;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.modpacks.XposedModPack;
import com.yhc.aospxpert.modpacks.utils.SystemUtils;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectedClass.ReflectionConsumer;
import com.yhc.aospxpert.modpacks.utils.toolkit.ReflectionTools;

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
	private boolean refreshing = false;
	private static float taskbarHeightOverride = 1f;
	private static float TaskbarRadiusOverride = 1f;

	private static boolean TaskbarHideAllAppsIcon = false;
	private Object model;
	String mTasksFieldName = null; // in case the code was obfuscated
	boolean mTasksIsList = false;
	private Object TaskbarModelCallbacks;
	private int mItemsLength = 0;
	private int mUpdateHotseatParams = 2;

	public TaskbarActivator(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {

		List<String> restartKeys = Arrays.asList(
				"taskBarMode",
				"TaskbarAsRecents",
				"TaskbarTransient",
				"taskbarHeightOverride",
				"TaskbarRadiusOverride",
				"TaskbarHideAllAppsIcon");

		if (Key.length > 0 && restartKeys.contains(Key[0])) {
			SystemUtils.killSelf();
		}

		taskbarMode = Integer.parseInt(Xprefs.getString("taskBarMode", String.valueOf(TASKBAR_DEFAULT)));

		TaskbarAsRecents = Xprefs.getBoolean("TaskbarAsRecents", false);
		TaskbarHideAllAppsIcon = true;//Xprefs.getBoolean("TaskbarHideAllAppsIcon", false);

		TaskbarRadiusOverride = Xprefs.getSliderFloat("TaskbarRadiusOverride", 1f);

		taskbarHeightOverride = Xprefs.getSliderFloat("taskbarHeightOverride", 100f) / 100f;

		taskbarMode = Integer.parseInt(Xprefs.getString("taskBarMode", "0"));

		TaskbarTransient = Xprefs.getBoolean("TaskbarTransient", false);

	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@SuppressLint("DiscouragedApi")
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {

		ReflectedClass RecentTasksListClass = ReflectedClass.of("com.android.quickstep.RecentTasksList");
		ReflectedClass AppInfoClass = ReflectedClass.of("com.android.launcher3.model.data.AppInfo");
		ReflectedClass TaskbarViewClass = ReflectedClass.of("com.android.launcher3.taskbar.TaskbarView");
		ReflectedClass ItemInfoClass = ReflectedClass.of("com.android.launcher3.model.data.ItemInfo");
		ReflectedClass TaskbarModelCallbacksClass = ReflectedClass.of("com.android.launcher3.taskbar.TaskbarModelCallbacks");
		ReflectedClass DeviceProfileClass = ReflectedClass.of("com.android.launcher3.DeviceProfile");
		ReflectedClass ActivityManagerWrapperClass = ReflectedClass.of("com.android.systemui.shared.system.ActivityManagerWrapper");
		ReflectedClass TaskbarActivityContextClass = ReflectedClass.of("com.android.launcher3.taskbar.TaskbarActivityContext");
		ReflectedClass LauncherModelClass = ReflectedClass.of("com.android.launcher3.LauncherModel");
		ReflectedClass BaseDraggingActivityClass = ReflectedClass.of("com.android.launcher3.BaseDraggingActivity");
		ReflectedClass DisplayControllerClass = ReflectedClass.of("com.android.launcher3.util.DisplayController");
		ReflectedClass DisplayControllerInfoClass = ReflectedClass.of("com.android.launcher3.util.DisplayController$Info");
		Method commitItemsToUIMethod = findMethodExact(TaskbarModelCallbacksClass.getClazz(), "commitItemsToUI");

		DisplayControllerInfoClass
				.before("isTablet")
				.run(param -> {
					if (taskbarMode == TASKBAR_DEFAULT) return;

					param.setResult(taskbarMode == TASKBAR_ON);
				});

		DisplayControllerClass
				.before("isTransientTaskbar")
				.run(param -> {
					if (taskbarMode == TASKBAR_ON)
						param.setResult(TaskbarTransient);
				});

		BaseDraggingActivityClass
				.after("onResume")
				.run(param -> {
					if (taskbarMode == TASKBAR_ON && model != null) {
						XposedHelpers.callMethod(model, "onAppIconChanged", BuildConfig.APPLICATION_ID, UserHandle.getUserHandleForUid(0));
					}
				});

		LauncherModelClass
				.afterConstruction()
				.run(param -> model = param.thisObject);

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

		//region recentbar
		UID = (int) callMethod(Process.myUserHandle(), "getIdentifier");

		View.OnClickListener listener = view -> {
			try {
				int id = (int) getAdditionalInstanceField(view.getTag(), "taskId");
				callMethod(
						getStaticObjectField(ActivityManagerWrapperClass.getClazz(), "sInstance"),
						"startActivityFromRecents",
						id,
						null);
			} catch (Throwable ignored) {
			}
		};

		String taskbarHeightField = findFieldIfExists(DeviceProfileClass.getClazz(), "taskbarSize") != null
				? "taskbarSize" //pre 13 QPR3
				: "taskbarHeight"; //13 QPR3

		String stashedTaskbarHeightField = findFieldIfExists(DeviceProfileClass.getClazz(), "stashedTaskbarSize") != null
				? "stashedTaskbarSize" //pre 13 QPR3
				: "stashedTaskbarHeight"; //13 QPR3

		DeviceProfileClass
				.afterConstruction()
				.run(param -> {
					if (taskbarMode == TASKBAR_DEFAULT) return;

					boolean taskbarEnabled = taskbarMode == TASKBAR_ON;

//				setObjectField(param.thisObject, "isTaskbarPresent", taskbarEnabled);

					if (taskbarEnabled) {
						numShownHotseatIcons = getIntField(param.thisObject, "numShownHotseatIcons") +
								(TaskbarHideAllAppsIcon
										? 1
										: 0);

						Resources res = mContext.getResources();

						setObjectField(param.thisObject, taskbarHeightField, res.getDimensionPixelSize(res.getIdentifier("taskbar_size", "dimen", mContext.getPackageName())));
						setObjectField(param.thisObject, stashedTaskbarHeightField, res.getDimensionPixelSize(res.getIdentifier("taskbar_stashed_size", "dimen", mContext.getPackageName())));

						if (taskbarHeightOverride != 1f) {
							setObjectField(param.thisObject, taskbarHeightField, Math.round(getIntField(param.thisObject, taskbarHeightField) * taskbarHeightOverride));
						}
					}
				});

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
				.run(param -> {
					mTaskBarViews.add((ViewGroup) param.thisObject);

					try { //Since A15QPR1 button is now a container and can't be null anymore. removing it manually only from recents
						if (taskbarMode == TASKBAR_ON && TaskbarHideAllAppsIcon) {
							mTaskBarViews.forEach(view -> setObjectField(view, "mAllAppsButton", null));
						}
					} catch (Throwable ignored) {
					}
				});

		Method updateHotseatItemsMethod = XposedHelpers.findMethodExact(
				TaskbarViewClass.getClazz(),
				"updateHotseatItems",
				"com.android.launcher3.model.data.ItemInfo[]"
		);
		mUpdateHotseatParams = updateHotseatItemsMethod == null ? 2 : 1;
		RecentTasksListClass.afterConstruction().run(param -> recentTasksList = param.thisObject);

		TaskbarViewClass
				.after("updateHotseatItems")
				.run(param -> {
					if(TaskbarAsRecents) {
						try {
							View container = (View) getObjectField(param.thisObject, "mAllAppsButtonContainer");
							ViewGroup taskbarView = (ViewGroup) param.thisObject;
							taskbarView.removeView(container);

							container = (View) getObjectField(param.thisObject, "mTaskbarDividerContainer");
							taskbarView.removeView(container);
						} catch (Throwable ignored) {}
					}
				});

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
										if (f.getType().getName().contains("List")) {
											mTasksFieldName = f.getName();
											mTasksIsList = true;
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
									callMethod(taskBarView, "updateHotseatItems", itemInfos, new ArrayList<>());
								} else { //Older
									callMethod(taskBarView, "updateHotseatItems", new Object[]{itemInfos});
							}

								int startPoint = taskBarView.getChildAt(0).getClass().getName().endsWith("SearchDelegateView") ? 1 : 0;

								for (int i = 0; i < itemInfos.length; i++) {
									View iconView = taskBarView.getChildAt(i + startPoint);

									try {
										if (getAdditionalInstanceField(iconView, "taskId")
												.equals(getAdditionalInstanceField(itemInfos[itemInfos.length - i - 1], "taskId")))
											continue;
									} catch (Throwable ignored) {
									}

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
					} catch (Throwable ignored) {
					}
				}
			}
		}

		public boolean isEmpty() {
			cleanup();
			return mViews.isEmpty();
		}
	}
}