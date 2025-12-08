package ir.shecan.ui.fragment.refactor;

import static android.view.View.GONE;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import ir.shecan.core.util.AppUtils;
import ir.shecan.data.api.ApiCallback;
import ir.shecan.data.api.AuthApi;
import ir.shecan.data.modelDto.BannerViewModel;
import ir.shecan.data.modelDto.HomePage;
import ir.shecan.data.modelDto.VerifyApiViewModel;
import ir.shecan.ui.activity.MainActivityNew;
import ir.shecan.ui.adapter.ServiceAdapter;
import ir.shecan.databinding.FragmentConfigListBinding;
import ir.shecan.ui.fragment.ToolbarFragment;
import ir.shecan.ui.fragment.bottomSheet.SubscriptionBottomSheet;
import ir.shecan.data.modelDto.IssuesViewModel;
import ir.shecan.data.modelDto.ServiceItem;
import ir.shecan.data.modelDto.ServiceItemMapper;
import ir.shecan.data.storage.AppStorage;

public class ConfigListFragment extends ToolbarFragment {

    private FragmentConfigListBinding binding;
    MainActivityNew activity;

    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentConfigListBinding.inflate(inflater, container, false);
        activity = (MainActivityNew) getActivity();

        AppStorage appStorage = new AppStorage(getContext());
        List<ServiceItem> serviceItems = buildServiceItems(appStorage);

        setupRecyclerView(serviceItems, appStorage);

        VerifyApiViewModel token = appStorage.getToken(VerifyApiViewModel.class);
        if (token == null || token.getApiKey() == null) {
            binding.dividerHeader.setVisibility(GONE);
            if (activity.bannerUrl == null) updateBanner();
            else handleBannerImage(activity.bannerUrl);
        }else {
            binding.bannerSlider.setVisibility(GONE);
        }

        binding.swipeRefresh.setOnRefreshListener(() -> {
            refreshPage();
        });

        return binding.getRoot();
    }

    private void refreshPage() {
        AppStorage storage = new AppStorage(getContext());
        VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);

        if (token != null && token.getApiKey() != null) {

            AuthApi auth = new AuthApi(getContext());

            auth.issues(
                    token.getApiKey(),
                    0, 1000,
                    new ApiCallback<IssuesViewModel>() {
                        @Override
                        public void onSuccess(IssuesViewModel res, boolean fromCache) {
                            // ذخیره دیتا
                            storage.saveIssues(res);

                            // ساخت لیست جدید
                            List<ServiceItem> items = buildServiceItems(storage);
                            setupRecyclerView(items, storage);

                            // ریفرش بنر (در صورت نیاز)
                            updateBanner();

                            binding.swipeRefresh.setRefreshing(false);
                        }

                        @Override
                        public void onError(int statusCode, String message) {
                            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                            binding.swipeRefresh.setRefreshing(false);
                        }
                    }
            );

        } else {
            // کاربر لاگین نیست → فقط بنر را ریفرش کن
            updateBanner();
            binding.swipeRefresh.setRefreshing(false);
        }
    }


    private List<ServiceItem> buildServiceItems(AppStorage appStorage) {
        IssuesViewModel viewModel = appStorage.getIssue(IssuesViewModel.class);

        List<IssuesViewModel.IssuesDTO> issues = (viewModel != null && viewModel.getIssues() != null)
                ? new ArrayList<>(viewModel.getIssues())
                : new ArrayList<>();

        // آیتم پیش‌فرض free
        issues.add(IssuesViewModel.IssuesDTO.createDefault());

        List<ServiceItem> items = new ArrayList<>();
        for (IssuesViewModel.IssuesDTO issue : issues) {
            items.add(ServiceItemMapper.map(getContext(), issue));
        }

        return items;
    }

    private void setupRecyclerView(List<ServiceItem> items, AppStorage appStorage) {
        ServiceItem savedItem = appStorage.getServiceStatus(ServiceItem.class);

        int defaultSelected = -1;
        if (savedItem != null) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getOrderCode().equals(savedItem.getOrderCode())) {
                    defaultSelected = i;
                    break;
                }
            }
        }

        ServiceAdapter adapter = new ServiceAdapter(
                getContext(),
                items,
                new ServiceAdapter.OnMoreClickListener() {
                    @Override
                    public void onBackgroundClicked(ServiceItem item) {
                        appStorage.saveServiceStatus(item);
                    }

                    @Override
                    public void onOptionClicked(ServiceItem item) {
                        SubscriptionBottomSheet bottomSheet = SubscriptionBottomSheet.newInstance(item);
                        bottomSheet.show(getParentFragmentManager(), "subscription_sheet");
                    }
                }
        );

        adapter.setSelectedPosition(defaultSelected);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) binding.bannerSlider.stop();
        binding = null;
    }

    @Override
    public void checkStatus() {
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivityNew) getActivity()).binding.customBar.select(0);
    }

    public void updateBanner() {
        AuthApi auth = new AuthApi(getContext());

        auth.homePageApi(
                new ApiCallback<HomePage>() {
                    @Override
                    public void onSuccess(HomePage res, boolean fromCache) {
                        auth.bannerList(
                                res.getBannerService().getAndroid(),
                                new ApiCallback<List<BannerViewModel>>() {
                                    @Override
                                    public void onSuccess(List<BannerViewModel> list, boolean fromCache) {
                                        if (!isAdded()) return;
                                        activity.bannerUrl = list;
                                        handleBannerImage(list);
                                    }

                                    @Override
                                    public void onError(int statusCode, String message) {
                                        if (!isAdded()) return;
                                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                                    }
                                }
                        );
                    }

                    @Override
                    public void onError(int statusCode, String message) {

                    }
                }
        );
    }


    private void handleBannerImage(List<BannerViewModel> banners) {

        if (!isAdded() || binding == null) return;

        List<String> slideList = new ArrayList<>();

        for (BannerViewModel banner : banners) {
            if (banner.getType() == 1) {
                slideList.add(banner.getImageURL());
            } else if (banner.getType() == 2) {
                slideList.add(banner.getImageBase64());
            }
        }

        binding.bannerSlider.setBanners(banners);
        binding.bannerSlider.start();

        binding.bannerSlider.getImageView().setOnClickListener(v -> {
            BannerViewModel banner = binding.bannerSlider.getCurrentBanner();
            if (banner != null && banner.getUrl() != null && !banner.getUrl().isEmpty()) {
                AppUtils.openUrl(banner.getUrl(), getActivity());
            }
        });
    }
}
