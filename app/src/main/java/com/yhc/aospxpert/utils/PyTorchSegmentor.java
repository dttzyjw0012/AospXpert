package com.yhc.aospxpert.utils;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static com.yhc.aospxpert.R.string.download_channel_name;
import static com.yhc.aospxpert.ui.Constants.ASSETS_DOWNLOADING_ID;
import static com.yhc.aospxpert.ui.Constants.DOWNLOAD_CHANNEL_ID;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.PRDownloader;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.yhc.aospxpert.BuildConfig;
import com.yhc.aospxpert.R;

public class PyTorchSegmentor {

	private static final String TAG = "PyTorchSegmentor";
	private static final String PYTORCH_LIB = "libpytorch_jni_lite.so";
	private static final String LIB_BASE_URL = "https://github.com/siavash79/AospXpert/raw/refs/heads/canary/app/lib/";
	private static final String MODEL_FILENAME = "u2net.ptl";
	private static final String MODEL_BASE_URL = "https://github.com/siavash79/AospXpert/raw/refs/heads/canary/app/pytorchModel/";
	private static final Set<String> activeNotifications = new HashSet<>();

	public static Bitmap extractSubject(Context context, Bitmap input)
	{
		try {
			if (!loadAssets(context)) return null;

			String modelPath = String.format("%s/%s", context.getCacheDir().getAbsolutePath(), MODEL_FILENAME);

			return new PyTorchBackgroundRemover(modelPath).removeBackground(input);
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static void createNotificationChannel(Context context) {
		NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

		notificationManager.createNotificationChannel(new NotificationChannel(DOWNLOAD_CHANNEL_ID, context.getString(download_channel_name), IMPORTANCE_DEFAULT));
	}


	/**Loads assets from web. returns true if everything is pre-loaded and false if anything is still ongoing*/
	public static boolean loadAssets(Context context) {
		createNotificationChannel(context);

		boolean libLoaded = loadPyTorchLibrary(context);
		boolean modelLoaded = downloadAIModel(context);

		return libLoaded && modelLoaded;
	}

	private static boolean downloadAIModel(Context context) {
		String AIPath = String.format("%s/%s", context.getCacheDir(), MODEL_FILENAME);
		if(new File(AIPath).exists()) return true;

		String downloadURL = String.format("%s/%s", MODEL_BASE_URL, MODEL_FILENAME);

		downloadFile(downloadURL, AIPath, "ai_model", context);

		return false;
	}

	private static void downloadFile(String downloadURL, String destPath, String notificationTag, Context context) {
		if (!activeNotifications.contains(notificationTag)) {
			activeNotifications.add(notificationTag);

			NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
			Pair<NotificationCompat.Builder, Notification> notificationPair = postNotification(context, notificationManager, notificationTag);
			NotificationCompat.Builder notificationBuilder = notificationPair.first;
			Notification[] notification = {notificationPair.second};

			try {
				File tempFile = File.createTempFile("DLTmp", "tmp");
				final long[] lastUpdateTime = {0};

				//noinspection DataFlowIssue
				PRDownloader
						.download(downloadURL, tempFile.getParentFile().getAbsolutePath(), tempFile.getName())
						.build()
						.setOnProgressListener(progress -> {
							long currentTime = SystemClock.elapsedRealtime();
							if (currentTime - lastUpdateTime[0] >= 1000) {
								long progressPercent = progress.currentBytes * 100 / progress.totalBytes;
								String progressText = getProgressDisplayLine(progress.currentBytes, progress.totalBytes);

								notificationBuilder
										.setContentText(String.format("%s\n%s", context.getText(R.string.assets_download_body), context.getString(R.string.assets_download_size_body, progressText)))
										.setProgress(100, (int) progressPercent, false);

								notification[0] = notificationBuilder.build();
								notification[0].flags |= Notification.FLAG_ONLY_ALERT_ONCE;
								notificationManager.notify(notificationTag, ASSETS_DOWNLOADING_ID, notification[0]);
								lastUpdateTime[0] = currentTime;
							}
						})
						.start(new OnDownloadListener() {
							@Override
							public void onDownloadComplete() {
								//noinspection ResultOfMethodCallIgnored
								tempFile.renameTo(new File(destPath));

								removeNotification(context, notificationTag);
								Log.i(TAG, String.format("PRDownloader %s download completed successfully", notificationTag));

								try {
									Intent intent = new Intent(BuildConfig.APPLICATION_ID + ".ACTION_MODEL_DOWNLOADED");
									LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
								} catch (Throwable ignored) {}
							}

							@Override
							public void onError(Error error) {
								//noinspection ResultOfMethodCallIgnored
								tempFile.delete();
								removeNotification(context, notificationTag);

								StringBuilder logMessage = new StringBuilder(String.format("PRDownloader %s download failed:\n", notificationTag));
								if (error.isConnectionError()) {
									logMessage.append(String.format(Locale.ENGLISH, "Connection Exception: %s\n", error.getConnectionException()));
								}
								if (error.isServerError()) {
									logMessage.append(String.format(Locale.ENGLISH, "Response Code: %d\nServer Error Message: %s\n", error.getResponseCode(), error.getServerErrorMessage()));
								}

								Log.e(TAG, logMessage.toString().trim());
							}
						});
			} catch (Throwable throwable) {
				Log.e(TAG, "downloadFile: ", throwable);
			}
		}
	}

	private static Pair<NotificationCompat.Builder, Notification> postNotification(Context context, NotificationManager notificationManager, String tag) {
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
				.setSmallIcon(R.drawable.ic_notification_foreground)
				.setContentTitle(context.getText(R.string.assets_download_title))
				.setContentText(context.getText(R.string.assets_download_body))
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)
				.setOngoing(true)
				.setProgress(100, 0, false)
				.setAutoCancel(false);
		Notification notification = notificationBuilder.build();

		notificationManager.notify(tag, ASSETS_DOWNLOADING_ID, notification);

		return new Pair<>(notificationBuilder, notification);
	}

	private static void removeNotification(Context context, String tag) {
		NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

		notificationManager.cancel(tag, ASSETS_DOWNLOADING_ID);
		activeNotifications.remove(tag);
	}

	@SuppressLint("UnsafeDynamicallyLoadedCode")
	private static boolean loadPyTorchLibrary(Context context) {
		String libPath = String.format("%s/%s", context.getCacheDir(), PYTORCH_LIB);
		if(new File(libPath).exists())
		{
			try {
				System.loadLibrary("fbjni");
				System.load(libPath);
				return true;
			} catch (Throwable throwable) {
				Log.e(TAG, "loadPyTorchLibrary: ", throwable);
			}
		}

		downloadLibrary(context);
		return false;
	}

	private static void downloadLibrary(Context context) {
		String architecture = Build.SUPPORTED_ABIS[0];
		String downloadURL = String.format("%s%s/%s", LIB_BASE_URL, architecture, PYTORCH_LIB);
		String libPath = String.format("%s/%s", context.getCacheDir(), PYTORCH_LIB);

		downloadFile(downloadURL, libPath, "ai_lib", context);
	}

	private static String getProgressDisplayLine(long currentBytes, long totalBytes) {
		return String.format("%s / %s", getBytesToMBString(currentBytes), getBytesToMBString(totalBytes));
	}

	private static String getBytesToMBString(long bytes) {
		return String.format(Locale.ENGLISH, "%.2f MB", bytes / (1024.00 * 1024.00));
	}
}