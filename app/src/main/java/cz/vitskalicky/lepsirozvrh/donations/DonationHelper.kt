package cz.vitskalicky.lepsirozvrh.donations

import android.app.Activity
import androidx.lifecycle.MutableLiveData

/** A Utility class to avoid repeating code. Helps you manage the LiveData, since the Donations API is slightly stupid.
 * **Don't forget to call [onCreate] int your activity's onCreate and [release] in onDestroy**  */
class DonationHelper(val activity: Activity){
    var donations: Donations? = null
        private set
    val donationsEnabledLD = MutableLiveData<Boolean>(false);
    val isSponsorLD = MutableLiveData<Boolean>(false);
    fun onCreate(){
        donations = Donations(activity, onPurchaseChangesListener = {
            donationsEnabledLD.value = donations?.isEnabled;
            isSponsorLD.value = donations?.isSponsor;
        })
        donationsEnabledLD.value = donations?.isEnabled;
        isSponsorLD.value = donations?.isSponsor;
    }

    fun release(){
        donations?.release()
    }
}