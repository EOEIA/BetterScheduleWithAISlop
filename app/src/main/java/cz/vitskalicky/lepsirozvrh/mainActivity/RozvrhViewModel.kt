package cz.vitskalicky.lepsirozvrh.mainActivity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import cz.vitskalicky.lepsirozvrh.model.Account
import cz.vitskalicky.lepsirozvrh.model.RozvrhRecord
import cz.vitskalicky.lepsirozvrh.model.StatusInfo

/** Interface for [RozvrhWithControls] so that it is not tied to MainActivity*/
interface RozvrhViewModel {
    fun getAccountIdLD(): LiveData<Long?>
    fun getAccountLD(): LiveData<Account?>
    fun getDisplayLD(): LiveData<RozvrhRecord?>
    fun getStatusLD(): LiveData<StatusInfo>
    fun getIsOfflineLD(): LiveData<Boolean>

    fun getShowSettingsBadgeLD(): LiveData<Boolean>

    var weekPosition: Int
    val showError: Boolean
    fun forceRefresh()

    /** Keeps track of whether to jump to current lesson as soon as possible. After successful jump, it is set to false again. */
    var centerToCurrentLessonLD: MutableLiveData<Boolean?>

    companion object{
        const val PERM: Int = Int.MIN_VALUE
    }
}