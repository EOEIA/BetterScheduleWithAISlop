package cz.vitskalicky.lepsirozvrh.settings

import android.app.Application
import androidx.lifecycle.*
import cz.vitskalicky.lepsirozvrh.*
import cz.vitskalicky.lepsirozvrh.model.Account
import cz.vitskalicky.lepsirozvrh.notification.PermanentNotification
import cz.vitskalicky.lepsirozvrh.whatsnew.WhatsNew

class SettingsViewModel(application: Application): AndroidViewModel(application) {
    private val repository = getApplication<MainApplication>().repository
    private val accountRepository = getApplication<MainApplication>().accountRepository
    private val sp = SharedPrefsKt(getApplication())
    private val app: MainApplication = application as MainApplication;

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

    var stickyDayColumn: Boolean
        get() = sp.boolean(PrefsConsts.STICKY_DAY_COLUMN) ?: true
        set(value) = sp.edit { putBoolean(PrefsConsts.STICKY_DAY_COLUMN, value) }
    val stickyDayColumnLD: LiveData<Boolean> = SharedPrefsBooleanLiveData(sp.sharedPreferences, PrefsConsts.STICKY_DAY_COLUMN, true)

    var highlightCurrentDay: Boolean
        get() = sp.boolean(PrefsConsts.HIGHLIGHT_CURRENT_DAY) ?: false
        set(value) = sp.edit { putBoolean(PrefsConsts.HIGHLIGHT_CURRENT_DAY, value) }
    val highlightCurrentDayLD: LiveData<Boolean> = SharedPrefsBooleanLiveData(sp.sharedPreferences, PrefsConsts.HIGHLIGHT_CURRENT_DAY, false)

    var changedLessonVisuals: Boolean
        get() = sp.boolean(PrefsConsts.CHANGED_LESSON_VISUALS) ?: true
        set(value) = sp.edit { putBoolean(PrefsConsts.CHANGED_LESSON_VISUALS, value) }
    val changedLessonVisualsLD: LiveData<Boolean> = SharedPrefsBooleanLiveData(sp.sharedPreferences, PrefsConsts.CHANGED_LESSON_VISUALS, true)

    var compactTimetable: Boolean
        get() = sp.boolean(PrefsConsts.COMPACT_TIMETABLE) ?: false
        set(value) = sp.edit { putBoolean(PrefsConsts.COMPACT_TIMETABLE, value) }
    val compactTimetableLD: LiveData<Boolean> = SharedPrefsBooleanLiveData(sp.sharedPreferences, PrefsConsts.COMPACT_TIMETABLE, false)

    var showNextLessonCard: Boolean
        get() = sp.boolean(PrefsConsts.SHOW_NEXT_LESSON_CARD) ?: true
        set(value) = sp.edit { putBoolean(PrefsConsts.SHOW_NEXT_LESSON_CARD, value) }
    val showNextLessonCardLD: LiveData<Boolean> = SharedPrefsBooleanLiveData(sp.sharedPreferences, PrefsConsts.SHOW_NEXT_LESSON_CARD, true)

    var transposedTimetable: Boolean
        get() = sp.boolean(PrefsConsts.TIMETABLE_TRANSPOSED) ?: false
        set(value) = sp.edit { putBoolean(PrefsConsts.TIMETABLE_TRANSPOSED, value) }
    val transposedTimetableLD: LiveData<Boolean> = SharedPrefsBooleanLiveData(sp.sharedPreferences, PrefsConsts.TIMETABLE_TRANSPOSED, false)

    var changeAlertsEnabled: Boolean
        get() = sp.boolean(PrefsConsts.CHANGE_ALERTS_ENABLED) ?: false
        set(value) = sp.edit { putBoolean(PrefsConsts.CHANGE_ALERTS_ENABLED, value) }
    val changeAlertsEnabledLD: LiveData<Boolean> = SharedPrefsBooleanLiveData(sp.sharedPreferences, PrefsConsts.CHANGE_ALERTS_ENABLED, false)

    var changeAlertLessons: Boolean
        get() = sp.boolean(PrefsConsts.CHANGE_ALERT_LESSONS) ?: true
        set(value) = sp.edit { putBoolean(PrefsConsts.CHANGE_ALERT_LESSONS, value) }
    val changeAlertLessonsLD: LiveData<Boolean> = SharedPrefsBooleanLiveData(sp.sharedPreferences, PrefsConsts.CHANGE_ALERT_LESSONS, true)

    var changeAlertNoSchool: Boolean
        get() = sp.boolean(PrefsConsts.CHANGE_ALERT_NO_SCHOOL) ?: true
        set(value) = sp.edit { putBoolean(PrefsConsts.CHANGE_ALERT_NO_SCHOOL, value) }
    val changeAlertNoSchoolLD: LiveData<Boolean> = SharedPrefsBooleanLiveData(sp.sharedPreferences, PrefsConsts.CHANGE_ALERT_NO_SCHOOL, true)

    var gradeAlertsEnabled: Boolean
        get() = sp.boolean(PrefsConsts.GRADE_ALERTS_ENABLED) ?: false
        set(value) = sp.edit { putBoolean(PrefsConsts.GRADE_ALERTS_ENABLED, value) }
    val gradeAlertsEnabledLD: LiveData<Boolean> = SharedPrefsBooleanLiveData(sp.sharedPreferences, PrefsConsts.GRADE_ALERTS_ENABLED, false)

    var homeworkAlertsEnabled: Boolean
        get() = sp.boolean(PrefsConsts.HOMEWORK_ALERTS_ENABLED) ?: false
        set(value) = sp.edit { putBoolean(PrefsConsts.HOMEWORK_ALERTS_ENABLED, value) }
    val homeworkAlertsEnabledLD: LiveData<Boolean> = SharedPrefsBooleanLiveData(sp.sharedPreferences, PrefsConsts.HOMEWORK_ALERTS_ENABLED, false)

    var debugDemoMode: Boolean
        get() = BuildConfig.DEBUG && (sp.boolean(PrefsConsts.DEBUG_DEMO_MODE) ?: false)
        set(value) = sp.edit { putBoolean(PrefsConsts.DEBUG_DEMO_MODE, value) }
    val debugDemoModeLD: LiveData<Boolean> =
        SharedPrefsBooleanLiveData(sp.sharedPreferences, PrefsConsts.DEBUG_DEMO_MODE, false)
            .map { BuildConfig.DEBUG && it }

    var sendCrashReports: Boolean
        get() = sp.boolean(PrefsConsts.ENABLE_SENTRY) ?: false
        set(value) {
            if (value){
                app.enableSentry();
            }else{
                app.disableSentry();
            }
        }
    val sendCrashReportsLD: LiveData<Boolean> = SharedPrefsBooleanLiveData(sp.sharedPreferences, PrefsConsts.ENABLE_SENTRY, false)

    var notificationAccountId: Long?
        get() = sp.long(PrefsConsts.NOTIFICATION_ACCOUNT).takeUnless { it != null && !PermanentNotification.isNotificationAccountValid(it) }
        set(value) = sp.edit { if (value == null) putLong(PrefsConsts.NOTIFICATION_ACCOUNT, PermanentNotification.ACCOUNT_NOTIFICATION_DISABLED) else putLong(PrefsConsts.NOTIFICATION_ACCOUNT, value) }
    val notificationAccountIdLD: LiveData<Long?> = SharedPrefsLongLiveData(sp.sharedPreferences, PrefsConsts.NOTIFICATION_ACCOUNT, -1).map { if (it == -1L) null else it }
    val notificationAccountLD: LiveData<Account?> = notificationAccountIdLD.switchMap { it?.let { accountRepository.getAccountLD(it) } ?: MutableLiveData(null) }

    var dontShowNotiBanner: Boolean
        get() = sp.boolean(PrefsConsts.NOTIFICATION_DONT_SHOW_SETTINGS_BANNER) ?: false
        set(value) = sp.putOne(PrefsConsts.NOTIFICATION_DONT_SHOW_SETTINGS_BANNER, value)
    val dontShowNotiBannerLD: LiveData<Boolean> = SharedPrefsBooleanLiveData(sp.sharedPreferences, PrefsConsts.NOTIFICATION_DONT_SHOW_SETTINGS_BANNER, false);

    val shouldNotifyAboutNewLD: LiveData<Boolean> = WhatsNew.shouldNotifyAboutNewLD(app)
    fun userAcknowledgedNew() {
        WhatsNew.userAcknowledgedNew(app)
    }

    suspend fun logout(accountId: Long){
        accountRepository.logout(accountId)
    }
}
