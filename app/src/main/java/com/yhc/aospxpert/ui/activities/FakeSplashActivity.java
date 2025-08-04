package com.yhc.aospxpert.ui.activities;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

@SuppressLint("CustomSplashScreen")
public class FakeSplashActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
		super.onCreate(savedInstanceState);

		splashScreen.setKeepOnScreenCondition(() -> true);
		Intent receivedIntent = getIntent();
		Intent splashIntent = new Intent(FakeSplashActivity.this, SplashScreenActivity.class);
		if (receivedIntent != null) {
			ComponentName cn = receivedIntent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName.class);
			if (cn != null) {
				splashIntent.putExtra(Intent.EXTRA_COMPONENT_NAME, cn);
			}
		}
		startActivity(splashIntent);
		finish();
	}
}
