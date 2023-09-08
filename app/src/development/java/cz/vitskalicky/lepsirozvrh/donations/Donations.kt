package cz.vitskalicky.lepsirozvrh.donations

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import cz.vitskalicky.lepsirozvrh.BuildConfig
import cz.vitskalicky.lepsirozvrh.Utils

/** In flavor "development", diantions are turned off*/
class Donations(
    private val activity: Activity,
    onPurchaseChangesListener: Utils.Listener?
) {
    val isEnabled: Boolean
        get() = false
    val isSponsor: Boolean
        get() = true

    fun restorePurchases() {
        Toast.makeText(
            activity,
            "Error (in-app purchases not enabled in " + BuildConfig.FLAVOR + " flavour)",
            Toast.LENGTH_SHORT
        ).show()
    }

    @Composable
    fun ShowDialog(onDismiss: ()-> Unit) {
        Log.e(Donations::class.qualifiedName, "Error (no donate dialog in " + BuildConfig.FLAVOR + " flavour)")
    }

    fun release() {}
}
