package ir.shecan.ui.activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static ir.shecan.core.util.AppUtils.adjustUIForFragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.List;
import java.util.Stack;

import ir.shecan.R;
import ir.shecan.Shecan;
import ir.shecan.core.constant.Constant;
import ir.shecan.core.util.AppUtils;
import ir.shecan.data.api.ApiCallback;
import ir.shecan.data.api.AuthApi;
import ir.shecan.data.modelDto.AccountViewModel;
import ir.shecan.data.modelDto.BannerViewModel;
import ir.shecan.data.modelDto.IssuesViewModel;
import ir.shecan.data.modelDto.VerifyApiViewModel;
import ir.shecan.data.storage.AppStorage;
import ir.shecan.databinding.ActivityMainNewBinding;
import ir.shecan.ui.activity.mainActivityUtils.LaunchHandler;
import ir.shecan.ui.activity.mainActivityUtils.TabItem;
import ir.shecan.ui.activity.mainActivityUtils.ThemeManager;
import ir.shecan.ui.activity.mainActivityUtils.VpnManager;
import ir.shecan.ui.fragment.ToolbarFragment;
import ir.shecan.ui.widget.CustomBottomBar;

public class MainActivityNew extends AppCompatActivity {

    // Launch Actions
    public static final int LAUNCH_ACTION_NONE = 0;
    public static final int LAUNCH_ACTION_ACTIVATE = 1;
    public static final int LAUNCH_ACTION_DEACTIVATE = 2;
    public static final int LAUNCH_ACTION_SERVICE_DONE = 3;

    // Fragments
    public static final int FRAGMENT_NONE = -1;
    public static final int FRAGMENT_HOME = 0;
    public static final int FRAGMENT_DNS_TEST = 1;
    public static final int FRAGMENT_SETTINGS = 2;
    public static final int FRAGMENT_ABOUT = 3;
    public static final int FRAGMENT_LOG = 6;
    public static final String LAUNCH_ACTION = "ir.shecan.ui.activity.MainActivityNew.LAUNCH_ACTION";
    public static final String LAUNCH_FRAGMENT = "ir.shecan.ui.activity.MainActivityNew.LAUNCH_FRAGMENT";
    public static final String LAUNCH_NEED_RECREATE = "ir.shecan.ui.activity.MainActivityNew.LAUNCH_NEED_RECREATE";
    public static final String LAST_TAB = "LAST_TAB_KEY";

    public int currentTab = 1;
    public boolean configIsChange = false;
    private final Stack<Integer> tabHistory = new Stack<>();

    private static MainActivityNew instance = null;

    private ToolbarFragment currentFragment;

    public ActivityMainNewBinding binding;

    private VpnManager vpnManager;
    private ThemeManager themeManager;
    public List<BannerViewModel> bannerUrl;

    public static MainActivityNew getInstance() {
        return instance;
    }

    public ToolbarFragment getCurrentFragment() {
        return currentFragment;
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(LAST_TAB, currentTab);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        themeManager = new ThemeManager(this);
        themeManager.applyTheme();
        Shecan.getInstance().updateLocale();

        super.onCreate(savedInstanceState);

        int selectedTab = 1;
        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt(LAST_TAB, 1);
        }

        instance = this;
        binding = ActivityMainNewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        vpnManager = new VpnManager(this);

        requestNotificationPermission();

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            FirebaseMessaging.getInstance().subscribeToTopic("afterPushPoleScenarioTopic");
        });

        updateLoginInformation();
        updateConfigsIfSignedIn();

        setupCustomBottomBar();

        currentTab = selectedTab;
        updateFragment(selectedTab);
        binding.customBar.select(selectedTab);

        LaunchHandler.handle(this, getIntent());
        onBackPressedHandler();
        vipClickHandler();

//        AppSignatureHelper helper = new AppSignatureHelper(this);
//        ArrayList<String> signatures = helper.getAppSignatures();
//
//        for (String signature : signatures) {
//            Log.d("APP_HASH", signature);
//            Toast.makeText(this, signature, Toast.LENGTH_LONG).show();
//        }
    }

    private void checkUserIsLogin() {
        AppStorage storage = new AppStorage(getApplicationContext());
        VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);
        if (token == null) {
            startActivity(new Intent(this, AuthorizeActivity.class)
                    .putExtra("isShowBackButton", false));

//            finish();
        }
    }

    private void vipClickHandler() {
        binding.toolbar.vip.setVisibility(Constant.IsSiteMode? VISIBLE : GONE);
        binding.toolbar.vip.setOnClickListener(view -> AppUtils.openUrl(Constant.PlanUrl, this));
    }

    private void onBackPressedHandler() {

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

                if (!tabHistory.isEmpty()) {
                    int previousTab = tabHistory.pop();
                    currentTab = previousTab;

                    binding.customBar.select(previousTab);
                    updateFragment(previousTab);
                    return;
                }

                finish();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }


    public void setupCustomBottomBar() {

        String settingTitle = getString(R.string.setting);
        AppStorage storage = new AppStorage(getApplicationContext());
        VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);

        if (token == null || token.getApiKey() == null)
            settingTitle = getString(R.string.login);

        CustomBottomBar bar = binding.customBar;
        bar.removeItems();

        bar.addItem(getString(R.string.connections), R.drawable.ic_connection_inactive, R.drawable.ic_config_active);
        bar.addItem(getString(R.string.connect), R.drawable.ic_vpn_inactive, R.drawable.ic_vpn_active);
        bar.addItem(settingTitle, R.drawable.ic_setting_inactive, R.drawable.ic_profile_active);

        // ⭐⭐ ذخیره تاریخچه تب‌ها ⭐⭐
        bar.setOnItemSelected(index -> {

            if (currentTab != index) {
                tabHistory.push(currentTab);
            }

            currentTab = index;
            updateFragment(index);

            switch (index) {
                case 0:
                    adjustUIForFragment(this, R.color.mainBack, R.color.mainBack);
                    binding.toolbar.appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.mainBack));
                    break;

                case 1:
                    adjustUIForFragment(this, R.color.lightBack, R.color.mainBack);
                    binding.toolbar.appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.lightBack));
                    break;

                case 2:
                    adjustUIForFragment(this, R.color.profileBackground, R.color.mainBack);
                    binding.toolbar.appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.profileBackground));
                    break;
            }
        });
    }


    public void switchFragment(Class fragmentClass, boolean addToBackStack) {

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ToolbarFragment fragment =
                (ToolbarFragment) fm.findFragmentByTag(fragmentClass.getName());

        if (fragment == null) {
            try {
                fragment = (ToolbarFragment) fragmentClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        ft.replace(R.id.id_content, fragment, fragmentClass.getName());

        if (addToBackStack) {
            boolean exists = false;
            for (int i = 0; i < fm.getBackStackEntryCount(); i++) {
                if (fm.getBackStackEntryAt(i).getName().equals(fragmentClass.getName())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) ft.addToBackStack(fragmentClass.getName());
        }

        ft.commitAllowingStateLoss();

        adjustUIForFragment(this, R.color.lightBack, R.color.mainBack);
        currentFragment = fragment;
    }


    public void updateFragment(int index) {

        TabItem tab = TabItem.fromIndex(index);

        if (tab.getTitle() != null) {
            binding.toolbar.toolbarLogo.setVisibility(GONE);
            binding.toolbar.toolbarTitle.setVisibility(VISIBLE);
            binding.toolbar.toolbarTitle.setText(tab.getTitle());
        } else {
            binding.toolbar.toolbarLogo.setVisibility(VISIBLE);
            binding.toolbar.toolbarTitle.setVisibility(GONE);
        }

        switch (index) {
            case 0:
                adjustUIForFragment(this, R.color.mainBack, R.color.mainBack);
                binding.toolbar.appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.mainBack));
                break;

            case 1:
                adjustUIForFragment(this, R.color.lightBack, R.color.mainBack);
                binding.toolbar.appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.lightBack));
                break;

            case 2:
                adjustUIForFragment(this, R.color.profileBackground, R.color.mainBack);
                binding.toolbar.appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.profileBackground));
                break;
        }

        switchFragment(tab.getFragmentClass(), false);
    }


    @Override
    public void onResume() {
        super.onResume();
        if (themeManager != null && themeManager.handleOnResume()) recreate();
        checkUserIsLogin();
        updateLoginInformation();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        LaunchHandler.handle(this, intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
        currentFragment = null;
        binding = null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Shecan.getInstance().updateLocale();
    }

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(ir.shecan.core.util.server.LocaleHelper.onAttach(base));
    }

    public void updateLoginInformation() {
        AppStorage storage = new AppStorage(getApplicationContext());
        VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);
        if (token != null && token.getApiKey() != null) {
            AuthApi auth = new AuthApi(getApplicationContext());
            auth.me(
                    token.getApiKey(),
                    new ApiCallback<AccountViewModel>() {
                        @Override
                        public void onSuccess(AccountViewModel res, boolean fromCache) {
                            if (res != null && res.getUser() != null) {
                                token.setFirstname(res.getUser().getFirstname());
                                token.setMail(res.getUser().getMail());
                                token.setLastname(res.getUser().getLastname());
                                storage.saveToken(token);
                            }
                        }

                        @Override
                        public void onError(int statusCode, String message) {

                        }
                    }
            );
        }
        setupCustomBottomBar();
        updateConfigsIfSignedIn();
    }

    public void updateConfigsIfSignedIn() {
        AppStorage storage = new AppStorage(getApplicationContext());
        VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);
        if (token != null && token.getApiKey() != null) {
            AuthApi auth = new AuthApi(getApplicationContext());
            auth.issues(
                    token.getApiKey(),
                    0, 1000,
                    new ApiCallback<IssuesViewModel>() {
                        @Override
                        public void onSuccess(IssuesViewModel res, boolean fromCache) {
                            storage.saveIssues(res);
                        }

                        @Override
                        public void onError(int statusCode, String message) {

                        }
                    }
            );
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    public void activateService() {
        if (vpnManager != null) vpnManager.startVpnActivation();
    }

    public void applyThemeForRecreate() {
        if (themeManager != null) themeManager.applyTheme();
    }
}
