package ir.shecan.core.billing

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Log
import androidx.activity.result.ActivityResultRegistry
import ir.cafebazaar.poolakey.Connection
import ir.cafebazaar.poolakey.Payment
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.entity.PurchaseInfo
import ir.cafebazaar.poolakey.request.PurchaseRequest
import ir.shecan.BuildConfig
import ir.shecan.core.constant.Constant
import java.security.SecureRandom
import java.util.UUID

class CafeBazaarBillingManager(context: Context) {

    private val appContext = context.applicationContext
    private val preferences: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()
    private val consumableSkus = mutableSetOf<String>()
    private val nonConsumableSkus = mutableSetOf<String>()

    private var payment: Payment? = null
    private var connection: Connection? = null
    private var listener: Listener? = null
    private var ready = false

    fun startSetup(
        consumables: List<String>?,
        nonConsumables: List<String>?,
        listener: Listener?
    ) {
        this.listener = listener
        consumableSkus.clear()
        nonConsumableSkus.clear()
        consumables?.filter { it.isNotBlank() }?.mapTo(consumableSkus) { it.trim() }
        nonConsumables?.filter { it.isNotBlank() }?.mapTo(nonConsumableSkus) { it.trim() }

        if (!Constant.IsCafeBazaarMode) {
            notifyUnavailable("Cafe Bazaar billing is available only in the cafeBazaar flavor.")
            return
        }

        if (BuildConfig.IAB_PUBLIC_KEY.isBlank()) {
            notifyUnavailable("BAZAAR_IAB_PUBLIC_KEY is empty.")
            return
        }

        dispose()

        val securityCheck = SecurityCheck.Enable(rsaPublicKey = BuildConfig.IAB_PUBLIC_KEY)
        val configuration = PaymentConfiguration(localSecurityCheck = securityCheck)
        payment = Payment(context = appContext, config = configuration)
        connection = payment?.connect {
            connectionSucceed {
                ready = true
                this@CafeBazaarBillingManager.listener?.onBillingReady()
                queryInventory()
            }
            connectionFailed { throwable ->
                ready = false
                notifyUnavailable("Problem connecting to Cafe Bazaar billing: ${throwable.message}")
            }
            disconnected {
                ready = false
                this@CafeBazaarBillingManager.listener?.onBillingDisconnected()
            }
        }
    }

    fun isReady(): Boolean = ready && payment != null && connection != null

    fun queryInventory() {
        if (!isReady()) {
            notifyError("Cafe Bazaar billing is not ready.")
            return
        }

        querySkuDetails()
        queryPurchasedProducts()
    }

    fun launchPurchaseFlow(registry: ActivityResultRegistry, sku: String) {
        val currentPayment = payment
        if (!isReady() || currentPayment == null) {
            notifyError("Cafe Bazaar billing is not ready.")
            return
        }

        if (sku.isBlank()) {
            notifyError("Invalid Cafe Bazaar purchase request.")
            return
        }

        val payload = createDeveloperPayload(sku)
        savePendingPayload(sku, payload)
        val request = PurchaseRequest(productId = sku, payload = payload)

        currentPayment.purchaseProduct(registry = registry, request = request) {
            purchaseFlowBegan {
                listener?.onPurchaseFlowBegan(sku)
            }
            failedToBeginFlow { throwable ->
                notifyError("Failed to begin Cafe Bazaar purchase flow: ${throwable.message}")
            }
            purchaseSucceed { purchaseInfo ->
                handleVerifiedPurchase(purchaseInfo, restoredFromInventory = false)
            }
            purchaseCanceled {
                listener?.onPurchaseCanceled(sku)
            }
            purchaseFailed { throwable ->
                notifyError("Cafe Bazaar purchase failed: ${throwable.message}")
            }
        }
    }

    fun consumePurchase(purchaseToken: String) {
        val currentPayment = payment
        if (!isReady() || currentPayment == null) {
            notifyError("Cafe Bazaar billing is not ready.")
            return
        }

        if (purchaseToken.isBlank()) {
            notifyError("Purchase token is empty.")
            return
        }

        currentPayment.consumeProduct(purchaseToken) {
            consumeSucceed {
                listener?.onPurchaseConsumed(purchaseToken)
            }
            consumeFailed { throwable ->
                notifyError("Error consuming Cafe Bazaar purchase: ${throwable.message}")
            }
        }
    }

    fun dispose() {
        ready = false
        try {
            connection?.disconnect()
        } catch (e: RuntimeException) {
            Log.w(TAG, "Error disconnecting Cafe Bazaar billing", e)
        }
        connection = null
        payment = null
    }

    private fun querySkuDetails() {
        val currentPayment = payment ?: return
        val inAppSkus = (consumableSkus + nonConsumableSkus).toList()

        if (inAppSkus.isNotEmpty()) {
            currentPayment.getInAppSkuDetails(inAppSkus) {
                getSkuDetailsSucceed { skuDetails ->
                    listener?.onInAppSkuDetailsLoaded(skuDetails)
                }
                getSkuDetailsFailed { throwable ->
                    notifyError("Failed to load Cafe Bazaar in-app sku details: ${throwable.message}")
                }
            }
        }
    }

    private fun queryPurchasedProducts() {
        val currentPayment = payment ?: return
        currentPayment.getPurchasedProducts {
            querySucceed { purchases ->
                purchases.forEach { handleVerifiedPurchase(it, restoredFromInventory = true) }
            }
            queryFailed { throwable ->
                notifyError("Failed to query Cafe Bazaar purchases: ${throwable.message}")
            }
        }
    }

    private fun handleVerifiedPurchase(purchaseInfo: PurchaseInfo, restoredFromInventory: Boolean) {
        if (!verifyDeveloperPayload(purchaseInfo)) {
            notifyError("Cafe Bazaar developer payload verification failed for sku: ${purchaseInfo.productId}")
            return
        }

        clearPendingPayload(purchaseInfo.productId)

        when {
            nonConsumableSkus.contains(purchaseInfo.productId) ->
                listener?.onNonConsumablePurchaseReady(purchaseInfo, restoredFromInventory)
            else ->
                listener?.onConsumablePurchaseReady(purchaseInfo, restoredFromInventory)
        }
    }

    private fun verifyDeveloperPayload(purchaseInfo: PurchaseInfo): Boolean {
        val expectedPayload = preferences.getString(payloadKey(purchaseInfo.productId), null)
        return if (expectedPayload == null) {
            !TextUtils.isEmpty(purchaseInfo.payload)
        } else {
            expectedPayload == purchaseInfo.payload
        }
    }

    private fun createDeveloperPayload(sku: String): String {
        return "${appContext.packageName}:$sku:${System.currentTimeMillis()}:" +
            "${secureRandom.nextInt(Int.MAX_VALUE)}:${UUID.randomUUID()}"
    }

    private fun savePendingPayload(sku: String, payload: String) {
        preferences.edit().putString(payloadKey(sku), payload).apply()
    }

    private fun clearPendingPayload(sku: String) {
        preferences.edit().remove(payloadKey(sku)).apply()
    }

    private fun payloadKey(sku: String): String = PAYLOAD_PREFIX + sku

    private fun notifyUnavailable(message: String) {
        Log.w(TAG, message)
        listener?.onBillingUnavailable(message)
    }

    private fun notifyError(message: String) {
        Log.e(TAG, message)
        listener?.onBillingError(message)
    }

    interface Listener {
        fun onBillingReady()

        fun onBillingUnavailable(message: String)

        fun onBillingDisconnected()

        fun onInAppSkuDetailsLoaded(skuDetails: List<*>)

        fun onPurchaseFlowBegan(sku: String)

        fun onConsumablePurchaseReady(purchaseInfo: Any, restoredFromInventory: Boolean)

        fun onNonConsumablePurchaseReady(purchaseInfo: Any, restoredFromInventory: Boolean)

        fun onPurchaseCanceled(sku: String)

        fun onPurchaseConsumed(purchaseToken: String)

        fun onBillingError(message: String)
    }

    companion object {
        private const val TAG = "CafeBazaarBilling"
        private const val PREFS_NAME = "cafe_bazaar_billing"
        private const val PAYLOAD_PREFIX = "payload_"
    }
}
