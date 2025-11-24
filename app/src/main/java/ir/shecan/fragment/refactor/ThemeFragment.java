package ir.shecan.fragment.refactor;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import ir.shecan.R;
import ir.shecan.adapter.ThemeAdapter;
import ir.shecan.databinding.FragmentThemeBinding;
import ir.shecan.fragment.ToolbarFragment;
import ir.shecan.modelDto.ThemeItem;

public class ThemeFragment extends ToolbarFragment {

    private FragmentThemeBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentThemeBinding.inflate(inflater, container, false);

        setupRecycler(getContext());

        return binding.getRoot();
    }

    private void setupRecycler(Context context) {
        List<ThemeItem> list = new ArrayList<>();
        list.add(new ThemeItem(getString(R.string.lightMode), AppCompatDelegate.MODE_NIGHT_NO));
        list.add(new ThemeItem(getString(R.string.darkMode), AppCompatDelegate.MODE_NIGHT_YES));
        list.add(new ThemeItem(getString(R.string.systemSetting), AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));

        int mode = getActivity().getSharedPreferences("settings", MODE_PRIVATE).getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        ThemeAdapter adapter = new ThemeAdapter(context, mode, list, item -> {
            saveThemeMode(item.getMode());
            AppCompatDelegate.setDefaultNightMode(item.getMode());
            updateThemeWithoutRecreate();
        });

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void checkStatus() {
    }

    private void updateThemeWithoutRecreate() {
        ViewGroup root = (ViewGroup) binding.getRoot().getParent();

        if (root != null) {
            // پاک کردن محتویات قبلی
            root.removeAllViews();

            // دوباره Inflate کردن کل صفحه با Theme جدید
            LayoutInflater inflater = LayoutInflater.from(requireContext());
            binding = FragmentThemeBinding.inflate(inflater, root, false);

            // اضافه کردن دوباره
            root.addView(binding.getRoot());

            // دوباره مقداردهی لیست
            setupRecycler(getContext());
        }
    }

    private void saveThemeMode(int mode) {
        getActivity()
                .getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit()
                .putInt("theme_mode", mode)
                .apply();
    }
}

