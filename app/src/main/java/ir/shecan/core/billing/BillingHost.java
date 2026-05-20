package ir.shecan.core.billing;

public interface BillingHost {
    void setBillingPurchaseObserver(BillingPurchaseObserver billingPurchaseObserver);

    boolean isBillingReadyForStore(BillingStore store);

    void launchMyketPurchase(String sku);

    void launchCafeBazaarPurchase(String sku);
}
