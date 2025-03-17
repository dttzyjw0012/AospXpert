package com.yhc.aospxpert.service;

import static com.yhc.aospxpert.modpacks.Constants.AI_METHOD_MLKIT;
import static com.yhc.aospxpert.modpacks.Constants.AI_METHOD_PYTORCH;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import com.topjohnwu.superuser.Shell;

import java.util.Arrays;
import java.util.List;

import com.yhc.aospxpert.IRootProviderProxy;
import com.yhc.aospxpert.AOSPXpert;
import com.yhc.aospxpert.R;
import com.yhc.aospxpert.modpacks.Constants;
import com.yhc.aospxpert.utils.PyTorchSegmentor;
import com.yhc.aospxpert.utils.MLKitSegmentor;

public class RootProviderProxy extends Service {
	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return new RootProviderProxyIPC(this);
	}

	class RootProviderProxyIPC extends IRootProviderProxy.Stub
	{
		/** @noinspection unused*/
		String TAG = getClass().getSimpleName();

		private final List<String> rootAllowedPacks;
		private final boolean rootGranted;

		private RootProviderProxyIPC(Context context)
		{
			try {
				Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER));
			}
			catch (Throwable ignored){}
			rootGranted = Shell.getShell().isRoot();

			if(!rootGranted)
			{
				context.sendBroadcast(new Intent(Constants.ACTION_KSU_ACQUIRE_ROOT));
			}

			rootAllowedPacks = Arrays.asList(context.getResources().getStringArray(R.array.root_requirement));
		}

		/** @noinspection RedundantThrows*/
		@Override
		public String[] runCommand(String command) throws RemoteException {
			try {
				ensureEnvironment();

				List<String> result = Shell.cmd(command).exec().getOut();
				return result.toArray(new String[0]);
			}
			catch (Throwable t)
			{
				return new String[0];
			}
		}

		@Override
		public Bitmap extractSubject(Bitmap input, int method) throws RemoteException {
			ensureEnvironment();

			if(!AOSPXpert.get().isCoreRootServiceBound())
			{
				AOSPXpert.get().tryConnectRootService();
			}

			switch (method)
			{
				case AI_METHOD_MLKIT:
					return MLKitSegmentor.extractSubject(AOSPXpert.get(), input);
				case AI_METHOD_PYTORCH:
					return PyTorchSegmentor.extractSubject(AOSPXpert.get(), input);
			}

			return null;
		}

		private void ensureEnvironment() throws RemoteException {
			if(!rootGranted)
			{
				throw new RemoteException("Root permission denied");
			}

			ensureSecurity(Binder.getCallingUid());
		}

		private void ensureSecurity(int uid) throws RemoteException {
			for (String packageName : getPackageManager().getPackagesForUid(uid)) {
				if(rootAllowedPacks.contains(packageName))
					return;
			}
			throw new RemoteException("You do know you're not supposed to use this service. So...");
		}
	}
}