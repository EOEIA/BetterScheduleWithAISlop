package cz.vitskalicky.lepsirozvrh.fragment

import android.app.Application
import androidx.lifecycle.*
import cz.vitskalicky.lepsirozvrh.*
import cz.vitskalicky.lepsirozvrh.fragment.RozvrhViewModel.Companion.PERM
import cz.vitskalicky.lepsirozvrh.model.Account
import cz.vitskalicky.lepsirozvrh.model.RozvrhRecord
import cz.vitskalicky.lepsirozvrh.model.RozvrhRecord.Key
import cz.vitskalicky.lepsirozvrh.model.StatusInfo
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh
import org.joda.time.LocalDate
//todo is this still needed?

class MainActivityViewModel(
    application: Application,
) : AndroidViewModel(application), RozvrhViewModel {
    private val repository = getApplication<MainApplication>().repository
    private val accountRepository = getApplication<MainApplication>().accountRepository
    private val accountIdLD: LiveData<Long?> = SharedPrefsLongLiveData(application.prefs.sharedPreferences, PrefsConsts.ACTIVE_ACCOUNT_ID, -1).map { if (it == -1L) null else it }
    override fun getAccountIdLD(): LiveData<Long?> = accountIdLD
    private val accountLD: LiveData<Account?> = accountIdLD.switchMap { it?.let { accountRepository.getAccountLD(it) } ?: MutableLiveData(null) }
    override fun getAccountLD(): LiveData<Account?> = accountLD
    private val displayLD: MediatorLiveData<RozvrhRecord?> = MediatorLiveData()
    override fun getDisplayLD(): LiveData<RozvrhRecord?> = displayLD
    private val statusLD: MediatorLiveData<StatusInfo> = MediatorLiveData()
    override fun getStatusLD(): LiveData<StatusInfo> = statusLD

    private var currentlyUsedLD: LiveData<RozvrhRecord?>? = null
    private var currentlyUsedStatusLD: LiveData<StatusInfo>? = null

    //"cache" to make switching instant
    private var nextLD: LiveData<RozvrhRecord?>? = null
    private var nextStatusLD: LiveData<StatusInfo>? = null
    private var prevLD: LiveData<RozvrhRecord?>? = null
    private var prevStatusLD: LiveData<StatusInfo>? = null
    private var permLD: LiveData<RozvrhRecord?>? = null
    private var permStatusLD: LiveData<StatusInfo>? = null
    private var thisWeekLD: LiveData<RozvrhRecord?>? = null
    private var thisWeekStatusLD: LiveData<StatusInfo>? = null

    private var invalidateCache = false

    private val switchDayLD = SharedPrefsIntLiveData(application.prefs.sharedPreferences,PrefsConsts.SWITCH_TO_NEXT_WEEK_OPTION_INDEX,0)
    private val switchDayObserver: Observer<Int> = Observer { newOptionIndex->
        invalidateCache = true
        weekPosition = weekPosition
    }

    /**
     * Tells if the last request was successful. If not, infoline should show "offline" on all weeks.
     */
    override fun getIsOfflineLD(): LiveData<Boolean> = repository.getOfflineStatusLiveData()


    var monday: LocalDate = Utils.getDisplayWeekMonday(application)
        private set

    /**
     * If loading rozvrh fails, but there is one in cache, then show the user a messege that they are offline. But when the user refreshed then show the true error.
     */
    override var showError: Boolean = false
        private set

    private fun weekToMonday(week: Int): LocalDate = if(week == PERM) {
        Rozvrh.PERM
    }else{
        Utils.getDisplayWeekMonday(getApplication()).plusWeeks(week)
    }

    /**
     * Loads the LiveData and triggers data load so that it has a value ready to be instantly displayed. todo replace with a better method if you find any.
     */
    private fun prepareLD(week: Int): Pair<LiveData<RozvrhRecord?>, LiveData<StatusInfo>>{
        val mnd = weekToMonday(week)
        val rozvrhLD: LiveData<RozvrhRecord?> = getRozvrhLD(mnd)
        val statusLD: LiveData<StatusInfo> = getRozvrhStatusLD(mnd)

        //we must observe the live data to load the value. Observer is removed as soon as it receives any meaningful data.
        var rozvrhObserver = Observer<RozvrhRecord?> { }
        var statusObserver = Observer<StatusInfo> { };

        statusObserver = Observer {
            if (it.status == StatusInfo.Status.ERROR){
                statusLD.removeObserver(statusObserver)
                rozvrhLD.removeObserver(rozvrhObserver)
            }
        }
        rozvrhObserver = Observer {
            statusLD.removeObserver(statusObserver)
            rozvrhLD.removeObserver(rozvrhObserver)
        }

        //make sure the observer gets removed
        rozvrhLD.observeForever(rozvrhObserver)
        statusLD.observeForever(statusObserver)

        return Pair(rozvrhLD, statusLD)
    }

    /** Returns a live data for given week bound to account of this viewModel */
    private fun getRozvrhLD(monday: LocalDate): LiveData<RozvrhRecord?>{
        return accountIdLD.switchMap { accntId ->
            if (accntId != null) {
                repository.getRozvrhLive(Key(accntId, monday), true)
            } else {
                MutableLiveData<RozvrhRecord?>(null)
            }
        }
    }
    /** Returns a live data for given week bound to account of this viewModel */
    private fun getRozvrhStatusLD(monday: LocalDate): LiveData<StatusInfo>{
        return accountIdLD.switchMap { accntId ->
            if (accntId != null){
                repository.getRozvrhStatusLiveData(Key(accntId,monday))
            }else {
                MutableLiveData<StatusInfo>(StatusInfo.unknown())
            }
        }
    }
//    var accountId: Long?
//        get() = accountIdLD.value
//        set(value) {
//            accountIdLD.value = value
//        }

    /**
     *  0 = current week, 1 = next week, -1 = previous week, [Int.MIN_VALUE] = permanent
     */
    override var weekPosition: Int = 0
        set(value) {
            val old = field
            val diff = value - old;
            field = value;
            monday = weekToMonday(value)

            currentlyUsedLD?.let {
                displayLD.removeSource(it)
                displayLD.value = null
            }
            currentlyUsedStatusLD?.let {
                statusLD.removeSource(it)
                statusLD.value = StatusInfo.loading()
            }
            if (invalidateCache){
                nextLD = null
                nextStatusLD = null
                prevLD = null
                prevStatusLD = null
                permLD = null
                permStatusLD = null
                thisWeekLD = null
                thisWeekStatusLD = null
                currentlyUsedLD = null
                currentlyUsedStatusLD = null
                initThisAndPermLD()
                invalidateCache = false
            }

            if (diff == 1){
                //shift the livedata
                prevLD = currentlyUsedLD
                prevStatusLD = currentlyUsedStatusLD
                currentlyUsedLD = nextLD ?: getRozvrhLD(monday)
                currentlyUsedStatusLD = nextStatusLD ?: getRozvrhStatusLD(monday)
                val nextLDs = prepareLD(field + 1)
                nextLD = nextLDs.first
                nextStatusLD = nextLDs.second
            } else if (diff == -1){
                //shift the livedata
                nextLD = currentlyUsedLD
                nextStatusLD = currentlyUsedStatusLD
                currentlyUsedLD = prevLD ?: getRozvrhLD(monday)
                currentlyUsedStatusLD = prevStatusLD ?: getRozvrhStatusLD(monday)
                val prevLDs = prepareLD(field - 1)
                prevLD = prevLDs.first
                prevStatusLD = prevLDs.second
            } else {

                if (field == 0){
                    //jumped to current week
                    currentlyUsedLD = thisWeekLD
                    currentlyUsedStatusLD = thisWeekStatusLD
                }else if (field == PERM){
                    currentlyUsedLD = permLD
                    currentlyUsedStatusLD = permStatusLD
                }else{
                    currentlyUsedLD = getRozvrhLD(monday)
                    currentlyUsedStatusLD = getRozvrhStatusLD(monday)
                }

                if (field != PERM){
                    val prevLDs = prepareLD(field - 1)
                    prevLD = prevLDs.first
                    prevStatusLD = prevLDs.second
                    val nextLDs = prepareLD( field + 1)
                    nextLD = nextLDs.first
                    nextStatusLD = nextLDs.second
                }
            }

            //soft refresh in case the data has expired (there is no expiration check when just switching live data)
            accountIdLD.value?.let {
                repository.refresh(Key(it, monday), true, force = false)
                if (field != PERM){
                    repository.refresh(Key(it, monday.plusWeeks(1)),true, force = false)
                    repository.refresh(Key(it, monday.plusWeeks(-1)),true, force = false)
                }
            }

            displayLD.addSource(currentlyUsedLD!!) {displayLD.value = it}
            statusLD.addSource(currentlyUsedStatusLD!!) {statusLD.value = it}

            showError = false
        }

    override fun forceRefresh(){
        showError = true
        accountIdLD.value?.let {
            repository.refresh(Key(it, monday), true, true, true)
        }
    }

    private fun initThisAndPermLD(){
        val thisWeekLDs = prepareLD( 0)
        thisWeekLD = thisWeekLDs.first
        thisWeekStatusLD = thisWeekLDs.second
        val permLDs = prepareLD( PERM)
        permLD = permLDs.first
        permStatusLD = permLDs.second
    }

    init {
        initThisAndPermLD()

        switchDayLD.observeForever(switchDayObserver)

        weekPosition = weekPosition
    }

    override fun onCleared() {
        switchDayLD.removeObserver(switchDayObserver)
        super.onCleared()
    }
}

