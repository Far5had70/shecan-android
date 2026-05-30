package ir.shecan.ui.fragment.refactor;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ir.shecan.R;
import ir.shecan.Shecan;
import ir.shecan.core.monitoring.MonitoringManager;
import ir.shecan.core.service.BaseApiResponseListener;
import ir.shecan.core.service.ConnectionStatusApiListener;
import ir.shecan.core.service.CoreApiResponseListener;
import ir.shecan.core.service.ShecanVpnService;
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
import ir.shecan.data.storage.AppStorage;
import ir.shecan.databinding.FragmentHomeBinding;
import ir.shecan.ui.activity.MainActivityNew;
import ir.shecan.ui.dialog.ContactSupportDialog;
import ir.shecan.ui.dialog.RenewalDialog;
import ir.shecan.ui.dialog.UpdateDialog;
import ir.shecan.ui.fragment.ToolbarFragment;
import ir.shecan.ui.widget.rateHelper.RatingDialog;
import ir.shecan.ui.widget.rateHelper.RatingManager;

public class HomeFragment extends ToolbarFragment implements CoreApiResponseListener, ConnectionStatusApiListener {

    private FragmentHomeBinding binding;
    private boolean isUpdateVersionCheck = false;
    private boolean hasBanner = false;
    private ScheduledExecutorService scheduler;
    private MonitoringManager monitoringManager;
    private long dynamicIpCheckDeadlineMs = 0L;
    MainActivityNew activity;

    private static final String TAG = "HomeFragment";
    private static final long DYNAMIC_IP_CHECK_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(70);
    private static final long DYNAMIC_IP_CHECK_RETRY_DELAY_SECONDS = 10;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        activity = (MainActivityNew) getActivity();


        root.addOnLayoutChangeListener((v, left, top, right, bottom,
                                        oldLeft, oldTop, oldRight, oldBottom) -> {

            int width = v.getWidth();
            int height = v.getHeight();

            boolean hide = height < width * 1.5f;

            binding.bannerSlider.setVisibility(hide || !hasBanner ? GONE : VISIBLE);
            binding.constraintLayout.setVisibility(hide ? GONE : VISIBLE);
        });

        setupDonatePadding();

        if (activity.bannerUrl == null) updateBanner();
        else handleBannerImage(activity.bannerUrl);

        AppStorage appStorage = new AppStorage(getContext());
        monitoringManager = new MonitoringManager(requireContext());
        ServiceItem serviceItem = appStorage.getServiceStatus(ServiceItem.class);

//        ServiceItem finalServiceItem = serviceItem;

        binding.vpnButton.setOnClickListener(v -> {
            Shecan app = (Shecan) requireContext().getApplicationContext();

            if (ShecanVpnService.isActivated()) {
                TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_VPN_DISCONNECT_CLICK,
                        TrackingUtils.bundleOf(TrackingUtils.PARAM_SOURCE, "home_button"));
                cancelDynamicIpStatusCheck();
                app.getVpnState().setValue(0);
                ShecanVpnService.cancelConnectionStatusAPI(requireContext());
                ShecanVpnService.cancelCoreAPI(requireContext());
                Shecan.deactivateService(requireContext());
            } else if (binding.vpnButton.isLoading()) {
                TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_VPN_DISCONNECT_CLICK,
                        TrackingUtils.bundleOf(TrackingUtils.PARAM_SOURCE, "home_button_loading"));
                cancelDynamicIpStatusCheck();
                app.getVpnState().setValue(0);
                ShecanVpnService.cancelConnectionStatusAPI(requireContext());
                ShecanVpnService.cancelCoreAPI(requireContext());
                Shecan.deactivateService(requireContext());
            } else {
                TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_VPN_CONNECT_CLICK,
                        TrackingUtils.bundleOf(TrackingUtils.PARAM_SOURCE, "home_button"));
                app.connectVpn(requireContext(), HomeFragment.this);
            }
        });

        Shecan app = (Shecan) requireContext().getApplicationContext();
        app.getVpnState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || binding == null) return;

            switch (state) {
                case 0:
                    binding.vpnButton.showLoading(false);
                    binding.statusTv.setVisibility(GONE);
                    if (app.getVpnStatus().getValue() != null && !app.getVpnStatus().getValue().isEmpty()) {
                        ToastManager.show(getContext(), app.getVpnStatus().getValue());
                        app.getVpnStatus().setValue("");
                    }
                    break;
                case 1:
                    binding.vpnButton.showLoading(true);
                    binding.statusTv.setVisibility(GONE);
                    break;
                case 2:
                    binding.vpnButton.setConnected(true);
                    binding.statusTv.setVisibility(VISIBLE);
                    break;
            }
        });

        app.getProActivatedEvent().observe(getViewLifecycleOwner(), activated -> {
            if (activated == null || !activated) return;

            startActivity(new Intent(requireActivity(), MainActivityNew.class)
                    .putExtra(MainActivityNew.LAUNCH_ACTION,
                            MainActivityNew.LAUNCH_ACTION_ACTIVATE));

            // consume event
            app.getProActivatedEvent().setValue(false);
        });


        binding.chooseConfig.setOnClickListener(v -> {
            TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_SERVICE_DETAILS_CLICK,
                    TrackingUtils.bundleOf(TrackingUtils.PARAM_SOURCE, "home_choose_config"));
            activity.updateFragment(0);
            activity.binding.customBar.select(0);
        });


        if (serviceItem == null) {
            serviceItem = new ServiceItem("", ContextCompat.getString(getContext(), R.string.free), "", "", "", 0, 0, IssuesViewModel.IssuesDTO.createDefault());
        }
        try {
            binding.servicePanel.setStatus(serviceItem);
        } catch (ParseException ignored) {

        }

        binding.servicePanel.setOnClickListener(view -> {
            TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_SERVICE_DETAILS_CLICK,
                    TrackingUtils.bundleOf(TrackingUtils.PARAM_SOURCE, "home_service_panel"));
            activity.updateFragment(0);
            activity.binding.customBar.select(0);
        });

        return root;
    }

    private boolean isUpdateLinkMode(ServiceItem serviceItem) {
        if (serviceItem == null) {
            return false;
        }
        if (serviceItem.getUpdateLink() == null) {
            return false;
        }
        return !serviceItem.getUpdateLink().isEmpty();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void checkStatus() {
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchData();
        if (monitoringManager != null) {
            monitoringManager.start();
        }
        ((MainActivityNew) getActivity()).binding.customBar.select(1);

        if (activity.configIsChange) {
            activity.configIsChange = false;

            Shecan app = (Shecan) requireContext().getApplicationContext();

            if (ShecanVpnService.isActivated()) {
                app.pendingReconnect = true;
                Shecan.deactivateService(requireContext());
                app.waitForDeactivateThenReconnect(requireContext());
            } else {
                app.connectVpn(requireContext(), HomeFragment.this);
            }
        }

    }

    @Override
    public void onPause() {
        if (monitoringManager != null) {
            monitoringManager.stop();
        }
        super.onPause();
    }

    private void setupDonatePadding() {
//        final LinearLayout donate = binding.linearLayoutDonate;
//        if (!ViewConfiguration.get(requireContext()).hasPermanentMenuKey()) {
//            ViewCompat.setOnApplyWindowInsetsListener(donate, (v, insets) -> {
//                Insets navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
//                v.setPadding(0, 0, 0, navBarInsets.bottom);
//                return insets;
//            });
//        }
    }

    private void fetchData() {
        if (!isAdded()) return;

        Shecan.ShecanInfo.fetchData(requireContext(), new BaseApiResponseListener() {
            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                loadBanner();
            }

            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                if (!isUpdateVersionCheck) {
                    checkIsUpdateAvailable();
                    isUpdateVersionCheck = true;
                }
                loadBanner();
            }
        });
    }

    private void loadBanner() {
        if (!isAdded()) return;
        updateBanner();
    }

    private void checkIsUpdateAvailable() {
        if (!isAdded()) return;
        boolean isForce = false;
        String currentVersion = AppUtils.getVersionName(requireActivity());
        String minVersion = Shecan.ShecanInfo.getMinVersion();
        String latestVersion = Shecan.ShecanInfo.getCurrentVersion();

        if (AppUtils.compareVersionNames(minVersion, currentVersion) == 1) { // min > current
            isForce = true;
        }
        if (AppUtils.compareVersionNames(latestVersion, currentVersion) == 1) {
            new UpdateDialog(requireActivity()).show(isForce);
        }
    }

    @Override
    public void onSuccess(String response) {
        if (!isAdded()) return;
        if (ShecanVpnService.isDynamicIPMode()) {
            waitForDynamicIpActivation();
        } else {
            startVpnAfterConnectionStatusVerified("static");
        }
    }

    @Override
    public void onError(String errorMessage) {
        if (isAdded()) {
            TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_VPN_ERROR,
                    TrackingUtils.bundleOf(TrackingUtils.PARAM_ERROR, errorMessage != null ? errorMessage : "unknown"));
        }
        Shecan app = (Shecan) requireContext().getApplicationContext();
        app.getVpnState().setValue(0);
    }

    @Override
    public void onInvalid() {
        Shecan app = (Shecan) requireContext().getApplicationContext();
        app.getVpnState().setValue(0);
        if (isAdded()) new RenewalDialog(requireActivity()).show();
    }

    @Override
    public void onOutOfRange() {
        if (isAdded()) {
            new ContactSupportDialog(requireActivity()).show();
            Shecan app = (Shecan) requireContext().getApplicationContext();
            app.getVpnState().setValue(0);
        }
    }

    @Override
    public void onInTheRange() {
        if (isAdded()) {
            TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_VPN_CONNECTED,
                    TrackingUtils.bundleOf(TrackingUtils.PARAM_METHOD, "static"));
            Shecan.setStaticIPMode();
            startActivity(new Intent(requireActivity(), MainActivityNew.class)
                    .putExtra(MainActivityNew.LAUNCH_ACTION, MainActivityNew.LAUNCH_ACTION_ACTIVATE));
        }
    }

    @Override
    public void onConnected() {
        if (!isAdded()) return;
        cancelDynamicIpStatusCheck();
        startVpnAfterConnectionStatusVerified(ShecanVpnService.isDynamicIPMode() ? "dynamic" : "static");
    }

    @Override
    public void onRetry() {
        if (!isAdded() || isRemoving()) return;

        if (ShecanVpnService.isDynamicIPMode()) {
            if (!isDynamicIpCheckInProgress()) {
                failDynamicIpStatusCheck();
                return;
            }
            cancelScheduler();
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.schedule(() -> {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isAdded() && !isRemoving() && isDynamicIpCheckInProgress()) {
                        ShecanVpnService.callConnectionStatusAPI(requireContext(), HomeFragment.this, null);
                    } else if (isAdded() && !isRemoving()) {
                        failDynamicIpStatusCheck();
                    }
                });
            }, DYNAMIC_IP_CHECK_RETRY_DELAY_SECONDS, TimeUnit.SECONDS);
        } else {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isAdded() || isRemoving()) return;
                Shecan.deactivateService(requireContext());
            });
        }
    }

    private void waitForDynamicIpActivation() {
        dynamicIpCheckDeadlineMs = System.currentTimeMillis() + DYNAMIC_IP_CHECK_TIMEOUT_MS;
        ShecanVpnService.callConnectionStatusAPI(requireContext(), this, null);
    }

    private boolean isDynamicIpCheckInProgress() {
        return dynamicIpCheckDeadlineMs > 0 && System.currentTimeMillis() <= dynamicIpCheckDeadlineMs;
    }

    private void failDynamicIpStatusCheck() {
        cancelDynamicIpStatusCheck();
        Shecan app = (Shecan) requireContext().getApplicationContext();
        app.getVpnStatus().setValue(getString(R.string.dynamic_ip_connection_not_ready));
        app.getVpnState().setValue(0);
    }

    private void startVpnAfterConnectionStatusVerified(String method) {
        TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_VPN_CONNECTED,
                TrackingUtils.bundleOf(TrackingUtils.PARAM_METHOD, method));
        startActivity(new Intent(requireActivity(), MainActivityNew.class)
                .putExtra(MainActivityNew.LAUNCH_ACTION, MainActivityNew.LAUNCH_ACTION_ACTIVATE));
    }

    private void cancelDynamicIpStatusCheck() {
        dynamicIpCheckDeadlineMs = 0L;
        cancelScheduler();
    }

    private void cancelScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        scheduler = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (binding != null) binding.bannerSlider.stop();
        cancelDynamicIpStatusCheck();
        if (monitoringManager != null) {
            monitoringManager.stop();
            monitoringManager = null;
        }
        binding = null;
    }

    public void updateBanner() {
        AuthApi auth = new AuthApi(getContext());

        auth.homePageApi(
                new ApiCallback<HomePage>() {
                    @Override
                    public void onSuccess(HomePage res, boolean fromCache) {
                        showBanner(auth, res, fromCache);
                        checkRatingRule(res.getAppStoreRate().getAndroid());
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        Log.e(TAG, "onError: ");
                    }
                }
        );
    }

    private void showBanner(AuthApi auth, HomePage res, boolean fromCache) {
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
                        loadLegacyBanners(auth, res);
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
                        ToastManager.show(getContext(), message);
                    }
                }
        );
    }

    private void checkRatingRule(String url) {
        if (!isAdded()) return;

        RatingManager ratingManager = new RatingManager(requireContext());
        ratingManager.initFirstOpenIfNeeded();

        if (ratingManager.shouldShowRatingDialog()) {
            RatingDialog.showIfNotVisible(requireContext(), url);
        }
    }

    private void handleBannerImage(List<BannerViewModel> banners) {

        if (!isAdded() || binding == null) return;

        if (banners == null || banners.isEmpty()) {
            hasBanner = false;
            binding.bannerSlider.stop();
            binding.bannerSlider.setVisibility(GONE);
            return;
        }

        hasBanner = true;
        binding.bannerSlider.setVisibility(VISIBLE);
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

}
