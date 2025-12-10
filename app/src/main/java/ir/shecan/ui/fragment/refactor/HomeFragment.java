package ir.shecan.ui.fragment.refactor;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ir.shecan.R;
import ir.shecan.Shecan;
import ir.shecan.data.modelDto.AccountViewModel;
import ir.shecan.data.modelDto.HomePage;
import ir.shecan.ui.activity.MainActivityNew;
import ir.shecan.data.api.ApiCallback;
import ir.shecan.data.api.AuthApi;
import ir.shecan.databinding.FragmentHomeBinding;
import ir.shecan.ui.dialog.ContactSupportDialog;
import ir.shecan.ui.dialog.RenewalDialog;
import ir.shecan.ui.dialog.UpdateDialog;
import ir.shecan.ui.fragment.ToolbarFragment;
import ir.shecan.data.modelDto.BannerViewModel;
import ir.shecan.data.modelDto.IssuesViewModel;
import ir.shecan.data.modelDto.ServiceItem;
import ir.shecan.core.service.BaseApiResponseListener;
import ir.shecan.core.service.ConnectionStatusApiListener;
import ir.shecan.core.service.CoreApiResponseListener;
import ir.shecan.core.service.ShecanVpnService;
import ir.shecan.data.storage.AppStorage;
import ir.shecan.core.util.AppUtils;

public class HomeFragment extends ToolbarFragment implements CoreApiResponseListener, ConnectionStatusApiListener {

    private FragmentHomeBinding binding;
    private boolean isUpdateVersionCheck = false;
    private ScheduledExecutorService scheduler;
    MainActivityNew activity;
    private static final String TAG = "HomeFragment";

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

            binding.bannerSlider.setVisibility(hide ? View.GONE : View.VISIBLE);
            binding.constraintLayout.setVisibility(hide ? View.GONE : View.VISIBLE);
        });

        setupDonatePadding();

        if (activity.bannerUrl == null) updateBanner();
        else handleBannerImage(activity.bannerUrl);

        AppStorage appStorage = new AppStorage(getContext());
        ServiceItem serviceItem = appStorage.getServiceStatus(ServiceItem.class);

        ServiceItem finalServiceItem = serviceItem;
        binding.vpnButton.setOnClickListener(view -> {

            if (ShecanVpnService.isActivated()) {
                Shecan app = (Shecan) requireContext().getApplicationContext();
                app.getVpnState().setValue(0);
                ShecanVpnService.cancelConnectionStatusAPI(requireContext());
                ShecanVpnService.cancelCoreAPI(requireContext());
                Shecan.deactivateService(requireContext());
                return;
            }

            Shecan app = (Shecan) requireContext().getApplicationContext();
            app.getVpnState().setValue(1);

            if (isUpdateLinkMode(finalServiceItem)) {
                Shecan.setProMode();
                String updaterUrl = String.format("https://ddns.shecan.ir/update?password=%s", finalServiceItem.getUpdateLink());
                Shecan.setUpdaterLink(updaterUrl);
                ShecanVpnService.callCoreAPI(requireContext(), HomeFragment.this);
            } else {
                Shecan.setFreeMode();
                startActivity(new Intent(requireActivity(), MainActivityNew.class)
                        .putExtra(MainActivityNew.LAUNCH_ACTION, MainActivityNew.LAUNCH_ACTION_ACTIVATE));
            }
        });

        Shecan app = (Shecan) requireContext().getApplicationContext();
        app.getVpnState().observe(getViewLifecycleOwner(), state -> {
            if (state != null && binding != null) {
                switch (state) {
                    case 0:
                        binding.vpnButton.showLoading(false);
                        break;
                    case 1:
                        binding.vpnButton.showLoading(true);
                        break;
                    case 2:
                        binding.vpnButton.setConnected(true);
                        break;
                }
            }
        });


        binding.chooseConfig.setOnClickListener(v -> {
            activity.updateFragment(0);
            activity.binding.customBar.select(0);
        });


        if (serviceItem == null) {
            serviceItem = new ServiceItem("", ContextCompat.getString(getContext(), R.string.free), "", "", 0, 0, IssuesViewModel.IssuesDTO.createDefault());
        }
        try {
            binding.servicePanel.setStatus(serviceItem);
        } catch (ParseException ignored) {

        }

        binding.servicePanel.setOnClickListener(view -> {
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
        ((MainActivityNew) getActivity()).binding.customBar.select(1);
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
//        if (!isAdded()) return;
//        final String imageUrl = Shecan.ShecanInfo.getBannerImageUrl();
//        if (!imageUrl.isEmpty()) {
//            ImageUtils.INSTANCE.loadImage(requireContext(), imageUrl, binding.bannerImageView);
//        }
//        binding.bannerImageView.setOnClickListener(v -> {
//            String url = Shecan.ShecanInfo.getBannerLink();
//            if (!url.isEmpty() && isAdded()) {
//                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
//            }
//        });
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
        startActivity(new Intent(requireActivity(), MainActivityNew.class)
                .putExtra(MainActivityNew.LAUNCH_ACTION, MainActivityNew.LAUNCH_ACTION_ACTIVATE));
    }

    @Override
    public void onError(String errorMessage) {
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
            Shecan.setStaticIPMode();
            startActivity(new Intent(requireActivity(), MainActivityNew.class)
                    .putExtra(MainActivityNew.LAUNCH_ACTION, MainActivityNew.LAUNCH_ACTION_ACTIVATE));
        }
    }

    @Override
    public void onConnected() {
//        if (!isAdded()) return;
    }

    @Override
    public void onRetry() {
        if (ShecanVpnService.isDynamicIPMode()) {
            if (scheduler != null && !scheduler.isShutdown()) scheduler.shutdownNow();
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.schedule(() -> {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isAdded() && !isRemoving()) {
                        ShecanVpnService.callConnectionStatusAPI(requireContext(), HomeFragment.this, null);
                    }
                });
            }, 20, TimeUnit.SECONDS);
        } else {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isAdded() || isRemoving()) return;
                Shecan.deactivateService(requireContext());
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) binding.bannerSlider.stop();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        binding = null;
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
