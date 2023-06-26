package cz.vitskalicky.lepsirozvrh.model

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fasterxml.jackson.databind.JsonMappingException
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.Utils
import cz.vitskalicky.lepsirozvrh.bakaAPI.rozvrh.RozvrhWebservice.Companion.getSchedule
import cz.vitskalicky.lepsirozvrh.bakaAPI.rozvrh.rozvrh3.Rozvrh3
import cz.vitskalicky.lepsirozvrh.bakaAPI.rozvrh.rozvrh3.RozvrhConverter
import cz.vitskalicky.lepsirozvrh.database.RozvrhDatabase
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh
import kotlinx.coroutines.*
import org.joda.time.DateTime
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime
import retrofit2.HttpException
import java.io.IOException
import kotlin.random.Random
import cz.vitskalicky.lepsirozvrh.model.RozvrhRecord.Key

class RozvrhRepository(context: Context, scope: CoroutineScope? = null) {
    private val application: MainApplication = context.applicationContext as MainApplication
    private val db: RozvrhDatabase = application.rozvrhDb
    private val statusStr: RozvrhStatusStore = application.rozvrhStatusStore
    private val scope: CoroutineScope = scope ?: application.mainScope

    /** LiveData of the current week for each account. Useful for notification and widgets */
    private val currentWeekLD: HashMap<Int,MutableLiveData<Rozvrh?>> = HashMap()

    private fun getCurrentWeekMutLD(account: Int): MutableLiveData<Rozvrh?>{
        val ld = currentWeekLD.getOrPut(account){MutableLiveData()}
        if (ld.value == null){
            scope.launch {
                ld.value = getRozvrh(Key(account, Utils.getDisplayWeekMonday(application)), foreground = false)
            }
        }
        return ld
    }
    fun getCurrentWeekLD(account: Int): LiveData<Rozvrh?> = getCurrentWeekMutLD(account)

    fun getRozvrhLive(rozvrhId: Key, foreground: Boolean): LiveData<RozvrhRecord/*todo do we want to return rozvrh or rozvrhRecord?*/> {

        refresh(rozvrhId, foreground, false)
        return db.rozvrhDao().loadRozvrhLive(rozvrhId)
    }

    fun refresh(rozvrhId: Key, foreground: Boolean, force: Boolean = false, invalidateOthersIfSuccessful: Boolean = false){
        scope.launch(){
            if (force || refreshNeeded(rozvrhId, foreground)){
                val success = fetchAndCache(rozvrhId) != null
                if (success && invalidateOthersIfSuccessful){
                    withContext(Dispatchers.IO){
                        db.rozvrhDao().invalidateAllOther(rozvrhId)
                    }
                }
            }
        }
    }

    /**
     * Loads the "freshest" rozvrh available and returns it.
     */
    suspend fun getRozvrh(key: Key, foreground: Boolean): Rozvrh?{
        val result: Rozvrh? = if (refreshNeeded(key, foreground)){
            fetchAndCache(/*todo*/key, foreground) //is null on error
        } else null
        return result ?:
            //if refresh not needed or refresh failed
            db.rozvrhDao().loadRozvrh(key)?.data
    }

    /**
     * lads the cached rozvrh or `null` if not available. Disadvantage: it may not be very fresh; usually [getRozvrh] is better.
     */
    suspend fun getCachedRozvrh(rozvrhId: Key): Rozvrh?{
        return db.rozvrhDao().loadRozvrh(rozvrhId)?.data
    }

    fun getRozvrhStatusLiveData(rozvrhId: Key): LiveData<StatusInfo>{
        return statusStr.getLiveData(rozvrhId)
    }

    fun getOfflineStatusLiveData(): LiveData<Boolean>{
        return statusStr.isOffline
    }

    /**
     * Returns the time when data on widget and in notification should be updated. `null` means, that it could not be determined and should be checked again later.
     */
    suspend fun getUpdateDisplayedDataTime():LocalDateTime?{//todo

        val current: Rozvrh? = getRozvrh(Utils.getCurrentMonday(), false)
        var time: LocalDateTime?
        if (current == null){
            return null
        }else{
            time = current.getUpdateDisplayedDataTime()
        }

        if (time == null){
            val next = getRozvrh(Utils.getCurrentMonday().plusWeeks(1), false)
            if (next == null){
                return null
            }else{
                time = next.getUpdateDisplayedDataTime() ?: Utils.getCurrentMonday().plusDays(13).toLocalDateTime(LocalTime.MIDNIGHT)
            }
        }
        return time
    }

    suspend fun refreshNeeded(rozvrhId: Key, foreground: Boolean = true): Boolean{
        if (statusStr[rozvrhId].status == StatusInfo.Status.ERROR && foreground){ // when from background don't bother refreshing failed requests unless they are expired.
            return true
        }
        if (statusStr[rozvrhId].status == StatusInfo.Status.LOADING){
            return false
        }
        val expireTime = if (foreground){
            //if refreshing for foreground (e.g. MainActivity), we want a very fresh schedule to have more consistent data
            DateTime.now().minusMinutes(10)
        }else{
            // if it is only for widget or notification, we don't want to drain battery
            DateTime.now().minusHours(3)
        }
        return db.rozvrhDao().isExpired(rozvrhId,expireTime) != false
    }

    /**
     * Fetches from network and saves to database. Returns null on error.
     *
     * If [foreground] if false, a stricter timeout will be set to avoid ANR (application not responding) when running in
     * the background. Also resets refresh timeout if refresh fails in the background.
     *
     */
    private suspend fun fetchAndCache(rozvrhId: Key, foreground: Boolean = true): Rozvrh? {
        val deferred: Deferred<Rozvrh?> = scope.async {
            withContext(Dispatchers.Main) {
                statusStr[rozvrhId] = StatusInfo.loading()
            }
            try {
                val rozvrh3: Rozvrh3 = try {
                    //check for demo mode
                    if (application.debugUtils.isDemoMode) {
                        //simulate slow net
                        delay(Random.nextLong(3000))
                        //return demo rozvrh
                        application.debugUtils.getDemoRozvrh3(rozvrhId)/*todo*/
                    } else {
                        withTimeout(if (foreground) 15000 else 7000) { //use 7 second timeout if from background to avoid ANR
                            //download new from server
                            application.webservice?.getSchedule(rozvrhId) /*todo*/
                                ?: throw IOException("Webservice not ready")
                        }
                    }
                } catch (e: Exception) {
                    when (e) {
                        is HttpException -> if (e.code() == 401) //unauthorized
                            throw LoginRequiredException()
                        else
                            throw e
                        is TimeoutCancellationException, is IOException -> {
                            //transform timeoutCancell..tion to IOException
                            val newe =
                                if (e is TimeoutCancellationException) IOException("Request for rozvrh id '$rozvrhId' timed out") else e
                            if (!foreground) {
                                //if from background, reset refresh timeout to avoid battery drain
                                db.rozvrhDao().resetExpiration(rozvrhId)
                            }
                            throw newe
                        }
                        else -> throw e
                    }
                }

                val rozvrh: Rozvrh = withContext(Dispatchers.IO) {
                    RozvrhConverter.convert(//todo
                        rozvrh3,
                        rozvrhId,
                        application
                    )
                }
                db.rozvrhDao().insertRozvrh(RozvrhRecord(
                    rozvrhId,
                    DateTime.now(),
                    rozvrh
                ))
                if (rozvrh.monday == Utils.getCurrentMonday()) {
                    withContext(Dispatchers.Main) {
                        getCurrentWeekMutLD(rozvrhId.account).value = rozvrh
                    }
                }
                withContext(Dispatchers.Main) {
                    statusStr.isOffline.value = false
                    statusStr[rozvrhId] = StatusInfo.success()
                }
                return@async rozvrh
            } catch (e: Exception) {
                withContext(NonCancellable) {
                    withContext(Dispatchers.Main) {
                        reportError(e, rozvrhId)//todo
                    }
                }
                if (e is CancellationException) throw e
                return@async null
            }
        }
        //ensure for the third time that status is not loading after job completed
        deferred.invokeOnCompletion {cause ->
            scope.launch(NonCancellable) {
                withContext(Dispatchers.Main) {
                    if (statusStr[rozvrhId].status == StatusInfo.Status.LOADING) {
                        application.sendReport(IllegalStateException("Status was still LOADING after job completed", cause))
                        statusStr[rozvrhId] = StatusInfo.Rozvrh.appError()
                    }
                }
            }
        }
        return deferred.await()
    }

    /**
     * must run on UI thread and **make sure a coroutine is not cancelled too soon**
     * When switching to UI thread use:
     * ```
withContext(NonCancellable) {
    withContext(Dispatchers.Main) {
        reportError(e, rozvrhId)
    }
}
    ```
     * to make sure the coroutine is not cancelled on `withContext(Dispatchers.Main)`, which could
     * leave [RozvrhStatusStore] stuck on loading state and it would never refresh until full app
     * restart.
     */
    private fun reportError(e: Exception, rozvrhId: Key) {
        statusStr.isOffline.value = true
        statusStr[rozvrhId] = when (e) {
            is JsonMappingException ->{
                //parsing error
                //report
                application.sendReport(e)
                StatusInfo.Rozvrh.unexpectedResponse()
            }
            is IOException -> {
                //network error
                StatusInfo.Rozvrh.unreachable()
            }
            is RozvrhConverter.RozvrhConversionException -> {
                //conversion failed
                application.sendReport(e)
                StatusInfo.Rozvrh.unexpectedResponse()
            }
            is LoginRequiredException -> {
                application.accountRepository.logout()
                StatusInfo.Rozvrh.loginFailed()
            }
            is HttpException -> {
                StatusInfo.Rozvrh.unexpectedResponse()
            }
            else -> {
                statusStr[rozvrhId] = StatusInfo.Rozvrh.unexpectedResponse()
                application.sendReport(e)
                throw e
            }
        }
    }

}
