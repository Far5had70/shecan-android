package ir.shecan.ui.fragment.refactor;

import static ir.shecan.core.util.AppUtils.adjustUIForFragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ir.shecan.BuildConfig;
import ir.shecan.R;
import ir.shecan.ui.activity.AuthorizeActivity;
import ir.shecan.ui.activity.MainActivityNew;
import ir.shecan.ui.activity.ThemeActivity;
import ir.shecan.ui.activity.UpdateProfileActivity;
import ir.shecan.ui.adapter.ProfileAdapter;
import ir.shecan.core.constant.Constant;
import ir.shecan.databinding.FragmentProfileBinding;
import ir.shecan.ui.fragment.ToolbarFragment;
import ir.shecan.data.modelDto.ProfileItem;
import ir.shecan.data.modelDto.VerifyApiViewModel;
import ir.shecan.data.storage.AppStorage;
import ir.shecan.core.util.AppUtils;

public class ProfileFragment extends ToolbarFragment {

    private FragmentProfileBinding binding;
    private AppStorage storage;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        storage = new AppStorage(getContext());

        binding.vpnStatusView.setCheckUrl("https://check.shecan.ir/");

        adjustUi();

        setupRecycler();

        return binding.getRoot();
    }

    private void adjustUi() {
        binding.btnExit.setOnClickListener(view -> {
            storage.clearAll();

            if (!isAdded()) return;
            updateUi();
            ((MainActivityNew) requireActivity()).setupCustomBottomBar();
        });

        String version = "نسخه: v " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")";
        binding.versionTv.setText(version);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (storage != null) {
            updateUi();
        }
        ((MainActivityNew) getActivity()).updateLoginInformation();
//        ((MainActivityNew) getActivity()).binding.customBar.select(2);
        adjustUIForFragment(getActivity(), R.color.profileBackground, R.color.mainBack);
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
            ((MainActivityNew) getActivity()).binding.customBar.select(1);
            ((MainActivityNew) getActivity()).updateFragment(1);
            ((MainActivityNew) getActivity()).currentTab = 1;
        }
    }

    private void setupRecycler() {
        List<ProfileItem> list = new ArrayList<>();
        list.add(new ProfileItem(R.drawable.ic_info, "حساب کاربری"));
        list.add(new ProfileItem(R.drawable.ic_info, "ظاهر برنامه"));
        list.add(new ProfileItem(R.drawable.ic_info, "تراکنش‌ها"));
        list.add(new ProfileItem(R.drawable.ic_info, "پشتیبانی دامنه‌ها"));
        list.add(new ProfileItem(R.drawable.ic_info, "تیکت‌ها"));
//        list.add(new ProfileItem(R.drawable.ic_info, "درباره"));

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
                    AppUtils.openUrl(AppUtils.buildRedirect(Constant.TransactionUrlRaw, getContext()), getActivity());

                    break;

                case 3:
                    // پشتیبانی دامنه‌ها
                    AppUtils.openUrl(AppUtils.buildRedirect(Constant.DomainUrlRaw, getContext()), getActivity());
                    break;

                case 4:
                    // تیکت‌ها
                    AppUtils.openUrl(AppUtils.buildRedirect(Constant.TicketUrlRaw, getContext()), getActivity());
                    break;

                case 5:
                    // درباره ما
                    AppUtils.openUrl("https://shecan.ir", getActivity());
                    break;
            }
        });

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);
    }

    private void openFragment(Class fragmentClass) {
        if (getActivity() instanceof MainActivityNew) {
            ((MainActivityNew) getActivity()).switchFragment(fragmentClass, true);
        }
    }

    @Override
    public void checkStatus() {

    }
}

