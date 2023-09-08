package cz.vitskalicky.lepsirozvrh.donations

import android.app.Activity
import androidx.compose.runtime.Composable
import cz.vitskalicky.lepsirozvrh.Utils

/** In flavor "play", diantions are turned on. This class is only like a inteface to other classes implementing the logic.*/
class Donations(private val activity: Activity, onPurchaseChangesListener: Utils.Listener?) {
    private val billing: Billing

    init {
        billing = Billing(activity)
        billing.addOnPurchaseChangeListener(onPurchaseChangesListener)
    }

    val isEnabled: Boolean
        get() = true
    val isSponsor: Boolean
        get() = billing.isSponsor

    fun restorePurchases() {
        billing.restorePurchases()
    }

    @Composable
    fun ShowDialog(onDismiss: ()-> Unit) {
        DonateDialog(billing, activity, onDismiss)
    }

    fun release() {
        billing.release()
    }
}
