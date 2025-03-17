package com.yhc.aospxpert;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.google.android.material.color.DynamicColors;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ipc.RootService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.yhc.aospxpert.service.RootProvider;

public class AOSPXpert extends Application {

	/** @noinspection unused*/
	private static final String TAG = "AospXpertSingleton";
	private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

	private static AOSPXpert instance;
	private boolean mCoreRootServiceBound = false;
	public final CountDownLatch mRootServiceConnected = new CountDownLatch(1);

	private ServiceConnection mCoreRootServiceConnection;
	private IRootProviderService mCoreRootService;


	public void onCreate() {
		super.onCreate();
		instance = this;

		tryConnectRootService();
		DynamicColors.applyToActivitiesIfAvailable(this);
	}

	/** @noinspection unused*/
	public IRootProviderService getRootService()
	{
		return mCoreRootService;
	}

	public static AOSPXpert get() {
		if (instance == null) {
			instance = new AOSPXpert();
		}
		return instance;
	}

	/** @noinspection BooleanMethodIsAlwaysInverted*/
	public boolean isCoreRootServiceBound() {
		return mCoreRootServiceBound;
	}

	public boolean hasRootAccess()
	{
		return Shell.getShell().isRoot();
	}

	public void tryConnectRootService()
	{
		new Thread(() -> {
			for (int i = 0; i < 2; i++) {
				if (connectRootService())
					break;
			}
		}).start();
	}

	private boolean connectRootService() {
		try {
			// Start RootService connection
			Intent intent = new Intent(this, RootProvider.class);
			mCoreRootServiceConnection = new ServiceConnection() {
				@Override
				public void onServiceConnected(ComponentName name, IBinder service) {
					mCoreRootServiceBound = true;
					mRootServiceConnected.countDown();
					mCoreRootService = IRootProviderService.Stub.asInterface(service);
				}

				@Override
				public void onServiceDisconnected(ComponentName name) {
					mCoreRootServiceBound = false;
					mRootServiceConnected.countDown();
				}
			};

			mainThreadHandler.post(() -> RootService.bind(intent, mCoreRootServiceConnection));

			return mRootServiceConnected.await(5, TimeUnit.SECONDS);
		} catch (Exception ignored) {
			return false;
		}
	}

}
