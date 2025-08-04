package com.yhc.aospxpert.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yhc.aospxpert.databinding.FragmentBlankBinding;

public class BlankFragment extends BaseFragment {

    private FragmentBlankBinding binding;

    @Override
    public boolean isBackButtonEnabled() {
        return false;
    }

    @Override
    public String getTitle() {
        return "";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBlankBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }
}
