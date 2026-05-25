package ir.shecan.ui.fragment.refactor;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static ir.shecan.core.util.AppUtils.adjustUIForFragment;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ir.cafebazaar.poolakey.entity.PurchaseInfo;
import ir.myket.billingclient.util.Purchase;
import ir.shecan.BuildConfig;
import ir.shecan.R;
import ir.shecan.core.billing.BillingHost;
import ir.shecan.core.billing.BillingPaymentReturnState;
import ir.shecan.core.billing.BillingPeriod;
import ir.shecan.core.billing.BillingPlan;
import ir.shecan.core.billing.BillingPlanCatalog;
import ir.shecan.core.billing.BillingPlanPrice;
import ir.shecan.core.billing.BillingPurchaseObserver;
import ir.shecan.core.billing.BillingSla;
import ir.shecan.core.billing.BillingStore;
import ir.shecan.core.constant.Constant;
import ir.shecan.core.util.AppUtils;
import ir.shecan.core.util.TrackingUtils;
import ir.shecan.data.api.ApiCallback;
import ir.shecan.data.api.AuthApi;
import ir.shecan.data.modelDto.DiscountViewModel;
import ir.shecan.data.modelDto.EmptyResponse;
import ir.shecan.data.modelDto.IapVerifyViewModel;
import ir.shecan.data.modelDto.PriceViewModel;
import ir.shecan.data.modelDto.SitePaymentViewModel;
import ir.shecan.data.modelDto.VerifyApiViewModel;
import ir.shecan.data.storage.AppStorage;
import ir.shecan.databinding.FragmentBillingPlansBinding;
import ir.shecan.ui.activity.MainActivityNew;
import ir.shecan.ui.fragment.ToolbarFragment;
import saman.zamani.persiandate.PersianDate;
import saman.zamani.persiandate.PersianDateFormat;

public class BillingPlansFragment extends ToolbarFragment implements BillingPurchaseObserver {

    private static final String TAG = "BillingPlansFragment";
    public static final String ARG_PREFILL_SLA = "prefill_sla";
    public static final String ARG_PREFILL_PERIOD = "prefill_period";

    private FragmentBillingPlansBinding binding;
    private AuthApi authApi;
    private AppStorage storage;
    private final NumberFormat numberFormat = NumberFormat.getInstance(new Locale("fa", "IR"));
    private final List<BillingSla> serviceOptions = new ArrayList<>();
    private final List<BillingPeriod> periodOptions = new ArrayList<>();
    private BillingPlanPrice selectedItem;
    private BillingPlan pendingPlan;
    private boolean payButtonReady;
    private boolean paymentInProgress;
    private boolean suppressSelectionEvents;
    private int priceRequestSeq;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBillingPlansBinding.inflate(inflater, container, false);
        authApi = new AuthApi(requireContext());
        storage = new AppStorage(requireContext());
        setupUi();
        setupOptions();
        applyPrefillSelection();
        updateSelectedPlan();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        BillingHost host = getBillingHost();
        if (host != null) {
            host.setBillingPurchaseObserver(this);
        }

        if (getActivity() instanceof MainActivityNew) {
            MainActivityNew activity = (MainActivityNew) getActivity();
            activity.binding.customBar.select(2);
            activity.binding.toolbar.toolbarLogo.setVisibility(GONE);
            activity.binding.toolbar.toolbarTitle.setVisibility(VISIBLE);
            activity.binding.toolbar.toolbarTitle.setText(R.string.title_billing_plans);
            adjustUIForFragment(activity, R.color.profileBackground, R.color.mainBack);
        } else if (getActivity() != null) {
            adjustUIForFragment(getActivity(), R.color.profileBackground, R.color.mainBack);
        }
    }

    private void setupUi() {
        BillingStore store = BillingStore.current();
        binding.tvStore.setText(getString(R.string.billing_store_prefix, store.getTitle()));
        showStatus(null, false);
        binding.tvDiscountToggle.setVisibility(VISIBLE);
        binding.discountRow.setVisibility(GONE);
        binding.btnClearDiscount.setVisibility(GONE);
        binding.tvDiscountMessage.setVisibility(GONE);
        setupRulesLink();
        binding.checkboxRules.setOnCheckedChangeListener((buttonView, isChecked) -> updatePayButtonState());
        binding.btnPay.setOnClickListener(v -> {
            logSelectedPlanEvent(TrackingUtils.EVENT_BILLING_PURCHASE_CLICK);
            if (payButtonReady) {
                startPurchase();
            } else {
                showDisabledPayReason();
            }
        });
        binding.btnApplyDiscount.setOnClickListener(v -> applyDiscountCode());
        binding.btnClearDiscount.setOnClickListener(v -> clearAppliedDiscount());
        binding.tvDiscountToggle.setOnClickListener(v -> {
            binding.discountRow.setVisibility(VISIBLE);
            binding.tvDiscountToggle.setVisibility(GONE);
        });
    }

    private void setupRulesLink() {
        String fullText = getString(R.string.billing_accept_rules);
        String linkText = getString(R.string.billing_rules_link_text);
        int linkStart = fullText.indexOf(linkText);
        if (linkStart < 0) {
            binding.checkboxRules.setText(fullText);
            return;
        }

        SpannableString text = new SpannableString(fullText);
        int linkEnd = linkStart + linkText.length();
        text.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                AppUtils.openUrl(Constant.TermsUrl, requireActivity());
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setColor(ContextCompat.getColor(requireContext(), R.color.orangeMain));
                ds.setUnderlineText(true);
            }
        }, linkStart, linkEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        binding.checkboxRules.setText(text);
        binding.checkboxRules.setMovementMethod(LinkMovementMethod.getInstance());
        binding.checkboxRules.setHighlightColor(android.graphics.Color.TRANSPARENT);
    }

    private void setupOptions() {
        suppressSelectionEvents = true;
        serviceOptions.clear();
        periodOptions.clear();

        Set<BillingSla> uniqueServices = new LinkedHashSet<>();
        for (BillingPlan plan : BillingPlanCatalog.purchasablePlans()) {
            uniqueServices.add(plan.getSla());
        }
        serviceOptions.addAll(uniqueServices);

        periodOptions.add(BillingPeriod.ONE_MONTH);
        periodOptions.add(BillingPeriod.THREE_MONTHS);
        periodOptions.add(BillingPeriod.SIX_MONTHS);
        periodOptions.add(BillingPeriod.ONE_YEAR);

        binding.spinnerService.setAdapter(createSpinnerAdapter(buildServiceTitles()));
        binding.spinnerPeriod.setAdapter(createSpinnerAdapter(buildPeriodTitles()));

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!suppressSelectionEvents) updateSelectedPlan();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        binding.spinnerService.setOnItemSelectedListener(listener);
        binding.spinnerPeriod.setOnItemSelectedListener(listener);
        suppressSelectionEvents = false;
    }

    private void applyPrefillSelection() {
        Bundle args = getArguments();
        if (args == null) return;

        String prefillSla = args.getString(ARG_PREFILL_SLA);
        String prefillPeriod = args.getString(ARG_PREFILL_PERIOD);

        int serviceIndex = findServiceIndex(prefillSla);
        int periodIndex = findPeriodIndex(prefillPeriod);

        suppressSelectionEvents = true;
        if (serviceIndex >= 0) binding.spinnerService.setSelection(serviceIndex, false);
        if (periodIndex >= 0) binding.spinnerPeriod.setSelection(periodIndex, false);
        suppressSelectionEvents = false;
    }

    private int findServiceIndex(String apiValue) {
        if (apiValue == null || apiValue.trim().isEmpty()) return -1;
        for (int i = 0; i < serviceOptions.size(); i++) {
            if (apiValue.equals(serviceOptions.get(i).getApiValue())) return i;
        }
        return -1;
    }

    private int findPeriodIndex(String apiValue) {
        if (apiValue == null || apiValue.trim().isEmpty()) return -1;
        for (int i = 0; i < periodOptions.size(); i++) {
            if (apiValue.equals(periodOptions.get(i).getApiValue())) return i;
        }
        return -1;
    }

    private List<String> buildServiceTitles() {
        List<String> titles = new ArrayList<>();
        for (BillingSla sla : serviceOptions) {
            titles.add(getString(R.string.billing_service_name, sla.getTitle()));
        }
        return titles;
    }

    private ArrayAdapter<String> createSpinnerAdapter(List<String> values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                R.layout.item_billing_spinner,
                values
        ) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                forceRtlSpinnerItem(view);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                forceRtlSpinnerItem(view);
                return view;
            }
        };
        adapter.setDropDownViewResource(R.layout.item_billing_spinner);
        return adapter;
    }

    private void forceRtlSpinnerItem(View view) {
        view.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        view.setTextDirection(View.TEXT_DIRECTION_LTR);
        view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.lightBack));
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            textView.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        }
    }

    private List<String> buildPeriodTitles() {
        List<String> titles = new ArrayList<>();
        for (BillingPeriod period : periodOptions) {
            titles.add(period.getTitle());
        }
        return titles;
    }

    private void updateSelectedPlan() {
        BillingPlan plan = findSelectedPlan();
        if (plan == null) {
            selectedItem = null;
            showStatus(getString(R.string.billing_plan_not_purchasable), true);
            clearPriceUi();
            updatePayButtonState();
            return;
        }

        selectedItem = new BillingPlanPrice(plan);
        logSelectedPlanEvent(TrackingUtils.EVENT_BILLING_PLAN_SELECTED);
        pendingPlan = null;
        binding.etDiscountCode.setText("");
        binding.discountRow.setVisibility(GONE);
        binding.tvDiscountToggle.setVisibility(VISIBLE);
        binding.btnClearDiscount.setVisibility(GONE);
        binding.tvDiscountMessage.setVisibility(GONE);
        showStatus(null, false);
        updateFeatureBox(plan.getSla());
        loadSelectedPrice();
    }

    private BillingPlan findSelectedPlan() {
        if (serviceOptions.isEmpty() || periodOptions.isEmpty()) return null;
        int serviceIndex = Math.max(0, binding.spinnerService.getSelectedItemPosition());
        int periodIndex = Math.max(0, binding.spinnerPeriod.getSelectedItemPosition());
        BillingSla sla = serviceOptions.get(Math.min(serviceIndex, serviceOptions.size() - 1));
        BillingPeriod period = periodOptions.get(Math.min(periodIndex, periodOptions.size() - 1));

        for (BillingPlan plan : BillingPlanCatalog.purchasablePlans()) {
            if (plan.getSla() == sla && plan.getPeriod() == period) return plan;
        }
        return null;
    }

    private void loadSelectedPrice() {
        if (selectedItem == null) return;
        int requestId = ++priceRequestSeq;
        selectedItem.setLoading(true);
        selectedItem.setErrorMessage(null);
        clearPriceUi();
        showStatus(null, false);
        updatePayButtonState();

        BillingPlan plan = selectedItem.getPlan();
        VerifyApiViewModel token = storage != null ? storage.getToken(VerifyApiViewModel.class) : null;
        authApi.price(
                token != null ? token.getApiKey() : null,
                plan.getSla().getApiValue(),
                plan.getPeriod().getApiValue(),
                0,
                new ApiCallback<PriceViewModel>() {
                    @Override
                    public void onSuccess(PriceViewModel data, boolean fromCache) {
                        if (binding == null || requestId != priceRequestSeq) return;
                        selectedItem.setLoading(false);
                        selectedItem.setPrice(data);
                        showStatus(null, false);
                        renderPrice();
                        updatePayButtonState();
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        if (binding == null || requestId != priceRequestSeq) return;
                        selectedItem.setLoading(false);
                        selectedItem.setErrorMessage(formatBillingApiError(statusCode, message, R.string.billing_unknown_price));
                        showStatus(selectedItem.getErrorMessage(), true);
                        clearPriceUi();
                        updatePayButtonState();
                    }
                }
        );
    }

    private void renderPrice() {
        if (selectedItem == null || selectedItem.getPrice() == null) {
            clearPriceUi();
            return;
        }
        BillingStore store = BillingStore.current();
        binding.tvServicePrice.setText(formatToman(selectedItem.getServicePrice(store)));
        binding.tvTax.setText(formatToman(selectedItem.getTaxPrice(store)));
        binding.tvTotal.setText(formatToman(selectedItem.getTotalPrice(store)));
        PriceViewModel priceViewModel = selectedItem.getPrice();
        if (priceViewModel.getDueDate() != null) {
            binding.tvDueDate.setText(getString(R.string.billing_expire_date, formatPersianDate(priceViewModel.getDueDate())));
        } else {
            binding.tvDueDate.setText("");
        }
    }

    private void clearPriceUi() {
        binding.tvServicePrice.setText(formatToman(0));
        binding.tvTax.setText(formatToman(0));
        binding.tvTotal.setText(formatToman(0));
        binding.tvDueDate.setText("");
    }

    private void updateFeatureBox(BillingSla sla) {
        binding.tvFeatureOne.setText(getString(R.string.billing_feature_speed, getSpeedText(sla)));
        binding.tvFeatureTwo.setText(getString(R.string.billing_feature_dns, getDnsText(sla)));
    }

    private String getSpeedText(BillingSla sla) {
        switch (sla) {
            case GOLD:
                return "100Mbps";
            case COMMERCIAL:
                return getString(R.string.billing_feature_unlimited);
            case BRONZE:
                return "20Mbps";
            case SILVER:
            default:
                return "40Mbps";
        }
    }

    private String getDnsText(BillingSla sla) {
        switch (sla) {
            case GOLD:
                return "10000";
            case COMMERCIAL:
                return getString(R.string.billing_feature_unlimited);
            case BRONZE:
                return "1000";
            case SILVER:
            default:
                return "2000";
        }
    }

    private void startPurchase() {
        if (paymentInProgress) {
            showMessage(getString(R.string.billing_wait));
            return;
        }
        if (selectedItem == null) return;
        if (!binding.checkboxRules.isChecked()) {
            showMessage(getString(R.string.billing_rules_required));
            return;
        }
        if (selectedItem.getPrice() == null) {
            showMessage(getString(R.string.billing_price_not_ready));
            return;
        }

        pendingPlan = selectedItem.getPlan();
        BillingStore store = BillingStore.current();
        logSelectedPlanEvent(TrackingUtils.EVENT_BILLING_PURCHASE_START);
        showStatus(getString(R.string.billing_purchase_start, pendingPlan.getTitle(), store.getTitle()), false);
        setPaymentLoading(true);

        BillingHost host = getBillingHost();
        if (host == null) {
            setPaymentLoading(false);
            return;
        }

        if (!host.isBillingReadyForStore(store)) {
            String message = getString(R.string.billing_not_ready, store.getTitle());
            showError(message);
            pendingPlan = null;
            setPaymentLoading(false);
            return;
        }

        switch (store) {
            case CAFE_BAZAAR:
                host.launchCafeBazaarPurchase(pendingPlan.getSku());
                break;
            case MYKET:
                host.launchMyketPurchase(pendingPlan.getSku());
                break;
            case SITE:
                startSitePayment();
                break;
        }
    }

    private void startSitePayment() {
        if (selectedItem == null) return;
        VerifyApiViewModel token = storage != null ? storage.getToken(VerifyApiViewModel.class) : null;
        if (token == null || token.getApiKey() == null) {
            showError(getString(R.string.billing_payment_login_required));
            pendingPlan = null;
            setPaymentLoading(false);
            return;
        }

        long originalPrice = selectedItem.getPrice().getSafePrice();
        long finalPrice = getPayablePrice();
        long discount = Math.max(0L, originalPrice - finalPrice);

        showStatus(getString(R.string.billing_site_payment_creating), false);
        authApi.sitePayment(
                token.getApiKey(),
                finalPrice,
                selectedItem.getPlan().getSla().getApiValue(),
                selectedItem.getPlan().getPeriod().getApiValue(),
                discount,
                selectedItem.getDiscountCode(),
                token,
                new ApiCallback<SitePaymentViewModel>() {
                    @Override
                    public void onSuccess(SitePaymentViewModel data, boolean fromCache) {
                        if (binding == null) return;
                        pendingPlan = null;
                        if (data == null || data.getUrl() == null || data.getUrl().trim().isEmpty()) {
                            showError(getString(R.string.billing_site_payment_empty_url));
                            setPaymentLoading(false);
                            return;
                        }
                        showStatus(getString(R.string.billing_site_payment_redirecting), false);
                        new BillingPaymentReturnState(requireContext()).markBrowserOpening();
                        AppUtils.openUrl(data.getUrl(), requireActivity());
                        setPaymentLoading(false);
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        if (binding == null) return;
                        pendingPlan = null;
                        showPaymentError(statusCode, message, R.string.billing_site_payment_failed);
                        setPaymentLoading(false);
                    }
                }
        );
    }

    private void applyDiscountCode() {
        if (selectedItem == null) return;
        logSelectedPlanEvent(TrackingUtils.EVENT_BILLING_DISCOUNT_CLICK);
        String code = binding.etDiscountCode.getText() != null
                ? binding.etDiscountCode.getText().toString().trim()
                : "";
        if (code.isEmpty()) {
            showDiscountMessage(getString(R.string.billing_discount_required));
            return;
        }
        if (selectedItem.getPrice() == null) {
            showDiscountMessage(getString(R.string.billing_discount_price_required));
            return;
        }

        VerifyApiViewModel token = storage != null ? storage.getToken(VerifyApiViewModel.class) : null;
        if (token == null || token.getApiKey() == null) {
            showDiscountMessage(getString(R.string.billing_discount_login_required));
            return;
        }

        selectedItem.setDiscountLoading(true);
        showDiscountMessage(null);
        binding.btnApplyDiscount.setText("...");
        updatePayButtonState();

        authApi.discount(
                token.getApiKey(),
                selectedItem.getPrice().getSafePrice(),
                selectedItem.getPlan().getPlanId(),
                normalizeIranMobile(token.getLogin()),
                code,
                selectedItem.getPlan().getDurationId(),
                new ApiCallback<DiscountViewModel>() {
                    @Override
                    public void onSuccess(DiscountViewModel data, boolean fromCache) {
                        if (binding == null || selectedItem == null) return;
                        selectedItem.setDiscountLoading(false);
                        Long finalPrice = resolveDiscountedPrice(data, selectedItem.getPrice().getSafePrice());
                        String message = data != null && data.getMessage() != null
                                ? data.getMessage()
                                : getString(R.string.billing_discount_applied);
                        selectedItem.applyDiscount(code, finalPrice, message);
                        logSelectedPlanEvent(TrackingUtils.EVENT_BILLING_DISCOUNT_SUCCESS);
                        binding.btnApplyDiscount.setText(R.string.billing_apply_discount);
                        binding.btnClearDiscount.setVisibility(VISIBLE);
                        showDiscountMessage(message);
                        renderPrice();
                        updatePayButtonState();
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        if (binding == null || selectedItem == null) return;
                        selectedItem.setDiscountLoading(false);
                        selectedItem.clearDiscount(formatBillingApiError(statusCode, message, R.string.billing_discount_invalid));
                        binding.btnApplyDiscount.setText(R.string.billing_apply_discount);
                        binding.btnClearDiscount.setVisibility(GONE);
                        showDiscountMessage(selectedItem.getDiscountMessage());
                        renderPrice();
                        updatePayButtonState();
                    }
                }
        );
    }

    private void clearAppliedDiscount() {
        if (selectedItem == null) return;
        selectedItem.clearDiscount(null);
        binding.etDiscountCode.setText("");
        binding.btnClearDiscount.setVisibility(GONE);
        showDiscountMessage(null);
        renderPrice();
        updatePayButtonState();
    }

    private Long resolveDiscountedPrice(DiscountViewModel data, long originalPrice) {
        if (data == null) return null;
        Long finalPrice = data.getFinalPrice();
        if (finalPrice != null) return finalPrice;
        Long discount = data.getDiscount();
        return discount != null ? Math.max(0L, originalPrice - discount) : null;
    }

    private void showDiscountMessage(String message) {
        binding.tvDiscountMessage.setVisibility(message == null || message.isEmpty() ? GONE : VISIBLE);
        binding.tvDiscountMessage.setText(message == null ? "" : message);
    }

    private void updatePayButtonState() {
        updatePayButtonState(canPay());
    }

    private boolean canPay() {
        return selectedItem != null
                && selectedItem.getPrice() != null
                && !selectedItem.isLoading()
                && !selectedItem.isDiscountLoading()
                && !paymentInProgress
                && binding.checkboxRules.isChecked();
    }

    private void updatePayButtonState(boolean enabled) {
        payButtonReady = enabled;
        binding.btnPay.setEnabled(true);
        binding.btnPay.setAlpha(1f);
        binding.btnPay.setBackgroundResource(enabled ? R.drawable.primary_button : R.drawable.bg_billing_button_disabled);
        binding.btnPay.setTextColor(ContextCompat.getColor(
                requireContext(),
                enabled ? R.color.billingPayButtonEnabledText : R.color.billingPayButtonDisabledText
        ));
    }

    private void showDisabledPayReason() {
        if (pendingPlan != null || selectedItem != null && selectedItem.isDiscountLoading()) {
            showMessage(getString(R.string.billing_wait));
        } else if (!binding.checkboxRules.isChecked()) {
            showMessage(getString(R.string.billing_rules_required));
        } else {
            showMessage(getString(R.string.billing_price_not_ready));
        }
    }

    @Override
    public void onMarketplacePurchaseReady(BillingStore store, Object purchase, boolean restoredFromInventory) {
        if (binding == null) return;
        if (pendingPlan == null) {
            showStatus(getString(R.string.billing_restored_purchase_found, store.getTitle()), false);
            pendingPlan = BillingPlanCatalog.findBySku(getMarketplaceProductId(store, purchase));
            if (pendingPlan == null) {
                showError(getString(R.string.billing_iap_unknown_product));
                setPaymentLoading(false);
                return;
            }
        }

        setPaymentLoading(true);
        showStatus(getString(R.string.billing_iap_verify_in_progress), false);
        verifyMarketplacePurchase(store, purchase);
    }

    @Override
    public void onMarketplacePurchaseCanceled(BillingStore store, String sku) {
        if (binding == null) return;
        showError(getString(R.string.billing_purchase_canceled, store.getTitle()));
        android.os.Bundle params = selectedPlanParams();
        TrackingUtils.put(params, TrackingUtils.PARAM_STORE, store.name().toLowerCase(Locale.US));
        TrackingUtils.put(params, TrackingUtils.PARAM_PLAN_SKU, sku);
        TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_BILLING_PURCHASE_CANCEL, params);
        pendingPlan = null;
        setPaymentLoading(false);
    }

    @Override
    public void onMarketplaceBillingError(BillingStore store, String message) {
        if (binding == null) return;
        showError(getString(R.string.billing_payment_error, store.getTitle(), message));
        android.os.Bundle params = selectedPlanParams();
        TrackingUtils.put(params, TrackingUtils.PARAM_STORE, store.name().toLowerCase(Locale.US));
        TrackingUtils.put(params, TrackingUtils.PARAM_ERROR, message);
        TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_BILLING_PURCHASE_ERROR, params);
        setPaymentLoading(false);
    }

    private String formatToman(long rial) {
        return getString(R.string.billing_price_toman, numberFormat.format(Math.max(0L, rial)));
    }

    private long getPayablePrice() {
        return selectedItem != null ? selectedItem.getTotalPrice(BillingStore.current()) : 0L;
    }

    private void verifyMarketplacePurchase(BillingStore store, Object purchase) {
        VerifyApiViewModel token = storage != null ? storage.getToken(VerifyApiViewModel.class) : null;
        if (token == null || token.getApiKey() == null) {
            showError(getString(R.string.billing_payment_login_required));
            pendingPlan = null;
            setPaymentLoading(false);
            return;
        }

        String productId = getMarketplaceProductId(store, purchase);
        String purchaseToken = getMarketplacePurchaseToken(store, purchase);
        String storeOrderId = getMarketplaceOrderId(store, purchase);
        String packageName = getMarketplacePackageName(store, purchase);

        if (productId == null || productId.trim().isEmpty()) {
            productId = pendingPlan != null ? pendingPlan.getSku() : "";
        }
        if (packageName == null || packageName.trim().isEmpty()) {
            packageName = BuildConfig.APPLICATION_ID;
        }

        if (purchaseToken == null || purchaseToken.trim().isEmpty()) {
            showError(getString(R.string.billing_iap_purchase_token_missing));
            pendingPlan = null;
            setPaymentLoading(false);
            return;
        }

        authApi.verifyIap(
                token.getApiKey(),
                store == BillingStore.CAFE_BAZAAR ? "bazaar" : "myket",
                0,
                packageName,
                productId,
                purchaseToken,
                getIapVerificationAmount(),
                storeOrderId,
                new ApiCallback<IapVerifyViewModel>() {
                    @Override
                    public void onSuccess(IapVerifyViewModel data, boolean fromCache) {
                        if (binding == null) return;
                        if (data != null && data.isOk()) {
                            String order = data.getOrderId() != null
                                    ? getString(R.string.billing_order_code, String.valueOf(data.getOrderId()))
                                    : "";
                            consumeDiscountAfterIapVerifyIfNeeded(token, data, order);
                        } else {
                            String detail = data != null && data.getDetail() != null
                                    ? data.getDetail()
                                    : getString(R.string.billing_iap_verify_failed);
                            showError(detail);
                            pendingPlan = null;
                            setPaymentLoading(false);
                        }
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        if (binding == null) return;
                        showPaymentError(statusCode, message, R.string.billing_iap_verify_failed);
                        pendingPlan = null;
                        setPaymentLoading(false);
                    }
                }
        );
    }

    private long getIapVerificationAmount() {
        if (selectedItem == null || pendingPlan == null || selectedItem.getPrice() == null) return 0L;
        if (!pendingPlan.getSku().equals(selectedItem.getPlan().getSku())) return 0L;
        return getPayablePrice();
    }

    private void consumeDiscountAfterIapVerifyIfNeeded(VerifyApiViewModel token, IapVerifyViewModel verify, String orderMessage) {
        if (!hasAppliedDiscount() || verify.getOrderId() == null) {
            finishVerifiedIap(orderMessage);
            return;
        }

        authApi.useDiscount(
                normalizeIranMobile(token != null ? token.getLogin() : null),
                selectedItem.getDiscountCode(),
                verify.getOrderId(),
                selectedItem.getPrice().getSafePrice(),
                new ApiCallback<EmptyResponse>() {
                    @Override
                    public void onSuccess(EmptyResponse data, boolean fromCache) {
                        finishVerifiedIap(orderMessage);
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        String error = formatBillingApiError(statusCode, message, R.string.billing_discount_consume_failed);
                        Log.w(TAG, "Failed to consume discount code after IAP verify: " + error);
                        finishVerifiedIap(orderMessage);
                        showError(error);
                    }
                }
        );
    }

    private void finishVerifiedIap(String orderMessage) {
        if (binding == null) return;
        showStatus(getString(R.string.billing_iap_verified, orderMessage), false);
        showMessage(getString(R.string.billing_purchase_verified));
        logSelectedPlanEvent(TrackingUtils.EVENT_BILLING_PURCHASE_SUCCESS);
        pendingPlan = null;
        setPaymentLoading(false);
    }

    private boolean hasAppliedDiscount() {
        return selectedItem != null
                && selectedItem.getDiscountCode() != null
                && !selectedItem.getDiscountCode().trim().isEmpty()
                && selectedItem.getDiscountedPrice() != null
                && selectedItem.getPrice() != null;
    }

    private String normalizeIranMobile(String value) {
        if (value == null) return "";
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.startsWith("0098")) {
            digits = digits.substring(4);
        } else if (digits.startsWith("98")) {
            digits = digits.substring(2);
        }
        if (digits.length() == 10 && digits.startsWith("9")) {
            return "0" + digits;
        }
        return digits;
    }

    private String getMarketplaceProductId(BillingStore store, Object purchase) {
        if (store == BillingStore.CAFE_BAZAAR && purchase instanceof PurchaseInfo) {
            return ((PurchaseInfo) purchase).getProductId();
        }
        if (store == BillingStore.MYKET && purchase instanceof Purchase) {
            return ((Purchase) purchase).getSku();
        }
        return null;
    }

    private String getMarketplacePurchaseToken(BillingStore store, Object purchase) {
        if (store == BillingStore.CAFE_BAZAAR && purchase instanceof PurchaseInfo) {
            return ((PurchaseInfo) purchase).getPurchaseToken();
        }
        if (store == BillingStore.MYKET && purchase instanceof Purchase) {
            return ((Purchase) purchase).getToken();
        }
        return null;
    }

    private String getMarketplaceOrderId(BillingStore store, Object purchase) {
        if (store == BillingStore.CAFE_BAZAAR && purchase instanceof PurchaseInfo) {
            return ((PurchaseInfo) purchase).getOrderId();
        }
        if (store == BillingStore.MYKET && purchase instanceof Purchase) {
            return ((Purchase) purchase).getOrderId();
        }
        return null;
    }

    private String getMarketplacePackageName(BillingStore store, Object purchase) {
        if (store == BillingStore.CAFE_BAZAAR && purchase instanceof PurchaseInfo) {
            return ((PurchaseInfo) purchase).getPackageName();
        }
        if (store == BillingStore.MYKET && purchase instanceof Purchase) {
            return ((Purchase) purchase).getPackageName();
        }
        return null;
    }

    private String formatPersianDate(String rawDate) {
        try {
            if (rawDate == null || rawDate.trim().isEmpty()) return "";
            String normalized = rawDate.trim();
            if (normalized.contains("T")) normalized = normalized.substring(0, normalized.indexOf("T"));
            if (normalized.contains(" ")) normalized = normalized.substring(0, normalized.indexOf(" "));

            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(normalized);
            if (date == null) return rawDate;

            PersianDate persianDate = new PersianDate(date);
            return new PersianDateFormat("Y/m/d").format(persianDate);
        } catch (Exception ignored) {
            return rawDate;
        }
    }

    private BillingHost getBillingHost() {
        return getActivity() instanceof BillingHost ? (BillingHost) getActivity() : null;
    }

    private void showMessage(String message) {
        if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        String safeMessage = message != null && !message.trim().isEmpty()
                ? message
                : getString(R.string.billing_site_payment_failed);
        showStatus(safeMessage, true);
        if (isAdded()) Toast.makeText(requireContext(), safeMessage, Toast.LENGTH_LONG).show();
    }

    private void showPaymentError(int statusCode, String message, int fallbackMessageRes) {
        showError(formatBillingApiError(statusCode, message, fallbackMessageRes));
    }

    private String formatBillingApiError(int statusCode, String message, int fallbackMessageRes) {
        String safeMessage = message != null && !message.trim().isEmpty()
                ? message
                : getString(fallbackMessageRes);
        return statusCode > 0
                ? getString(R.string.billing_error_with_http_status, safeMessage, statusCode)
                : safeMessage;
    }

    private void setPaymentLoading(boolean loading) {
        paymentInProgress = loading;
        if (binding == null) return;
        binding.paymentOverlay.setVisibility(loading ? VISIBLE : GONE);
        binding.spinnerService.setEnabled(!loading);
        binding.spinnerPeriod.setEnabled(!loading);
        binding.tvDiscountToggle.setEnabled(!loading);
        binding.discountRow.setEnabled(!loading);
        binding.etDiscountCode.setEnabled(!loading);
        binding.btnApplyDiscount.setEnabled(!loading);
        binding.btnClearDiscount.setEnabled(!loading);
        binding.checkboxRules.setEnabled(!loading);
        updatePayButtonState();
    }

    private void showStatus(String message, boolean error) {
        if (binding == null) return;
        if (message == null || message.trim().isEmpty()) {
            binding.tvStatus.setVisibility(GONE);
            binding.tvStatus.setText("");
            return;
        }
        binding.tvStatus.setVisibility(VISIBLE);
        binding.tvStatus.setText(message);
        binding.tvStatus.setTextColor(ContextCompat.getColor(
                requireContext(),
                error ? R.color.red : R.color.greenSecondaryTextColor
        ));
    }

    private void logSelectedPlanEvent(String eventName) {
        if (!isAdded()) return;
        TrackingUtils.logEvent(requireContext(), eventName, selectedPlanParams());
    }

    private android.os.Bundle selectedPlanParams() {
        android.os.Bundle params = new android.os.Bundle();
        BillingPlan plan = selectedItem != null ? selectedItem.getPlan() : pendingPlan;
        BillingStore store = BillingStore.current();
        TrackingUtils.put(params, TrackingUtils.PARAM_STORE, store.name().toLowerCase(Locale.US));
        if (plan != null) {
            TrackingUtils.put(params, TrackingUtils.PARAM_PLAN_SKU, plan.getSku());
            TrackingUtils.put(params, TrackingUtils.PARAM_PLAN_TITLE, plan.getTitle());
            TrackingUtils.put(params, TrackingUtils.PARAM_SERVICE_LEVEL, plan.getSla().getApiValue());
            TrackingUtils.put(params, TrackingUtils.PARAM_PERIOD, plan.getPeriod().getApiValue());
        }
        if (selectedItem != null && selectedItem.getPrice() != null) {
            TrackingUtils.put(params, TrackingUtils.PARAM_AMOUNT, selectedItem.getTotalPrice(store));
            TrackingUtils.put(params, TrackingUtils.PARAM_DISCOUNT_APPLIED, hasAppliedDiscount());
        }
        return params;
    }

    @Override
    public void onDestroyView() {
        BillingHost host = getBillingHost();
        if (host != null) host.setBillingPurchaseObserver(null);
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void checkStatus() {
    }
}
