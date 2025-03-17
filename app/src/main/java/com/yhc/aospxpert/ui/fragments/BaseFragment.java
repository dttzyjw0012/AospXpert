package com.yhc.aospxpert.ui.fragments;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.yhc.aospxpert.R;

public abstract class BaseFragment extends Fragment {

	public NavController navController;

	protected boolean isBackButtonEnabled() {
		return true;
	}

	public boolean getBackButtonEnabled() {
		return isBackButtonEnabled();
	}

	public abstract String getTitle();

	@SuppressWarnings("deprecation")
	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.main_menu, menu);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		navController = NavHostFragment.findNavController(this);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		AppCompatActivity baseContext = (AppCompatActivity) getContext();
		Toolbar toolbar = view.findViewById(R.id.toolbar);

		if (baseContext != null) {
			if (toolbar != null) {
				baseContext.setSupportActionBar(toolbar);
				toolbar.setTitle(getTitle());
			}
			if (baseContext.getSupportActionBar() != null) {
				baseContext.getSupportActionBar().setDisplayHomeAsUpEnabled(getBackButtonEnabled());
			}
		}
	}
}
