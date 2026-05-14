package ir.shecan.core.billing

object CafeBazaarBillingProducts {

    // Replace these ids with the exact product ids created in the Cafe Bazaar developer panel.
    const val SKU_SERVICE_MONTHLY = ""
    const val SKU_SERVICE_YEARLY = ""

    @JvmStatic
    fun consumableSkus(): List<String> = buildSkuList(SKU_SERVICE_MONTHLY, SKU_SERVICE_YEARLY)

    @JvmStatic
    fun subscriptionSkus(): List<String> = emptyList()

    @JvmStatic
    fun allSkus(): List<String> = consumableSkus() + subscriptionSkus()

    @JvmStatic
    fun hasAnySku(): Boolean = allSkus().isNotEmpty()

    private fun buildSkuList(vararg skus: String): List<String> {
        return skus.map { it.trim() }.filter { it.isNotEmpty() }
    }
}
