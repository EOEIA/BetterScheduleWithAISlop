package cz.vitskalicky.lepsirozvrh.donations

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cz.vitskalicky.lepsirozvrh.R
/** see [DonateDialog]*/
 @Composable
private fun DonateDialogStateless(smallDonated: Boolean, bigDonated: Boolean, smallPrice: String?, bigPrice: String?, isInitialized: Boolean, onDonateSmall: () -> Unit, onDonateBig: () -> Unit, onUseCode: () -> Unit, onDismissed: ()->Unit ){
    val isSponsor = smallDonated || bigDonated
    Dialog(
        onDismissRequest = onDismissed,
        content = {
            Surface(
                color = MaterialTheme.colors.surface,
                elevation = 16.dp
            ) {
                Column {
                    Surface(
                        color = if (isSponsor) Color.Green /*todo choose better color*/ else MaterialTheme.colors.primarySurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(Icons.Filled.MonetizationOn, null, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.size(16.dp))
                            Text(
                                stringResource(if (isSponsor) R.string.donate_title_ok else R.string.donate_title),
                                style = MaterialTheme.typography.h5
                            )
                        }
                    }
                    Column(
                        Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                    ) {
                        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                            Text(
                                stringResource(if (isSponsor) R.string.donate_text1_ok else R.string.donate_text1),
                            )
                        }
                        Text(
                            stringResource(R.string.donate_bonus_features)
                        )

                        Spacer(Modifier.size(24.dp))
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(onDonateSmall, enabled = !smallDonated && isInitialized) {
                                if (smallPrice == null) {
                                    Text(stringResource(R.string.donate_a_little_no_price))
                                } else {
                                    Text(stringResource(R.string.donate_a_little, smallPrice))
                                }
                            }
                            Spacer(Modifier.size(8.dp))
                            Button(onDonateBig, enabled = !bigDonated && isInitialized) {
                                if (bigPrice == null) {
                                    Text(stringResource(R.string.donate_more_no_price))
                                } else {
                                    Text(stringResource(R.string.donate_more, bigPrice))
                                }
                            }
                        }
                        Spacer(Modifier.size(8.dp))
                        Text(
                            stringResource(R.string.donate_text2),
                            style = MaterialTheme.typography.caption
                        )
                        Spacer(Modifier.size(8.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onUseCode) { Text(stringResource(R.string.use_promo_code)) }
                            TextButton(onDismissed) { Text(stringResource(R.string.close)) }
                        }
                    }
                }
            }
        }
    )
}

/** UI for the dialog with donate buttons*/
@Composable
fun DonateDialog(billing: Billing, activity: Activity, onDismissed: () -> Unit){
    var smallDonated: Boolean = false
    var bigDonated: Boolean = false
    var smallPrice: String? = null
    var bigPrice: String? = null
    var isInitialized: Boolean = false
    if (billing.isInitialized){
        isInitialized = true
        smallDonated = billing.isSmallPurchased
        bigDonated = billing.isBigPurchased
        smallPrice = billing.smallDetails?.price
        bigPrice = billing.bigDetails?.price
    }

    val useCode = {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/redeem?code="))
        activity.startActivity(browserIntent)
    }
    DonateDialogStateless(
        smallDonated, bigDonated, smallPrice, bigPrice, isInitialized,
        {billing.buySmall(activity)}, {billing.buyBig(activity)}, useCode, onDismissed
    )
}

@Preview
@Composable
private fun Preview1(){
    DonateDialogStateless(false, false, "30Kč", null, true, {}, {} ,{}, {})
}