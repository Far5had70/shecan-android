package ir.shecan.fragment.refactor;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import ir.shecan.R;
import ir.shecan.activity.AuthorizeActivity;
import ir.shecan.activity.MainActivityNew;
import ir.shecan.activity.ThemeActivity;
import ir.shecan.activity.UpdateProfileActivity;
import ir.shecan.adapter.ProfileAdapter;
import ir.shecan.databinding.FragmentProfileBinding;
import ir.shecan.fragment.ToolbarFragment;
import ir.shecan.modelDto.ProfileItem;
import ir.shecan.modelDto.VerifyApiViewModel;
import ir.shecan.storage.AppStorage;

public class ProfileFragment extends ToolbarFragment {

    private FragmentProfileBinding binding;
    private AppStorage storage;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        storage = new AppStorage(getContext());

        setupRecycler();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (storage != null) {
            updateUi();
        }
        ((MainActivityNew) getActivity()).updateLoginInformation();
        ((MainActivityNew) getActivity()).binding.customBar.select(2);
    }

    private void updateUi() {
        VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);
        if (token != null) {
            binding.tvFullName.setText(String.format("%s %s", token.getFirstname(), token.getLastname()));
            binding.tvEmail.setText(String.format("%s", token.getMail()));
            binding.tvPhoneNumber.setText(String.format("%s", token.getLogin()));
        } else {
            getActivity().startActivity(new Intent(getActivity(), AuthorizeActivity.class));
        }
    }

    private void setupRecycler() {
        List<ProfileItem> list = new ArrayList<>();
        list.add(new ProfileItem(R.drawable.ic_info, "حساب کاربری"));
        list.add(new ProfileItem(R.drawable.ic_info, "ظاهر برنامه"));
        list.add(new ProfileItem(R.drawable.ic_info, "تراکنش‌ها"));
        list.add(new ProfileItem(R.drawable.ic_info, "پشتیبانی دامنه‌ها"));
        list.add(new ProfileItem(R.drawable.ic_info, "تیکت‌ها"));
        list.add(new ProfileItem(R.drawable.ic_info, "درباره"));
        list.add(new ProfileItem(R.drawable.ic_info, "خروج"));

        ProfileAdapter adapter = new ProfileAdapter(list, (position, item) -> {
            switch (position) {
                case 0:
//                    openFragment(AccountFragment.class);
                    getActivity().startActivity(new Intent(getActivity(), UpdateProfileActivity.class));
                    break;
                case 1:
                    getActivity().startActivity(new Intent(getActivity(), ThemeActivity.class));
                    break;

                case 2:
                    // تراکنش‌ها
                    openUrl("https://my.shecan.ir/new-dashboard/transactions");
                    break;

                case 3:
                    // پشتیبانی دامنه‌ها
                    openUrl("https://my.shecan.ir/new-dashboard/domains");
                    break;

                case 4:
                    // تیکت‌ها
                    openUrl("https://my.shecan.ir/new-dashboard/support");
                    break;

                case 5:
                    // درباره ما
                    openUrl("https://shecan.ir");
                    break;

                case 6:
                    storage.clearAll();
                    updateUi();
                    break;
            }
        });

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);
    }

    private void openFragment(Class fragmentClass) {
        if (getActivity() instanceof MainActivityNew) {
            ((MainActivityNew) getActivity()).switchFragment(fragmentClass, false, true);
        }
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(android.net.Uri.parse(url));
        startActivity(intent);
    }

    @Override
    public void checkStatus() {

    }
}

