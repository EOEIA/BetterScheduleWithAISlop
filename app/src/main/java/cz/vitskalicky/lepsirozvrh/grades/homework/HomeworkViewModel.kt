package cz.vitskalicky.lepsirozvrh.grades.homework

import android.app.Application
import android.text.Html
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import cz.vitskalicky.lepsirozvrh.BuildConfig
import cz.vitskalicky.lepsirozvrh.DebugUtils
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.PrefsConsts
import cz.vitskalicky.lepsirozvrh.Utils
import cz.vitskalicky.lepsirozvrh.bakaAPI.homework.Homework
import cz.vitskalicky.lepsirozvrh.database.HomeworkDone
import cz.vitskalicky.lepsirozvrh.database.PersonalTask
import cz.vitskalicky.lepsirozvrh.model.RozvrhRecord
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhCaption
import cz.vitskalicky.lepsirozvrh.prefs
import kotlinx.coroutines.launch
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.joda.time.format.ISODateTimeFormat

class HomeworkViewModel(app: Application) : AndroidViewModel(app) {
    val items = MutableLiveData<List<HomeworkItem>>(emptyList())
    val isLoading = MutableLiveData(false)

    val personalTasks: LiveData<List<PersonalTask>>
    /** homework id -> the user's explicit override of Bakaláři's Done flag */
    val homeworkDone: LiveData<Map<String, Boolean>>

    private val activeAccountId: Long?

    init {
        val application = getApplication<MainApplication>()
        activeAccountId = application.prefs.long(PrefsConsts.ACTIVE_ACCOUNT_ID)
        personalTasks = if (activeAccountId != null) {
            application.rozvrhDb.personalTaskDao().getAllForAccount(activeAccountId)
        } else {
            MutableLiveData(emptyList())
        }
        homeworkDone = if (activeAccountId != null) {
            application.rozvrhDb.homeworkDoneDao().getAllForAccount(activeAccountId)
                .map { rows -> rows.associate { it.homeworkId to it.isDone } }
        } else {
            MutableLiveData(emptyMap())
        }
        loadHomework()
    }

    fun addTask(
        title: String,
        subject: String = "",
        dueDate: LocalDate? = null,
        dueTime: LocalTime? = null,
        lessonKey: String? = null
    ) {
        val accountId = activeAccountId ?: return
        val application = getApplication<MainApplication>()
        viewModelScope.launch {
            application.rozvrhDb.personalTaskDao().insert(
                PersonalTask(
                    accountId = accountId,
                    title = title,
                    subject = subject,
                    dueDate = dueDate,
                    dueTime = dueTime,
                    lessonKey = lessonKey
                )
            )
        }
    }

    fun setHomeworkDone(homeworkId: String, done: Boolean) {
        val accountId = activeAccountId ?: return
        val application = getApplication<MainApplication>()
        viewModelScope.launch {
            application.rozvrhDb.homeworkDoneDao().upsert(HomeworkDone(accountId, homeworkId, done))
        }
    }

    fun toggleTaskDone(task: PersonalTask) {
        val application = getApplication<MainApplication>()
        viewModelScope.launch {
            application.rozvrhDb.personalTaskDao().setDone(task.id, !task.isDone)
        }
    }

    fun deleteTask(id: Long) {
        val application = getApplication<MainApplication>()
        viewModelScope.launch {
            application.rozvrhDb.personalTaskDao().delete(id)
        }
    }

    fun loadHomework() {
        val application = getApplication<MainApplication>()
        if (BuildConfig.DEBUG && application.prefs.boolean(PrefsConsts.DEBUG_DEMO_MODE) == true) {
            items.value = DebugUtils.getDemoHomework()
            isLoading.value = false
            return
        }
        val accountId = application.prefs.long(PrefsConsts.ACTIVE_ACCOUNT_ID) ?: run {
            isLoading.value = false
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            // matches the week the main schedule is currently showing (respects the
            // "switch to next week" preference), not the strict calendar week
            val monday = Utils.getDisplayWeekMonday(application)
            // real network fetch (falls back to cache only if the request fails) instead of cache-only,
            // so the screen doesn't go empty just because nothing has been cached yet this session
            val rozvrh = application.repository.getRozvrh(RozvrhRecord.Key(accountId, monday), true)
            val homeworks = fetchHomeworks(application, accountId)
            // The /homeworks endpoint is the source of truth: it lists every assignment regardless
            // of whether the timetable atoms happen to carry matching homeworkIds (many schools
            // return none, which used to leave this screen permanently empty). The schedule is only
            // consulted to pin an assignment to a lesson time.
            items.value = if (homeworks.isNotEmpty()) {
                homeworks.toHomeworkItems(rozvrh)
            } else {
                rozvrh?.extractHomework(homeworks.descriptionsById()) ?: emptyList()
            }
            isLoading.value = false
        }
    }

}

private val homeworkDateFormatter = ISODateTimeFormat.dateTimeParser().withOffsetParsed()

private fun parseHomeworkDate(raw: String): LocalDate? = try {
    if (raw.isBlank()) null else homeworkDateFormatter.parseLocalDate(raw)
} catch (e: Exception) {
    null
}

/**
 * Turns the raw API assignments into display items, looking up the matching lesson in [rozvrh]
 * (when there is one) to get the time of day the assignment is due.
 */
fun List<Homework>.toHomeworkItems(rozvrh: Rozvrh?): List<HomeworkItem> {
    val beginTimesById = HashMap<String, LocalTime>()
    rozvrh?.days?.forEach { day ->
        rozvrh.captions.indices.forEach caption@{ ci ->
            val begin = rozvrh.captions.getOrNull(ci)?.beginTime ?: return@caption
            (day.blocks.getOrNull(ci) ?: emptyList()).forEach { lesson ->
                // first lesson wins - Map.putIfAbsent needs API 24, minSdk here is 21
                lesson.homeworkIds.forEach { id -> if (!beginTimesById.containsKey(id)) beginTimesById[id] = begin }
            }
        }
    }
    return map { hw ->
        HomeworkItem(
            id = hw.ID,
            subjectName = hw.Subject.Name.ifBlank { hw.Subject.Abbrev },
            subjectAbbrev = hw.Subject.Abbrev.ifBlank { hw.Subject.Name },
            description = hw.Content.takeIf { it.isNotBlank() },
            date = parseHomeworkDate(hw.DateEnd),
            lessonBeginTime = beginTimesById[hw.ID],
            isDone = hw.Done
        )
    }.sortedByDescending { it.date }
}

fun List<Homework>.descriptionsById(): Map<String, String> =
    filter { it.Content.isNotBlank() }.associate { it.ID to it.Content }

/**
 * Fetches every assignment the account can see. Returns an empty list (never throws) when the
 * account is gone or the request fails, so callers degrade to whatever the schedule carries.
 */
suspend fun fetchHomeworks(application: MainApplication, accountId: Long): List<Homework> {
    return try {
        val account = application.accountRepository.getAccount(accountId) ?: return emptyList()
        val webservice = application.accountRepository.getHomeworkWebservice(account) ?: return emptyList()
        webservice.getHomeworks().Homeworks.map { hw ->
            hw.copy(Content = Html.fromHtml(hw.Content, Html.FROM_HTML_MODE_LEGACY).toString().trim())
        }
    } catch (e: Exception) {
        Log.w("Homework", "Could not fetch homework", e)
        emptyList()
    }
}

/** Shared with the background periodic check in [cz.vitskalicky.lepsirozvrh.UpdateBroadcastReciever]. */
suspend fun fetchHomeworkDescriptions(application: MainApplication, accountId: Long): Map<String, String> =
    fetchHomeworks(application, accountId).descriptionsById()

fun Rozvrh.extractHomework(descriptionsById: Map<String, String> = emptyMap()): List<HomeworkItem> =
    days.flatMap { day ->
        captions.indices.flatMap { ci ->
            val caption: RozvrhCaption? = captions.getOrNull(ci)
            (day.blocks.getOrNull(ci) ?: emptyList()).flatMap { lesson ->
                if (lesson.homeworkIds.isEmpty()) emptyList()
                else lesson.homeworkIds.map { id ->
                    val desc = descriptionsById[id] ?: lesson.homeworkDescriptions.firstOrNull()
                    HomeworkItem(id, lesson.subjectName, lesson.subjectAbbrev, desc, day.date, caption?.beginTime)
                }
            }
        }
    }.sortedByDescending { it.date }
