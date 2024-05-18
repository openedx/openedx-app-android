package org.openedx.core.module.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.openedx.core.domain.ProductInfo
import org.openedx.core.extension.decodeToString
import org.openedx.core.extension.encodeToString
import org.openedx.core.extension.safeResume
import org.openedx.core.utils.Logger

class BillingProcessor(
    context: Context,
    private val dispatcher: CoroutineDispatcher,
) : PurchasesUpdatedListener {

    private val logger = Logger(TAG)

    private var listener: PurchaseListeners? = null

    private var billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            if (!purchases.first().isAcknowledged) {
                CoroutineScope(dispatcher).launch {
                    acknowledgePurchase(purchases.first())
                }
            } else {
                listener?.onPurchaseComplete(purchases.first())
            }
        } else {
            listener?.onPurchaseCancel(billingResult.responseCode, billingResult.debugMessage)
        }
    }

    fun setPurchaseListener(listener: PurchaseListeners) {
        this.listener = listener
    }

    private suspend fun isReadyOrConnect(): Boolean {
        return billingClient.isReady || connect()
    }

    private suspend fun connect(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val billingClientStateListener = object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    logger.d { "BillingSetupFinished -> $billingResult" }
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        continuation.safeResume(true) {
                            continuation.cancel()
                        }
                    } else {
                        continuation.safeResume(false) {
                            continuation.cancel()
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    continuation.safeResume(false) {
                        continuation.cancel()
                    }
                }
            }
            billingClient.startConnection(billingClientStateListener)
        }
    }

    suspend fun querySyncDetails(productId: String): ProductDetailsResult {
        isReadyOrConnect()
        val productDetails = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        return withContext(dispatcher) {
            billingClient.queryProductDetails(
                QueryProductDetailsParams
                    .newBuilder()
                    .setProductList(listOf(productDetails))
                    .build()
            )
        }
    }

    suspend fun purchaseItem(
        activity: Activity,
        userId: Long,
        productInfo: ProductInfo,
    ) {
        if (isReadyOrConnect()) {
            val response = querySyncDetails(productInfo.storeSku)

            response.productDetailsList?.first()?.let {
                launchBillingFlow(activity, it, userId, productInfo.courseSku)
            }
        } else {
            listener?.onPurchaseCancel(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE, "")
        }
    }

    private fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        userId: Long,
        courseSku: String,
    ) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .setObfuscatedAccountId(userId.encodeToString())
            .setObfuscatedProfileId(courseSku.encodeToString())
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    private suspend fun acknowledgePurchase(purchase: Purchase) {
        isReadyOrConnect()
        val billingResult = billingClient.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        )
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            listener?.onPurchaseComplete(purchase)
        }
    }

    suspend fun consumePurchase(purchaseToken: String): BillingResult {
        isReadyOrConnect()
        val result = billingClient.consumePurchase(
            ConsumeParams
                .newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()
        )
        return result.billingResult
    }

    fun release() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    companion object {
        private const val TAG = "BillingClientWrapper"
        const val MICROS_TO_UNIT = 1_000_000 // 1,000,000 micro-units equal one unit of the currency
    }

    interface PurchaseListeners {
        fun onPurchaseComplete(purchase: Purchase)
        fun onPurchaseCancel(responseCode: Int, message: String)
    }
}

fun ProductDetails.OneTimePurchaseOfferDetails.getPriceAmount(): Double =
    this.priceAmountMicros.toDouble().div(BillingProcessor.MICROS_TO_UNIT)

fun Purchase.getCourseSku(): String? {
    return this.accountIdentifiers?.obfuscatedProfileId?.decodeToString()
}
