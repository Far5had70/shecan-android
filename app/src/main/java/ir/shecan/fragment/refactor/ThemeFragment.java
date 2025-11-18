package ir.shecan.fragment.refactor;

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
import ir.shecan.model.ThemeItem;

public class ThemeFragment extends ToolbarFragment {

    private FragmentThemeBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentThemeBinding.inflate(inflater, container, false);

        setupRecycler();

        return binding.getRoot();
    }

    private void setupRecycler() {
        List<ThemeItem> list = new ArrayList<>();
        list.add(new ThemeItem(getString(R.string.lightMode), AppCompatDelegate.MODE_NIGHT_NO));
        list.add(new ThemeItem(getString(R.string.darkMode), AppCompatDelegate.MODE_NIGHT_YES));
        list.add(new ThemeItem(getString(R.string.systemSetting), AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));

        ThemeAdapter adapter = new ThemeAdapter(list, item -> {
            saveThemeMode(item.getMode());
            AppCompatDelegate.setDefaultNightMode(item.getMode());
            requireActivity().recreate();
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

    private void saveThemeMode(int mode) {
        requireContext()
                .getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit()
                .putInt("theme_mode", mode)
                .apply();
    }
}

