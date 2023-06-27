package cz.vitskalicky.lepsirozvrh

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.multidex.MultiDexApplication
import androidx.room.Room
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.jaredrummler.cyanea.Cyanea
import cz.vitskalicky.lepsirozvrh.KotlinUtils.FLAG_IMMUTABLE
import cz.vitskalicky.lepsirozvrh.model.AccountRepository
import cz.vitskalicky.lepsirozvrh.model.RozvrhRepository
import cz.vitskalicky.lepsirozvrh.bakaAPI.rozvrh.RozvrhWebservice
import cz.vitskalicky.lepsirozvrh.database.RozvrhDatabase
import cz.vitskalicky.lepsirozvrh.model.RozvrhStatusStore
import cz.vitskalicky.lepsirozvrh.model.relations.RozvrhRelated
import cz.vitskalicky.lepsirozvrh.notification.NotificationState
import cz.vitskalicky.lepsirozvrh.notification.PermanentNotification
import cz.vitskalicky.lepsirozvrh.schoolsDatabase.SchoolsDatabase
import cz.vitskalicky.lepsirozvrh.schoolsDatabase.SchoolsWebservice
import cz.vitskalicky.lepsirozvrh.theme.DefaultThemes
import cz.vitskalicky.lepsirozvrh.theme.SystemTheme
import cz.vitskalicky.lepsirozvrh.theme.Theme
import cz.vitskalicky.lepsirozvrh.widget.WidgetProvider
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.User
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.joda.time.LocalDateTime
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.*

class MainApplication : MultiDexApplication(), LifecycleOwner {

    // a lifecycle alive for the entire life of MainApplication
    private val lifecycleRegistry = LifecycleRegistry(this);
    public override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    companion object {
        private val TAG = MainApplication::class.java.simpleName
        //private var _jacksonObjectMapper: ObjectMapper? = null
        public val objectMapper: ObjectMapper by lazy {
            val objectMapper = ObjectMapper()
            objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            objectMapper.registerModule(JodaModule())
            objectMapper.registerModule(KotlinModule())
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
    }

    private val tohle = this
    public val mainScope = MainScope()
    lateinit var notificationState: NotificationState
        private set
    private var updateTime: LocalDateTime? = null
    private lateinit var currentWeekLivedata: LiveData<RozvrhRelated?>
    private lateinit var currentWeekObserver: Observer<RozvrhRelated?>

    val rozvrhDb: RozvrhDatabase by lazy {
        Room.databaseBuilder(
                applicationContext,
                RozvrhDatabase::class.java, "rozvrh-database"
        ).build()
    }

    val repository: RozvrhRepository by lazy {
        RozvrhRepository(this)
    }

    val accountRepository: AccountRepository by lazy {
        AccountRepository(this)
    }

    val rozvrhStatusStore: RozvrhStatusStore by lazy {
        RozvrhStatusStore()
    }

    val debugUtils: DebugUtils by lazy {
        DebugUtils(this)
    }

    //region SCHOOLS DATABASE
    val schoolsDb: SchoolsDatabase by lazy {
        Room.databaseBuilder(
                applicationContext,
                SchoolsDatabase::class.java, "schools-database"
        ).build()
    }

    val schoolsRetrofit: Retrofit by lazy {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            val client = OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()
            Retrofit.Builder()
                    .baseUrl("https://vitskalicky.gitlab.io/bakalari-schools-list/")
                    .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                    .client(client)
                    .build()
     }

    val schoolsWebservice: SchoolsWebservice by lazy {
        schoolsRetrofit.create(SchoolsWebservice::class.java)
    }
    //endregion

    override fun onCreate() {
        super.onCreate()

        // Initialize Cyanea theme engine
        Cyanea.init(this, resources)

        // Initialize the Sentry (crash report) client
        if (SharedPrefs.getBooleanPreference(this, R.string.PREFS_SEND_CRASH_REPORTS)) {
            enableSentry()
        } else {
            diableSentry()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Register notification channel for the permanent notification
            val name: CharSequence = getString(R.string.notification_channel_name)
            val description = getString(R.string.notification_detials)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(PermanentNotification.PERMANENT_CHANNEL_ID, name, importance)
            channel.description = description
            channel.setSound(Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.raw.silence), AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
            channel.setShowBadge(false)
            channel.vibrationPattern = null
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        currentWeekObserver = Observer { rozvrh: RozvrhRelated? ->
            /*if (rozvrhWrapper!!.oldRozvrh != null) {
                WidgetProvider.updateAll(rozvrhWrapper.oldRozvrh, this)
                if (SharedPrefs.getBooleanPreference(this, R.string.PREFS_NOTIFICATION, true)) {
                    PermanentNotification.update(rozvrhWrapper.oldRozvrh, this)
                }
            }
            updateUpdateTime(rozvrhWrapper.oldRozvrh)*/
            WidgetProvider.updateAll(rozvrh, this)
            if (SharedPrefs.getBooleanPreference(this, R.string.PREFS_NOTIFICATION, true)) {
                PermanentNotification.update(rozvrh, this)
            }
            updateUpdateTime(rozvrh)
        }

        currentWeekLivedata = repository.getCurrentWeekLD()
        currentWeekLivedata.observe(this, currentWeekObserver)
        if (!SharedPrefs.containsPreference(this, R.string.PREFS_THEME_cHBg)) {
            //theme not initialized yet (first start or after update from pre-themes version)
            SharedPrefs.setStringPreference(this, R.string.PREFS_APP_THEME, "0")
            SharedPrefs.setBooleanPreference(this, R.string.PREFS_FOLLOW_SYSTEM_THEME, true)
            SharedPrefs.setBooleanPreference(this, R.string.PREFS_IS_DARK_THEME_FOR_SYSTEM_APPLIED, false)
            Theme.of(this).themeData = DefaultThemes.getLightTheme()
            Theme.of(this).checkSystemTheme()
        }
        notificationState = NotificationState(this)
        if (SharedPrefs.getBooleanPreference(this, R.string.PREFS_NOTIFICATION, true)) {
            enableNotification()
        } else {
            disableNotification()
        }
        if (SharedPrefs.getInt(this, SharedPrefs.LAST_VERSION_SEEN) < BuildConfig.VERSION_CODE) {
            //a new version is here
            // LAST_VERSION_SEEN is set by MainActivity

            //reapply default theme in case it changed
            var themeNumber = 4
            try {
                themeNumber = SharedPrefs.getStringPreference(this, R.string.PREFS_APP_THEME).toInt()
            } catch (ignored: NumberFormatException) {
            } catch (ignored: NullPointerException) {
            }
            val theme = Theme.of(this)
            when (themeNumber) {
                0 -> {
                    val systemIsDark = SystemTheme.isDarkTheme(this)
                    if (systemIsDark) {
                        theme.themeData = DefaultThemes.getDarkTheme()
                    } else {
                        theme.themeData = DefaultThemes.getLightTheme()
                    }
                    SharedPrefs.setBooleanPreference(this, R.string.PREFS_IS_DARK_THEME_FOR_SYSTEM_APPLIED, systemIsDark)
                }
                1 -> theme.themeData = DefaultThemes.getLightTheme()
                2 -> theme.themeData = DefaultThemes.getDarkTheme()
                3 -> theme.themeData = DefaultThemes.getBlackTheme()
            }
        }
        // "start up" the lifecycle
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        //this just needs to be run time by time, so I thought this could be a good place
        mainScope.launch {
            //delay to give time for the first schedule to load and display as fast as possible and not overload the database with another request.
            delay(1000)
            pruneDatabase()
        }
    }

    fun scheduleUpdate(triggerTime: LocalDateTime?) {
        var triggerTime: LocalDateTime? = triggerTime

        if (notificationState.offsetResetTime != null && triggerTime?.isAfter(notificationState.offsetResetTime) ?: true) {
            triggerTime = notificationState.offsetResetTime
        }
        if (triggerTime == updateTime){
            return
        }
        val intent = Intent(this, UpdateBroadcastReciever::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, UpdateBroadcastReciever.REQUEST_CODE, intent, FLAG_IMMUTABLE)
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        var type: Int = AlarmManager.RTC_WAKEUP;
        if (triggerTime == null){
            type = AlarmManager.RTC
            triggerTime = LocalDateTime.now().plusHours(12) //happens only if the current schedule is not available - could happen if server has been unreachable for over a week
        }
        alarmManager.setRepeating(type, triggerTime!!.toDate().time, (60 * 60000).toLong(), pendingIntent)
        Log.d(TAG, "Scheduled an update on " + triggerTime.toString("MM-dd HH:mm:ss"))
        updateTime = triggerTime
    }

    /**
     * Updates the widget and notification update time using the data from the given Rozvrh. !!! Use [.updateUpdateTime], because that one accounts for week shift during weekend !!!
     *
     * @return true if updated, false if the update time could not be determined from the given rozvrh.
     */
    private fun updateUpdateTime(rozvrh: RozvrhRelated?): Boolean {
        val time = rozvrh?.getUpdateDisplayedDataTime() ?: return false

        scheduleUpdate(time)

        return true
    }

    suspend fun updateUpdateTime() {
        val time: LocalDateTime? = repository.getUpdateDisplayedDataTime()
        scheduleUpdate(time)
    }

    fun enableNotification() {
        SharedPrefs.setBoolean(this, getString(R.string.PREFS_NOTIFICATION), true)
        PermanentNotification.update(currentWeekLivedata.value, this)
    }

    fun disableNotification() {
        SharedPrefs.setBoolean(this, getString(R.string.PREFS_NOTIFICATION), false)
        PermanentNotification.update(null, 0, this)
    }

    /**
     * Starts up sentry crash reporting, but only if it is an official build and crash reporting is
     * allowed (see build.gradle).
     */
    fun enableSentry() {
        /*
         * Only enable sentry on the official release build
         */
        if (BuildConfig.ALLOW_SENTRY) {
            SentryAndroid.init(this) { options ->
                options.dsn = "https://d13d732d380444f5bed7487cfea65814@o322743.ingest.sentry.io/1820627"
            }
            Sentry.setExtra("commit hash", BuildConfig.GitHash)
            if (!SharedPrefs.contains(this, SharedPrefs.SENTRY_ID) || SharedPrefs.getString(this, SharedPrefs.SENTRY_ID).isEmpty()) {
                SharedPrefs.setString(this, SharedPrefs.SENTRY_ID, "android:" + java.lang.Long.toHexString(Random().nextLong()))
            }
            User()
            Sentry.setExtra("build variant", BuildConfig.FLAVOR + " " + BuildConfig.BUILD_TYPE)
            Sentry.setUser(User().also { it.id = SharedPrefs.getString(this, SharedPrefs.SENTRY_ID)})
        } else {
            diableSentry()
            SharedPrefs.setBooleanPreference(this, R.string.PREFS_SEND_CRASH_REPORTS, false)
        }
    }

    fun diableSentry() {
        Sentry.close()
    }

    private val reported = HashSet<String>()
    /**
     * prevents overhauling the crash report service by many identical exceptions
     */
    fun sendReport(e: Exception){
        val msg = e.message?.takeUnless { it.isBlank() } ?: e.stackTraceToString()
        if (!reported.contains(msg)){
            reported.add(msg)
            Sentry.captureException(e)
        }
    }

    public suspend fun pruneDatabase() {
        rozvrhDb.rozvrhDao().deleteUnnecessary()
    }

    /** Calling a suspend fun from java is annoying
     */
    public fun pruneDatabaseAsync() {
        mainScope.launch {
            pruneDatabase()
        }
    }



    override fun onTerminate() {
        // "destroy" the lifecycle
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        mainScope.cancel()
        super.onTerminate()
    }
}