package ir.shecan.fragment.refactor;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ir.shecan.R;
import ir.shecan.Shecan;
import ir.shecan.activity.MainActivityNew;
import ir.shecan.api.ApiCallback;
import ir.shecan.api.AuthApi;
import ir.shecan.databinding.FragmentMainNewBinding;
import ir.shecan.dialog.ContactSupportDialog;
import ir.shecan.dialog.RenewalDialog;
import ir.shecan.dialog.UpdateDialog;
import ir.shecan.fragment.ToolbarFragment;
import ir.shecan.modelDto.BannerViewModel;
import ir.shecan.modelDto.IssuesViewModel;
import ir.shecan.modelDto.ServiceItem;
import ir.shecan.service.BaseApiResponseListener;
import ir.shecan.service.ConnectionStatusApiListener;
import ir.shecan.service.CoreApiResponseListener;
import ir.shecan.service.ShecanVpnService;
import ir.shecan.storage.AppStorage;
import ir.shecan.util.AppUtils;

public class HomeFragment extends ToolbarFragment implements CoreApiResponseListener, ConnectionStatusApiListener {

    private FragmentMainNewBinding binding;
    private boolean isUpdateVersionCheck = false;
    private ScheduledExecutorService scheduler;
    MainActivityNew activity;
    private BannerViewModel bannerUrl;
    private static final String TAG = "HomeFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMainNewBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        activity = (MainActivityNew) getActivity();

        setupDonatePadding();

        if (bannerUrl == null) updateBanner();

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
                String updaterUrl = String.format("https://ddns.shecan.ir/update?password=%s", finalServiceItem.getUpdateLink());
                Shecan.setUpdaterLink(updaterUrl);
                ShecanVpnService.callCoreAPI(requireContext(), HomeFragment.this);
            } else {
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
    }

    private void setupDonatePadding() {
        final LinearLayout donate = binding.linearLayoutDonate;
        if (!ViewConfiguration.get(requireContext()).hasPermanentMenuKey()) {
            ViewCompat.setOnApplyWindowInsetsListener(donate, (v, insets) -> {
                Insets navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
                v.setPadding(0, 0, 0, navBarInsets.bottom);
                return insets;
            });
        }
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

    }

    @Override
    public void onInvalid() {
        Shecan app = (Shecan) requireContext().getApplicationContext();
        app.getVpnState().setValue(0);
        if (isAdded()) new RenewalDialog(requireActivity()).show();
    }

    @Override
    public void onOutOfRange() {
        if (isAdded()) new ContactSupportDialog(requireActivity()).show();
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
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        binding = null;
    }

//    public void updateBanner() {
//
//        String url = "https://n8n.coolify.shcn.ir/webhook/banner?type=1";
//
//        StringRequest request = new StringRequest(
//                Request.Method.GET,
//                url,
//                response -> {
//                    try {
//                        JSONArray arr = new JSONArray(response);
//
//                        // کوچکترین order
//                        JSONObject best = arr.getJSONObject(0);
//                        for (int i = 1; i < arr.length(); i++) {
//                            if (arr.getJSONObject(i).getInt("order") < best.getInt("order")) {
//                                best = arr.getJSONObject(i);
//                            }
//                        }
//
//                        int type = best.getInt("type");
//
//                        if (type == 1) {
//                            String img = best.getString("imageURL");
//                            Glide.with(getContext()).load(img).into(binding.banner);
//
//                        } else if (type == 2) {
//                            String base = best.getString("imageBase64");
//                            byte[] decoded = Base64.decode(base, Base64.DEFAULT);
//                            Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
//                            binding.banner.setImageBitmap(bmp);
//                        }
//
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                },
//                error -> {
//                    Log.e("ERROR", error.toString());
//                }
//        );
//
//        Volley.newRequestQueue(getContext()).add(request);
//    }
//


    public void updateBanner() {
        AuthApi auth = new AuthApi(getContext());

        auth.banner(
                new ApiCallback<BannerViewModel>() {
                    @Override
                    public void onSuccess(BannerViewModel res, boolean fromCache) {
                        bannerUrl = res;

                        // binding.banner is imageview

                        if (res.getType() == 1) {
                            // is url

                            if (!isAdded() || binding == null) return;

                            String url = res.getImageURL();

                            Glide.with(requireContext())
                                    .load(url)
                                    .into(binding.banner);


                        } else if (res.getType() == 2) {
                            // is base64
                            String base64 = res.getImageBase64();
                            byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            binding.banner.setImageBitmap(decodedByte);
                        }
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }
}
