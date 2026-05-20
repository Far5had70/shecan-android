package ir.shecan.core.billing;

import java.util.Collections;
import java.util.List;

public final class MyketBillingProducts {

    private MyketBillingProducts() {
    }

    public static List<String> consumableSkus() {
        return Collections.emptyList();
    }

    public static List<String> nonConsumableSkus() {
        return BillingPlanCatalog.purchasableSkus();
    }

    public static List<String> allSkus() {
        return BillingPlanCatalog.purchasableSkus();
    }

    public static boolean hasAnySku() {
        return !allSkus().isEmpty();
    }
}
