package ir.shecan.ui.activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static ir.shecan.core.util.AppUtils.adjustUIForFragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import ir.shecan.R;
import ir.shecan.core.constant.Constant;
import ir.shecan.core.util.AppUtils;
import ir.shecan.databinding.ActivityPanelWebBinding;

public class PanelWebActivity extends AppCompatActivity {

    private static final String EXTRA_URL = "url";
    private static final String EXTRA_TITLE_RES = "title_res";
    private static final String EXTRA_REQUIRES_AUTH = "requires_auth";
    private static final String PANEL_HOST = "my.shecan.ir";
    private static final String EMBEDDED_PANEL_SCRIPT =
            "(function(){"
                    + "var styleId='shecan-android-embedded-style';"
                    + "var style=document.getElementById(styleId);"
                    + "if(!style){style=document.createElement('style');style.id=styleId;"
                    + "style.textContent='#sidebar,#sidebar-toggle{display:none!important;}'"
                    + "+'#sidebar + div{margin-right:0!important;margin-left:0!important;}';"
                    + "(document.head||document.documentElement).appendChild(style);}"
                    + "function apply(){"
                    + "var sidebar=document.getElementById('sidebar');"
                    + "if(sidebar){sidebar.style.setProperty('display','none','important');"
                    + "var content=sidebar.nextElementSibling;"
                    + "if(content){content.style.setProperty('margin-right','0','important');"
                    + "content.style.setProperty('margin-left','0','important');}}"
                    + "var toggle=document.getElementById('sidebar-toggle');"
                    + "if(toggle){toggle.style.setProperty('display','none','important');}"
                    + "document.querySelectorAll('span').forEach(function(label){"
                    + "if(label.textContent.replace(/\\s/g,'')==='خریداشتراک'){"
                    + "var action=label.closest('button,a')||label.parentElement.parentElement.parentElement;"
                    + "if(action){action.style.setProperty('display','none','important');}}});}"
                    + "apply();"
                    + "if(!window.__shecanAndroidEmbeddedObserver){"
                    + "window.__shecanAndroidEmbeddedObserver=new MutationObserver(apply);"
                    + "window.__shecanAndroidEmbeddedObserver.observe(document.documentElement,{childList:true,subtree:true});}"
                    + "})();";

    private ActivityPanelWebBinding binding;
    private String panelEntryUrl;
    private String entryHost;
    private boolean panelEntryHistoryCleared;

    public static void openTickets(Context context) {
        openAuthenticated(context, Constant.TicketUrlRaw, R.string.profile_tickets);
    }

    public static void openTransactions(Context context) {
        openAuthenticated(context, Constant.TransactionUrlRaw, R.string.profile_transactions);
    }

    public static void openDomainSupport(Context context) {
        openAuthenticated(context, Constant.DomainUrlRaw, R.string.profile_domain_support);
    }

    public static void openTerms(Context context) {
        openPublic(context, Constant.TermsUrl, R.string.billing_rules_link_text);
    }

    public static void openPlans(Context context) {
        openPublic(context, Constant.PlanUrl, R.string.title_billing_plans);
    }

    public static void openAuthenticated(Context context, String url, int titleRes) {
        open(context, url, titleRes, true);
    }

    public static void openPublic(Context context, String url, int titleRes) {
        open(context, url, titleRes, false);
    }

    private static void open(Context context, String url, int titleRes, boolean requiresAuth) {
        Intent intent = new Intent(context, PanelWebActivity.class);
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra(EXTRA_TITLE_RES, titleRes);
        intent.putExtra(EXTRA_REQUIRES_AUTH, requiresAuth);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppUtils.applySavedNightMode(this);
        super.onCreate(savedInstanceState);
        binding = ActivityPanelWebBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupWebView();

        panelEntryUrl = getIntent().getStringExtra(EXTRA_URL);
        entryHost = getHost(panelEntryUrl);
        boolean requiresAuth = getIntent().getBooleanExtra(EXTRA_REQUIRES_AUTH, true);
        String redirectUrl = requiresAuth ? AppUtils.buildRedirect(panelEntryUrl, this) : panelEntryUrl;
        if (redirectUrl == null || redirectUrl.trim().isEmpty()) {
            Toast.makeText(this, R.string.panel_web_login_required, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        binding.panelWebView.loadUrl(redirectUrl);
    }

    private void setupToolbar() {
        binding.toolbar.back.setVisibility(VISIBLE);
        binding.toolbar.back.setOnClickListener(v -> navigateBack());
        binding.toolbar.toolbarLogo.setVisibility(GONE);
        binding.toolbar.toolbarTitle.setVisibility(VISIBLE);
        int titleRes = getIntent().getIntExtra(EXTRA_TITLE_RES, R.string.profile_tickets);
        binding.toolbar.toolbarTitle.setText(titleRes);
        binding.toolbar.vip.setVisibility(GONE);

        adjustUIForFragment(this, R.color.mainBack, R.color.mainBack);
        binding.toolbar.appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.mainBack));
    }

    private void setupWebView() {
        WebSettings settings = binding.panelWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        binding.panelWebView.setBackgroundColor(ContextCompat.getColor(this, R.color.mainBack));
        binding.panelWebView.setVisibility(View.INVISIBLE);
        binding.panelWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                binding.panelWebView.setVisibility(View.INVISIBLE);
                binding.progress.setVisibility(VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (isPanelHost(url)) {
                    view.evaluateJavascript(EMBEDDED_PANEL_SCRIPT, null);
                }
                clearAuthenticationHistoryAtEntry(view, url);
                binding.progress.setVisibility(GONE);
                binding.panelWebView.postDelayed(() -> binding.panelWebView.setVisibility(VISIBLE), 100);
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                super.doUpdateVisitedHistory(view, url, isReload);
                clearAuthenticationHistoryAtEntry(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (shouldOpenInsideWebView(uri)) {
                    return false;
                }
                AppUtils.openUrl(uri.toString(), PanelWebActivity.this);
                return true;
            }
        });
    }

    private void clearAuthenticationHistoryAtEntry(WebView view, String url) {
        if (panelEntryHistoryCleared || !isPanelEntryUrl(url)) {
            return;
        }
        view.clearHistory();
        panelEntryHistoryCleared = true;
    }

    private boolean isPanelEntryUrl(String url) {
        if (url == null || panelEntryUrl == null) {
            return false;
        }
        Uri current = Uri.parse(url);
        Uri entry = Uri.parse(panelEntryUrl);
        return shouldOpenInsideWebView(current)
                && entry.getPath() != null
                && entry.getPath().equals(current.getPath());
    }

    private boolean shouldOpenInsideWebView(Uri uri) {
        String host = uri.getHost();
        return host != null && (host.equalsIgnoreCase(PANEL_HOST)
                || (entryHost != null && host.equalsIgnoreCase(entryHost)));
    }

    private boolean isPanelHost(String url) {
        return PANEL_HOST.equalsIgnoreCase(getHost(url));
    }

    private String getHost(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        return Uri.parse(url).getHost();
    }

    @Override
    public void onBackPressed() {
        navigateBack();
    }

    private void navigateBack() {
        if (binding != null && binding.panelWebView.canGoBack()) {
            binding.panelWebView.goBack();
            return;
        }
        AppUtils.applySavedNightMode(this);
        finish();
    }

    @Override
    protected void onPause() {
        AppUtils.applySavedNightMode(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        AppUtils.applySavedNightMode(this);
        if (binding != null) {
            binding.panelWebView.stopLoading();
            binding.panelWebView.setWebViewClient(null);
            binding.panelWebView.destroy();
        }
        super.onDestroy();
    }
}
