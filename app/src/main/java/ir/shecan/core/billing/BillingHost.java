package ir.shecan.core.billing;

public interface BillingHost {
    void setBillingPurchaseObserver(BillingPurchaseObserver billingPurchaseObserver);

    boolean isBillingReadyForStore(BillingStore store);

    void launchMyketPurchase(String sku, Long renewalOrderId);

    void launchCafeBazaarPurchase(String sku, Long renewalOrderId);
}
