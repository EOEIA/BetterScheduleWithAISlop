package cz.vitskalicky.lepsirozvrh.fragment

import androidx.lifecycle.LiveData
import cz.vitskalicky.lepsirozvrh.model.Account
import cz.vitskalicky.lepsirozvrh.model.RozvrhRecord
import cz.vitskalicky.lepsirozvrh.model.StatusInfo
import cz.vitskalicky.lepsirozvrh.compose.RozvrhWithControlsStateless

/** Interface for [RozvrhWithControls] so that it is not tied to MainActivity*/
interface RozvrhViewModel {
    fun getAccountIdLD(): LiveData<Long?>
    fun getAccountLD(): LiveData<Account?>
    fun getDisplayLD(): LiveData<RozvrhRecord?>
    fun getStatusLD(): LiveData<StatusInfo>
    fun getIsOfflineLD(): LiveData<Boolean>

    var weekPosition: Int
    val showError: Boolean
    fun forceRefresh()

    companion object{
        const val PERM: Int = Int.MIN_VALUE
    }
}