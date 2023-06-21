package cz.vitskalicky.lepsirozvrh.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.joda.time.LocalDate

class RozvrhStatusStore {
    private val map = HashMap<RozvrhRecord.Key, StatusInfo>()
    private val liveDatas = HashMap<RozvrhRecord.Key, MutableLiveData<StatusInfo>>()
    val isOffline = MutableLiveData<Boolean>(false)

    fun getLiveData(key: RozvrhRecord.Key): LiveData<StatusInfo> = liveDatas.getOrPut(key) {
        val ld = MutableLiveData<StatusInfo>()
        ld.value = get(key)
        return@getOrPut ld
    }

    operator fun get(key:RozvrhRecord.Key): StatusInfo = map[key] ?: StatusInfo.unknown()

    operator fun set(key: RozvrhRecord.Key, value: StatusInfo){
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