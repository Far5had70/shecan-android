package ir.shecan.ui.fragment.refactor;

import static android.view.View.GONE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ir.shecan.core.util.AppUtils;
import ir.shecan.core.util.DynamicBannerRequestFactory;
import ir.shecan.core.util.ToastManager;
import ir.shecan.core.util.TrackingUtils;
import ir.shecan.data.api.ApiCallback;
import ir.shecan.data.api.AuthApi;
import ir.shecan.data.modelDto.BannerViewModel;
import ir.shecan.data.modelDto.HomePage;
import ir.shecan.data.modelDto.IssuesViewModel;
import ir.shecan.data.modelDto.ServiceItem;
import ir.shecan.data.modelDto.ServiceItemMapper;
import ir.shecan.data.modelDto.VerifyApiViewModel;
import ir.shecan.data.storage.AppStorage;
import ir.shecan.databinding.FragmentConfigListBinding;
import ir.shecan.ui.activity.MainActivityNew;
import ir.shecan.ui.adapter.ServiceAdapter;
import ir.shecan.ui.fragment.ToolbarFragment;
import ir.shecan.ui.fragment.bottomSheet.SubscriptionBottomSheet;

public class ConfigListFragment extends ToolbarFragment {

    private FragmentConfigListBinding binding;
    MainActivityNew activity;

    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentConfigListBinding.inflate(inflater, container, false);
        activity = (MainActivityNew) getActivity();

        startLoading();

        AppStorage appStorage = new AppStorage(getContext());
        reloadServices(appStorage);

        refreshPage();

        binding.swipeRefresh.setOnRefreshListener(this::refreshPage);

        return binding.getRoot();
    }


    private void refreshPage() {

        AppStorage storage = new AppStorage(getContext());
        VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);

        if (token == null || token.getApiKey() == null) {
            updateBanner();
            reloadServices(storage);
            stopLoading();
            return;
        }

        new AuthApi(getContext()).issues(
                token.getApiKey(),
                0,
                1000,
                new ApiCallback<IssuesViewModel>() {

                    @Override
                    public void onSuccess(IssuesViewModel res, boolean fromCache) {
                        storage.saveIssues(res);
                        reloadServices(storage);
                        updateBanner();
                        stopLoading();
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        showSilentWarning(
                                message != null ? message : "خطا در دریافت سرویس‌ها"
                        );
                        reloadServices(storage);
                        stopLoading();
                    }
                }
        );
    }

    private void reloadServices(AppStorage storage) {
        setupRecyclerView(buildServiceItems(storage), storage);
    }

    private List<ServiceItem> buildServiceItems(AppStorage appStorage) {

        List<ServiceItem> items = new ArrayList<>();

        // free همیشه هست
        items.add(
                ServiceItemMapper.map(
                        getContext(),
                        IssuesViewModel.IssuesDTO.createDefault()
                )
        );

        IssuesViewModel viewModel = appStorage.getIssue(IssuesViewModel.class);

        if (viewModel == null || viewModel.getIssues() == null || viewModel.getIssues().isEmpty()) {
            showSilentWarning("اطلاعات سرویس‌ها کامل بارگذاری نشد");
            return items;
        }

        for (IssuesViewModel.IssuesDTO issue : viewModel.getIssues()) {
            items.add(ServiceItemMapper.map(getContext(), issue));
        }

        return items;
    }

    private void showSilentWarning(String message) {
        if (!isAdded()) return;
        ToastManager.show(getContext(), message);
    }

    private void setupRecyclerView(List<ServiceItem> items, AppStorage appStorage) {
        Context context = getContext();
        if(context == null){
            return;
        }
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

        ServiceAdapter adapter = getServiceAdapter(items, appStorage, defaultSelected);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(context));
        binding.recyclerView.setAdapter(adapter);
    }

    @NonNull
    private ServiceAdapter getServiceAdapter(List<ServiceItem> items, AppStorage appStorage, int defaultSelected) {
        ServiceAdapter adapter = new ServiceAdapter(
                getContext(),
                items,
                new ServiceAdapter.OnMoreClickListener() {
                    @Override
                    public void onBackgroundClicked(ServiceItem item) {
                        logServiceEvent(TrackingUtils.EVENT_SERVICE_SELECTED, item);
                        appStorage.saveServiceStatus(item);

                        activity.configIsChange = true;

                        ((MainActivityNew) getActivity()).updateFragment(1);
                        ((MainActivityNew) getActivity()).currentTab = 1;
                    }

                    @Override
                    public void onOptionClicked(ServiceItem item) {
                        logServiceEvent(TrackingUtils.EVENT_SERVICE_DETAILS_CLICK, item);
                        SubscriptionBottomSheet bottomSheet = SubscriptionBottomSheet.newInstance(item);
                        bottomSheet.show(getParentFragmentManager(), "subscription_sheet");
                    }
                }
        );

        adapter.setSelectedPosition(defaultSelected);
        return adapter;
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
                        auth.bannerMatch(
                                DynamicBannerRequestFactory.fromStorage(requireContext()),
                                new ApiCallback<BannerViewModel>() {
                                    @Override
                                    public void onSuccess(BannerViewModel banner, boolean fromCache) {
                                        if (!isAdded()) return;
                                        List<BannerViewModel> list = banner != null
                                                ? Collections.singletonList(banner)
                                                : new ArrayList<>();
                                        activity.bannerUrl = list;
                                        handleBannerImage(list);
                                    }

                                    @Override
                                    public void onError(int statusCode, String message) {
                                        if (!isAdded()) return;
                                        loadLegacyBanners(auth, res);
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

    private void loadLegacyBanners(AuthApi auth, HomePage res) {
        if (res == null || res.getBannerService() == null) {
            handleBannerImage(new ArrayList<>());
            return;
        }

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
                        handleBannerImage(new ArrayList<>());
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }


    private void handleBannerImage(List<BannerViewModel> banners) {

        if (!isAdded() || binding == null) return;

        if (banners == null || banners.isEmpty()) {
            binding.bannerSlider.stop();
            binding.bannerSlider.setVisibility(GONE);
            return;
        }

        binding.bannerSlider.setVisibility(View.VISIBLE);
        binding.bannerSlider.setBanners(banners);
        binding.bannerSlider.start();

        binding.bannerSlider.getImageView().setOnClickListener(v -> {
            BannerViewModel banner = binding.bannerSlider.getCurrentBanner();
            if (banner != null && banner.getUrl() != null && !banner.getUrl().isEmpty()) {
                TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_BANNER_CLICK,
                        TrackingUtils.bundleOf(TrackingUtils.PARAM_BANNER_URL, banner.getUrl()));
                AppUtils.openUrl(banner.getUrl(), getActivity());
            }
        });
    }

    private void logServiceEvent(String eventName, ServiceItem item) {
        if (!isAdded() || item == null) return;
        android.os.Bundle params = new android.os.Bundle();
        TrackingUtils.put(params, TrackingUtils.PARAM_SERVICE_TYPE, item.getServiceType());
        TrackingUtils.put(params, TrackingUtils.PARAM_ORDER_CODE, item.getOrderCode());
        TrackingUtils.logEvent(requireContext(), eventName, params);
    }

    private void startLoading() {
        if (binding == null) return;
        binding.swipeRefresh.post(() ->
                binding.swipeRefresh.setRefreshing(true)
        );
    }

    private void stopLoading() {
        if (binding == null) return;
        binding.swipeRefresh.setRefreshing(false);
    }
}
