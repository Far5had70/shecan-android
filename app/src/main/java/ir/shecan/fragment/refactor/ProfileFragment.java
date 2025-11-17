package ir.shecan.fragment.refactor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import ir.shecan.R;
import ir.shecan.adapter.ProfileAdapter;
import ir.shecan.databinding.FragmentProfileBinding;
import ir.shecan.fragment.ToolbarFragment;
import ir.shecan.model.ProfileItem;

public class ProfileFragment extends ToolbarFragment {

    private FragmentProfileBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        setupRecycler();

        return binding.getRoot();
    }

    private void setupRecycler() {
        List<ProfileItem> list = new ArrayList<>();
        list.add(new ProfileItem(R.drawable.ic_info, "حساب کاربری"));
        list.add(new ProfileItem(R.drawable.ic_info, "ظاهر برنامه"));
        list.add(new ProfileItem(R.drawable.ic_info, "تراکنش‌ها"));
        list.add(new ProfileItem(R.drawable.ic_info, "پشتیبانی دامنه‌ها"));
        list.add(new ProfileItem(R.drawable.ic_info, "تیکت‌ها"));
        list.add(new ProfileItem(R.drawable.ic_info, "درباره"));

        ProfileAdapter adapter = new ProfileAdapter(list, (position, item) -> {
            switch (position) {
                case 0:
                    // حساب کاربری
                    break;
                case 1:
                    // ظاهر برنامه
                    break;
                case 2:
                    // تراکنش‌ها
                    break;
                case 3:
                    // دامنه‌ها
                    break;
                case 4:
                    // تیکت‌ها
                    break;
                case 5:
                    // درباره
                    break;
            }
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
    public void checkStatus() { }
}
