package ir.shecan.activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import ir.shecan.R;
import ir.shecan.adapter.ThemeAdapter;
import ir.shecan.databinding.ActivityThemeBinding;
import ir.shecan.databinding.ActivityUpdatePrrofileBinding;
import ir.shecan.modelDto.AppConfig;
import ir.shecan.modelDto.ThemeItem;
import ir.shecan.storage.AppStorage;
import ir.shecan.widget.ProfileTabBar;

public class UpdateProfileActivity extends AppCompatActivity {

    private ActivityUpdatePrrofileBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityUpdatePrrofileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.back.setVisibility(VISIBLE);
        binding.toolbar.back.setOnClickListener(view -> {
            onBackPressed();
        });

        binding.profileTabBar.setOnProfileTabSelected(new ProfileTabBar.OnProfileTabSelected() {
            @Override
            public void onTabSelected(int index) {
                switch (index){
                    case 1:
                        binding.profileTab.setVisibility(VISIBLE);
                        binding.passwordTab.setVisibility(GONE);
                        break;
                    case 2:
                        binding.profileTab.setVisibility(GONE);
                        binding.passwordTab.setVisibility(VISIBLE);
                        break;
                }
            }
        });

    }
}
