package com.photocompressor.kbsize

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * BillingManager handles all Google Play Billing operations.
 * Uses Billing Library v6+ with proper acknowledgement and restore logic.
 * 
 * Product ID: pro_upgrade (one-time non-consumable purchase)
 */
class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"
        private const val PRODUCT_ID = "pro_upgrade"
        private const val PREFS_NAME = "billing_prefs"
        private const val KEY_IS_PRO = "is_pro"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var billingClient: BillingClient? = null
    private var productDetails: ProductDetails? = null

    // StateFlows for UI observation
    private val _isPro = MutableStateFlow(prefs.getBoolean(KEY_IS_PRO, false))
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _productPrice = MutableStateFlow<String?>(null)
    val productPrice: StateFlow<String?> = _productPrice.asStateFlow()

    private val _billingReady = MutableStateFlow(false)
    val billingReady: StateFlow<Boolean> = _billingReady.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /**
     * Initialize the billing client and connect.
     */
    fun initialize(appContext: Context) {
        billingClient = BillingClient.newBuilder(appContext)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        connectToBilling()
    }

    private fun connectToBilling() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        Log.d(TAG, "Billing setup successful")
                        _billingReady.value = true
                        _lastError.value = null
                        refreshProductDetails()
                        restorePurchases()
                    }
                    else -> {
                        Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                        _billingReady.value = false
                        _lastError.value = "Billing unavailable: ${billingResult.debugMessage}"
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected. Retrying...")
                _billingReady.value = false
                // Retry connection
                connectToBilling()
            }
        })
    }

    /**
     * Query product details from Play Console.
     */
    fun refreshProductDetails() {
        if (billingClient?.isReady != true) {
            Log.w(TAG, "Billing client not ready")
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    if (productDetailsList.isNotEmpty()) {
                        productDetails = productDetailsList[0]
                        val price = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice
                        _productPrice.value = price
                        Log.d(TAG, "Product details loaded: $PRODUCT_ID - $price")
                    } else {
                        Log.w(TAG, "No product details found for $PRODUCT_ID")
                        _lastError.value = "Product not found. Create '$PRODUCT_ID' in Play Console."
                    }
                }
                else -> {
                    Log.e(TAG, "Failed to query product details: ${billingResult.debugMessage}")
                    _lastError.value = "Failed to load product: ${billingResult.debugMessage}"
                }
            }
        }
    }

    /**
     * Launch purchase flow for Pro upgrade.
     */
    fun purchasePro(activity: Activity) {
        if (billingClient?.isReady != true) {
            _lastError.value = "Billing not ready"
            return
        }

        val details = productDetails
        if (details == null) {
            _lastError.value = "Product not available"
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
        
        if (billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
            _lastError.value = "Failed to launch purchase: ${billingResult?.debugMessage}"
        }
    }

    /**
     * Restore purchases from Play Store.
     */
    fun restorePurchases() {
        if (billingClient?.isReady != true) {
            Log.w(TAG, "Billing client not ready for restore")
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    Log.d(TAG, "Restore purchases: found ${purchases.size} purchases")
                    
                    val proPurchase = purchases.find { it.products.contains(PRODUCT_ID) }
                    
                    if (proPurchase != null && proPurchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        handlePurchase(proPurchase)
                    } else {
                        Log.d(TAG, "No Pro purchase found")
                        // Don't reset Pro status here - user might be offline
                    }
                }
                else -> {
                    Log.e(TAG, "Failed to restore purchases: ${billingResult.debugMessage}")
                    _lastError.value = "Restore failed: ${billingResult.debugMessage}"
                }
            }
        }
    }

    /**
     * Called when purchases are updated (new purchase or restored).
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled purchase")
                _lastError.value = null // Not an error
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Item already owned, restoring...")
                restorePurchases()
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
                _lastError.value = "Purchase failed: ${billingResult.debugMessage}"
            }
        }
    }

    /**
     * Handle a purchase: acknowledge if needed and unlock Pro.
     */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.products.contains(PRODUCT_ID) && 
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            } else {
                // Already acknowledged, unlock immediately
                unlockPro()
            }
        }
    }

    /**
     * Acknowledge the purchase.
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(params) { billingResult ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    Log.d(TAG, "Purchase acknowledged successfully")
                    unlockPro()
                }
                else -> {
                    Log.e(TAG, "Failed to acknowledge: ${billingResult.debugMessage}")
                    _lastError.value = "Acknowledge failed: ${billingResult.debugMessage}"
                }
            }
        }
    }

    /**
     * Unlock Pro status and persist to SharedPreferences.
     */
    private fun unlockPro() {
        prefs.edit().putBoolean(KEY_IS_PRO, true).apply()
        _isPro.value = true
        _lastError.value = null
        Log.d(TAG, "Pro unlocked!")
    }

    /**
     * Clean up billing client.
     */
    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
    }
}