package cz.vitskalicky.lepsirozvrh.settings

import android.app.Application
import androidx.lifecycle.*
import cz.vitskalicky.lepsirozvrh.*
import cz.vitskalicky.lepsirozvrh.model.Account
import cz.vitskalicky.lepsirozvrh.notification.PermanentNotification

class SettingsViewModel(application: Application): AndroidViewModel(application) {
    private val repository = getApplication<MainApplication>().repository
    private val accountRepository = getApplication<MainApplication>().accountRepository
    private val sp = SharedPrefsKt(getApplication())

    val accounts: LiveData<List<Account>> = accountRepository.getAccountsLD();

    val accountIdLD: LiveData<Long?> = SharedPrefsLongLiveData(sp.sharedPreferences, PrefsConsts.ACTIVE_ACCOUNT_ID, -1).map { if (it == -1L) null else it }
    val accountLD: LiveData<Account?> = accountIdLD.switchMap { it?.let { accountRepository.getAccountLD(it) } ?: MutableLiveData(null) }

    var accountId: Long?
        get() = sp.long(PrefsConsts.ACTIVE_ACCOUNT_ID)
        set(value) = sp.edit { if (value == null) remove(PrefsConsts.ACTIVE_ACCOUNT_ID) else putLong(PrefsConsts.ACTIVE_ACCOUNT_ID, value) }

    var showInfoline: Boolean
        get() = sp.boolean(PrefsConsts.SHOW_INFO_LINE) ?: false
        set(value) = sp.edit { putBoolean(PrefsConsts.SHOW_INFO_LINE, value) }
    val showInfolineLD: LiveData<Boolean> = SharedPrefsBooleanLiveData(sp.sharedPreferences, PrefsConsts.SHOW_INFO_LINE, false)

    /**
     * The index of the selected option. See [R.array.switch_to_next_week_entries]
     */
    var switchToNextWeekOptionIndex: Int
        get() = sp.int(PrefsConsts.SWITCH_TO_NEXT_WEEK_OPTION_INDEX) ?: 0
        set(value) = sp.edit {
            putInt(PrefsConsts.SWITCH_TO_NEXT_WEEK_OPTION_INDEX, value)
        }
    val switchToNextWeekOptionIndexLD: LiveData<Int> = SharedPrefsIntLiveData(sp.sharedPreferences, PrefsConsts.SWITCH_TO_NEXT_WEEK_OPTION_INDEX, 0)

    var centerToCurrentLesson: Boolean
        get() = sp.boolean(PrefsConsts.CENTER_TO_CURRENT_LESSON) ?: true
        set(value) = sp.edit { putBoolean(PrefsConsts.CENTER_TO_CURRENT_LESSON, value) }
    val centerToCurrentLessonLD: LiveData<Boolean> = SharedPrefsBooleanLiveData(sp.sharedPreferences, PrefsConsts.CENTER_TO_CURRENT_LESSON, true)

    var sendCrashReports: Boolean
        get() = sp.boolean(PrefsConsts.ENABLE_SENTRY) ?: false
        set(value) = sp.edit { putBoolean(PrefsConsts.ENABLE_SENTRY, value) }
    val sendCrashReportsLD: LiveData<Boolean> = SharedPrefsBooleanLiveData(sp.sharedPreferences, PrefsConsts.ENABLE_SENTRY, false)

    var notificationAccountId: Long?
        get() = sp.long(PrefsConsts.NOTIFICATION_ACCOUNT)
        set(value) = sp.edit { if (value == null) putLong(PrefsConsts.NOTIFICATION_ACCOUNT, PermanentNotification.ACCOUNT_NOTIFICATION_DISABLED) else putLong(PrefsConsts.NOTIFICATION_ACCOUNT, value) }
    val notificationAccountIdLD: LiveData<Long?> = SharedPrefsLongLiveData(sp.sharedPreferences, PrefsConsts.NOTIFICATION_ACCOUNT, -1).map { if (it == -1L) null else it }
    val notificationAccountLD: LiveData<Account?> = notificationAccountIdLD.switchMap { it?.let { accountRepository.getAccountLD(it) } ?: MutableLiveData(null) }

    var dontShowNotiBanner: Boolean
        get() = sp.boolean(PrefsConsts.NOTIFICATION_DONT_SHOW_SETTINGS_BANNER) ?: false
        set(value) = sp.putOne(PrefsConsts.NOTIFICATION_DONT_SHOW_SETTINGS_BANNER, value)
    val dontShowNotiBannerLD: LiveData<Boolean> = SharedPrefsBooleanLiveData(sp.sharedPreferences, PrefsConsts.NOTIFICATION_DONT_SHOW_SETTINGS_BANNER, false);

    suspend fun logout(accountId: Long){
        accountRepository.logout(accountId)
    }
}