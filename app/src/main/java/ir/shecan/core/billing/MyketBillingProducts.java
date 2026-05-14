package ir.shecan.core.billing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MyketBillingProducts {

    // Replace these ids with the exact product ids created in the Myket developer panel.
    public static final String SKU_SERVICE_MONTHLY = "";
    public static final String SKU_SERVICE_YEARLY = "";

    private MyketBillingProducts() {
    }

    public static List<String> consumableSkus() {
        return buildSkuList(SKU_SERVICE_MONTHLY, SKU_SERVICE_YEARLY);
    }

    public static List<String> nonConsumableSkus() {
        return Collections.emptyList();
    }

    public static List<String> allSkus() {
        List<String> allSkus = new ArrayList<>();
        allSkus.addAll(consumableSkus());
        allSkus.addAll(nonConsumableSkus());
        return allSkus;
    }

    public static boolean hasAnySku() {
        return !allSkus().isEmpty();
    }

    private static List<String> buildSkuList(String... skus) {
        List<String> result = new ArrayList<>();
        for (String sku : skus) {
            if (sku != null && !sku.trim().isEmpty()) {
                result.add(sku.trim());
            }
        }
        return result;
    }
}
