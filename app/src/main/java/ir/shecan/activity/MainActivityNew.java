package ir.shecan.activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.messaging.FirebaseMessaging;

import ir.shecan.R;
import ir.shecan.Shecan;
import ir.shecan.activity.mainActivityUtils.FragmentNavigator;
import ir.shecan.activity.mainActivityUtils.LaunchHandler;
import ir.shecan.activity.mainActivityUtils.TabItem;
import ir.shecan.activity.mainActivityUtils.ThemeManager;
import ir.shecan.activity.mainActivityUtils.VpnManager;
import ir.shecan.api.ApiCallback;
import ir.shecan.api.AuthApi;
import ir.shecan.databinding.ActivityMainNewBinding;
import ir.shecan.fragment.ToolbarFragment;
import ir.shecan.fragment.refactor.HomeFragment;
import ir.shecan.modelDto.AccountViewModel;
import ir.shecan.modelDto.IssuesViewModel;
import ir.shecan.modelDto.VerifyApiViewModel;
import ir.shecan.storage.AppStorage;
import ir.shecan.widget.CustomBottomBar;

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
    public static final String LAUNCH_ACTION = "ir.shecan.activity.MainActivityNew.LAUNCH_ACTION";
    public static final String LAUNCH_FRAGMENT = "ir.shecan.activity.MainActivityNew.LAUNCH_FRAGMENT";
    public static final String LAUNCH_NEED_RECREATE = "ir.shecan.activity.MainActivityNew.LAUNCH_NEED_RECREATE";
    public static final String LAST_TAB = "LAST_TAB_KEY";

    private int currentTab = 1;

    private static MainActivityNew instance = null;

    private ToolbarFragment currentFragment;

    public ActivityMainNewBinding binding;

    private VpnManager vpnManager;
    private ThemeManager themeManager;

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

        binding.toolbar.appBarLayout.setPadding(0, getStatusBarHeight(), 0, 0);

        requestNotificationPermission();

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
//            String token = task.getResult();
            FirebaseMessaging.getInstance()
                    .subscribeToTopic("afterPushPoleScenarioTopic");
        });

        updateLoginInformation();

        updateConfigsIfSignedIn();

        setupCustomBottomBar();

        currentTab = selectedTab;
        updateFragment(selectedTab);

        LaunchHandler.handle(this, getIntent());

        onBackPressedHandler();
    }

    private void onBackPressedHandler() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!(currentFragment instanceof HomeFragment)) {
                    switchFragment(HomeFragment.class, true, false);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    public void updateLoginInformation() {
        AppStorage storage = new AppStorage(getApplicationContext());
        VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);
        if (token != null && token.getApiKey() != null){
            AuthApi auth = new AuthApi(getApplicationContext());
            auth.me(
                    token.getApiKey(),
                    new ApiCallback<AccountViewModel>() {
                        @Override
                        public void onSuccess(AccountViewModel res, boolean fromCache) {
                            if (res != null && res.getUser() != null){
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
        updateConfigsIfSignedIn();
    }

    public void updateConfigsIfSignedIn() {
        AppStorage storage = new AppStorage(getApplicationContext());
        VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);
        if (token != null && token.getApiKey() != null){
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

    private void setupCustomBottomBar() {
        CustomBottomBar bar = binding.customBar;
        bar.addItem(getString(R.string.connections), R.drawable.ic_connection_inactive, R.drawable.ic_config_active);
        bar.addItem(getString(R.string.connect), R.drawable.ic_vpn_inactive, R.drawable.ic_vpn_active);
        bar.addItem(getString(R.string.setting), R.drawable.ic_setting_inactive, R.drawable.ic_profile_active);

        bar.setOnItemSelected(index -> {
            currentTab = index;
            updateFragment(index);
        });

        updateFragment(1);
        binding.customBar.select(1);
    }

    public void switchFragment(Class fragmentClass, boolean isHome, boolean isAdd) {
        FragmentManager fm = getSupportFragmentManager();

        try {
            ToolbarFragment fragment = FragmentNavigator.switchFragment(
                    fm,
                    R.id.id_content,
                    fragmentClass,
                    isAdd
            );

            if (fragment == null) return;

            if (isAdd && currentFragment != null) {
                fm.beginTransaction().hide(currentFragment).commitAllowingStateLoss();
            }

            currentFragment = fragment;

        } catch (ClassCastException e) {
            e.printStackTrace();
            return;
        }

        adjustUIForFragment(isHome);
    }

    private void adjustUIForFragment(boolean isHome) {
        Window window = getWindow();
        CoordinatorLayout coordinatorLayout = binding.idContent;
        CoordinatorLayout.LayoutParams params =
                (CoordinatorLayout.LayoutParams) coordinatorLayout.getLayoutParams();
        AppBarLayout appBarLayout = binding.toolbar.appBarLayout;

        if (isHome) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

            if (!ViewConfiguration.get(getApplicationContext()).hasPermanentMenuKey()) {
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            }

            appBarLayout.setPadding(0, getStatusBarHeight(), 0, 0);
            params.setBehavior(null);

        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.setStatusBarColor(fetchPrimaryDarkColor());
            appBarLayout.setPadding(0, 0, 0, 0);
            params.setBehavior(new AppBarLayout.ScrollingViewBehavior());
            window.getDecorView().setFitsSystemWindows(true);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }

        coordinatorLayout.requestLayout();
    }

    public void activateService() {
        if (vpnManager != null) vpnManager.startVpnActivation();
    }

    public void applyThemeForRecreate() {
        if (themeManager != null) themeManager.applyTheme();
    }

    private int fetchPrimaryDarkColor() {
        return getTheme().obtainStyledAttributes(new int[]{R.color.colorPrimaryDark}).getColor(0, 0);
    }

    public int getStatusBarHeight() {
        return (int) Math.ceil(25 * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (themeManager != null && themeManager.handleOnResume()) {
            recreate();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        LaunchHandler.handle(this, intent);
    }

    @Override
    public void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (vpnManager != null) vpnManager.handleActivityResult(result);
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
        super.attachBaseContext(ir.shecan.util.server.LocaleHelper.onAttach(base));
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

        switchFragment(tab.getFragmentClass(), true, false);
    }

}
