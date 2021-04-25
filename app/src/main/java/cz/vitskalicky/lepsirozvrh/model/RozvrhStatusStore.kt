package cz.vitskalicky.lepsirozvrh.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.joda.time.LocalDate

class RozvrhStatusStore {
    private val map = HashMap<LocalDate, StatusInfo>()
    private val liveDatas = HashMap<LocalDate, MutableLiveData<StatusInfo>>()
    val isOffline = MutableLiveData<Boolean>(false)

    fun getLiveData(key: LocalDate): LiveData<StatusInfo> = liveDatas.getOrPut(key) {
        val ld = MutableLiveData<StatusInfo>()
        ld.value = get(key)
        return@getOrPut ld
    }

    operator fun get(key:LocalDate): StatusInfo = map[key] ?: StatusInfo.unknown()

    operator fun set(key: LocalDate, value: StatusInfo){
        map[key] = value
        liveDatas[key]?.value = value
    }

    fun clear(){
        map.clear()
        liveDatas.forEach{
            it.value.value = StatusInfo.unknown()
        }
    }
}