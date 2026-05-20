package ir.shecan.core.billing;

import ir.shecan.data.modelDto.PriceViewModel;

public class BillingPlanPrice {
    private final BillingPlan plan;
    private PriceViewModel price;
    private boolean loading;
    private String errorMessage;
    private Long discountedPrice;
    private String discountCode;
    private String discountMessage;
    private boolean discountLoading;

    public BillingPlanPrice(BillingPlan plan) {
        this.plan = plan;
    }

    public BillingPlan getPlan() {
        return plan;
    }

    public PriceViewModel getPrice() {
        return price;
    }

    public void setPrice(PriceViewModel price) {
        this.price = price;
        this.errorMessage = null;
        this.discountedPrice = null;
        this.discountCode = null;
        this.discountMessage = null;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getDiscountedPrice() {
        return discountedPrice;
    }

    public String getDiscountCode() {
        return discountCode;
    }

    public String getDiscountMessage() {
        return discountMessage;
    }

    public boolean isDiscountLoading() {
        return discountLoading;
    }

    public void setDiscountLoading(boolean discountLoading) {
        this.discountLoading = discountLoading;
    }

    public void applyDiscount(String code, Long discountedPrice, String message) {
        this.discountCode = code;
        this.discountedPrice = discountedPrice;
        this.discountMessage = message;
    }

    public void clearDiscount(String message) {
        this.discountCode = null;
        this.discountedPrice = null;
        this.discountMessage = message;
    }

    public long getEffectivePrice() {
        if (discountedPrice != null) return discountedPrice;
        return price != null ? price.getSafePrice() : 0L;
    }

    public long getEffectivePrice(BillingStore store) {
        long basePrice = getEffectivePrice();
        if (store == BillingStore.CAFE_BAZAAR || store == BillingStore.MYKET) {
            return Math.round(basePrice * 1.0d);
        }
        return basePrice;
    }
}
