package ir.shecan.activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.messaging.FirebaseMessaging;

import ir.shecan.R;
import ir.shecan.Shecan;
import ir.shecan.databinding.ActivityMainNewBinding;
import ir.shecan.fragment.AboutFragment;
import ir.shecan.fragment.DNSTestFragment;
import ir.shecan.fragment.refactor.ConfigListFragment;
import ir.shecan.fragment.refactor.HomeFragment;
import ir.shecan.fragment.LogFragment;
import ir.shecan.fragment.SettingsFragment;
import ir.shecan.fragment.ToolbarFragment;
import ir.shecan.fragment.refactor.ProfileFragment;
import ir.shecan.modelDto.AppConfig;
import ir.shecan.service.ShecanVpnService;
import ir.shecan.storage.AppStorage;
import ir.shecan.util.Logger;
import ir.shecan.util.server.DNSServerHelper;
import ir.shecan.util.server.LocaleHelper;
import ir.shecan.widget.CustomBottomBar;

public class MainActivityNew extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "DMainActivityNew";

    public static final String LAUNCH_ACTION = "ir.shecan.activity.MainActivityNew.LAUNCH_ACTION";
    public static final int LAUNCH_ACTION_NONE = 0;
    public static final int LAUNCH_ACTION_ACTIVATE = 1;
    public static final int LAUNCH_ACTION_DEACTIVATE = 2;
    public static final int LAUNCH_ACTION_SERVICE_DONE = 3;

    public static final String LAUNCH_FRAGMENT = "ir.shecan.activity.MainActivityNew.LAUNCH_FRAGMENT";
    public static final int FRAGMENT_NONE = -1;
    public static final int FRAGMENT_HOME = 0;
    public static final int FRAGMENT_DNS_TEST = 1;
    public static final int FRAGMENT_SETTINGS = 2;
    public static final int FRAGMENT_ABOUT = 3;
    public static final int FRAGMENT_LOG = 6;

    public static final String LAUNCH_NEED_RECREATE = "ir.shecan.activity.MainActivityNew.LAUNCH_NEED_RECREATE";
    public static final String LAST_TAB = "LAST_TAB_KEY";
    private int currentTab = 1;

    private static MainActivityNew instance = null;

    private ToolbarFragment currentFragment;

    private ActivityResultLauncher<Intent> vpnPermissionLauncher;

    private ActivityMainNewBinding binding;


    public static MainActivityNew getInstance() {
        return instance;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(LAST_TAB, currentTab);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Shecan.getInstance().updateLocale();
        applyTheme();
        super.onCreate(savedInstanceState);

        int selectedTab = 1; // default
        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt(LAST_TAB, 1);
        }

        instance = this;
        binding = ActivityMainNewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        vpnPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        onVpnPermissionGranted();
                    }
                }
        );

        binding.toolbar.appBarLayout.setPadding(0, getStatusBarHeight(), 0, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    Log.d("FCM", "Token: " + token);

                    FirebaseMessaging.getInstance().subscribeToTopic("afterPushPoleScenarioTopic")
                            .addOnCompleteListener(subscribeTask -> {
                                if (!subscribeTask.isSuccessful()) {
                                    Log.w("FCM", "Subscription failed", subscribeTask.getException());
                                } else {
                                    Log.d("FCM", "Subscribed to topic");
                                }
                            });
                });

        // Custom Bottom Bar
        CustomBottomBar bar = binding.customBar;
        bar.addItem(getString(R.string.connections), R.drawable.ic_connection_inactive, R.drawable.ic_config_active);
        bar.addItem(getString(R.string.connect), R.drawable.ic_vpn_inactive, R.drawable.ic_vpn_active);
        bar.addItem(getString(R.string.setting), R.drawable.ic_setting_inactive, R.drawable.ic_profile_active);

        bar.setOnItemSelected(index -> {
            currentTab = index;
            updateFragment(index);
        });

        currentTab = selectedTab;
        bar.select(selectedTab);
        updateFragment(selectedTab);

        handleIntent(getIntent());
    }

    public void switchFragment(Class fragmentClass, boolean isHome, boolean isAdd) {

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        try {
            ToolbarFragment fragment = (ToolbarFragment) fragmentClass.newInstance();

            if (isAdd) {
                if (currentFragment != null) {
                    ft.hide(currentFragment);
                }
                ft.add(R.id.id_content, fragment);
            } else {
                ft.replace(R.id.id_content, fragment);
            }

            ft.commitAllowingStateLoss();
            currentFragment = fragment;

        } catch (Exception e) {
            Logger.logException(e);
        }

        Window window = getWindow();

        CoordinatorLayout coordinatorLayout = binding.idContent;
        CoordinatorLayout.LayoutParams params =
                (CoordinatorLayout.LayoutParams) coordinatorLayout.getLayoutParams();

        AppBarLayout appBarLayout = binding.toolbar.appBarLayout;

        if (isHome) {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

            if (!hasPermanentMenuKey()) {
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            }

            appBarLayout.setPadding(0, getStatusBarHeight(), 0, 0);
            params.setBehavior(null);

        } else {

            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

            window.setStatusBarColor(fetchPrimaryDarkColor());
            appBarLayout.setPadding(0, 0, 0, 0);
            params.setBehavior(new AppBarLayout.ScrollingViewBehavior());

            window.getDecorView().setFitsSystemWindows(true);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }

        coordinatorLayout.requestLayout();
    }


    private boolean hasPermanentMenuKey() {
        return ViewConfiguration.get(getApplicationContext()).hasPermanentMenuKey();
    }

    public int getStatusBarHeight() {
        return (int) Math.ceil(25 * getResources().getDisplayMetrics().density);
    }

    private int fetchPrimaryDarkColor() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.color.colorPrimaryDark, typedValue, true);
        return typedValue.data;
    }

    @Override
    public void onBackPressed() {
        if (!(currentFragment instanceof HomeFragment)) {
            switchFragment(HomeFragment.class, true, false);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
        currentFragment = null;
        binding = null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleIntent(intent);
    }

    @Override
    public void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result == Activity.RESULT_OK) {

            if (ShecanVpnService.isProMode()) {
                ShecanVpnService.primaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getProPrimary());
                ShecanVpnService.secondaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getProSecondary());
            } else {
                ShecanVpnService.primaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getPrimary());
                ShecanVpnService.secondaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getSecondary());
            }

            Shecan.getInstance().startService(
                    Shecan.getServiceIntent(getApplicationContext()).setAction(ShecanVpnService.ACTION_ACTIVATE)
            );
            Shecan.updateShortcut(getApplicationContext());
        }
    }


    private void handleIntent(Intent intent) {

        int launchAction = intent.getIntExtra(LAUNCH_ACTION, LAUNCH_ACTION_NONE);

        if (launchAction == LAUNCH_ACTION_ACTIVATE) {
            this.activateService();
        } else if (launchAction == LAUNCH_ACTION_DEACTIVATE) {
            Shecan.deactivateService(getApplicationContext());
        } else if (launchAction == LAUNCH_ACTION_SERVICE_DONE) {
            Shecan.updateShortcut(getApplicationContext());
            applyTheme();
            this.recreate();
        }

        int fragment = intent.getIntExtra(LAUNCH_FRAGMENT, FRAGMENT_NONE);

        if (intent.getBooleanExtra(LAUNCH_NEED_RECREATE, false)) {
            if (fragment != FRAGMENT_NONE)
                getIntent().putExtra(MainActivityNew.LAUNCH_FRAGMENT, fragment);
            recreate();
            return;
        }

        switch (fragment) {
            case FRAGMENT_ABOUT:
                switchFragment(AboutFragment.class, false, false);
                break;
            case FRAGMENT_DNS_TEST:
                switchFragment(DNSTestFragment.class, false, false);
                break;
            case FRAGMENT_HOME:
                switchFragment(HomeFragment.class, true, false);
                break;
            case FRAGMENT_SETTINGS:
                switchFragment(SettingsFragment.class, false, false);
                break;
            case FRAGMENT_LOG:
                switchFragment(LogFragment.class, false, false);
                break;
        }

        if (currentFragment == null) {
            switchFragment(HomeFragment.class, true, false);
        }
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        switch (id) {
            case R.id.nav_about:
                switchFragment(AboutFragment.class, false, false);
                break;
            case R.id.nav_dns_test:
                switchFragment(DNSTestFragment.class, false, false);
                break;
            case R.id.nav_home:
                switchFragment(HomeFragment.class, true, false);
                break;
            case R.id.nav_settings:
                switchFragment(SettingsFragment.class, false, false);
                break;
            case R.id.nav_log:
                switchFragment(LogFragment.class, false, false);
                break;
        }

        InputMethodManager imm =
                (InputMethodManager) Shecan.getInstance().getSystemService(Context.INPUT_METHOD_SERVICE);

        imm.hideSoftInputFromWindow(binding.idContent.getWindowToken(), 0);

        return true;
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Shecan.getInstance().updateLocale();
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }


    public void activateService() {
        Intent intent = VpnService.prepare(Shecan.getInstance());
        if (intent != null) {
            if (intent.resolveActivity(getPackageManager()) != null) {
                vpnPermissionLauncher.launch(intent);
            } else {
                runOnUiThread(() ->
                        Toast.makeText(this, "دستگاه شما از VPN داخلی پشتیبانی نمی‌کند.", Toast.LENGTH_LONG).show()
                );
            }
        } else {
            onVpnPermissionGranted();
        }

        long activateCounter = Shecan.configurations.getActivateCounter();
        if (activateCounter != -1) {
            Shecan.configurations.setActivateCounter(++activateCounter);
        }
    }

    private void onVpnPermissionGranted() {
        if (ShecanVpnService.isProMode()) {
            ShecanVpnService.primaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getProPrimary());
            ShecanVpnService.secondaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getProSecondary());
        } else {
            ShecanVpnService.primaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getPrimary());
            ShecanVpnService.secondaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getSecondary());
        }

        Shecan.getInstance().startService(
                Shecan.getServiceIntent(getApplicationContext()).setAction(ShecanVpnService.ACTION_ACTIVATE)
        );
        Shecan.updateShortcut(getApplicationContext());
    }

    private AppStorage appStorage;
    private int currentMode = 0;

    @SuppressLint("WrongConstant")
    private void applyTheme() {
        appStorage = new AppStorage(getApplicationContext());
        AppConfig appConfig = appStorage.getAppConfig(AppConfig.class);
        if(appConfig != null){
            int mode = appConfig.getMode();
            currentMode = mode;
            AppCompatDelegate.setDefaultNightMode(mode);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        appStorage = new AppStorage(getApplicationContext());
        AppConfig appConfig = appStorage.getAppConfig(AppConfig.class);
        if(appConfig != null){
            int mode = appConfig.getMode();
            if (currentMode != mode) {
                recreate();
            }
            currentMode = mode;
        }
    }

    private void updateFragment(int index) {
        switch (index) {
            case 0:
                binding.toolbar.toolbarLogo.setVisibility(GONE);
                binding.toolbar.toolbarTitle.setVisibility(VISIBLE);
                binding.toolbar.toolbarTitle.setText("تراکنش ها");
                switchFragment(ConfigListFragment.class, true, false);
                break;
            case 1:
                binding.toolbar.toolbarLogo.setVisibility(VISIBLE);
                binding.toolbar.toolbarTitle.setVisibility(GONE);
                switchFragment(HomeFragment.class, true, false);
                break;
            case 2:
                binding.toolbar.toolbarLogo.setVisibility(GONE);
                binding.toolbar.toolbarTitle.setVisibility(VISIBLE);
                binding.toolbar.toolbarTitle.setText("تنظیمات");
                switchFragment(ProfileFragment.class, true, false);
                break;
        }
    }
}
