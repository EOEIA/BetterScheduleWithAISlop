package cz.vitskalicky.lepsirozvrh.grades.homework

import android.app.Application
import android.text.Html
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cz.vitskalicky.lepsirozvrh.BuildConfig
import cz.vitskalicky.lepsirozvrh.DebugUtils
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.PrefsConsts
import cz.vitskalicky.lepsirozvrh.Utils
import cz.vitskalicky.lepsirozvrh.database.PersonalTask
import cz.vitskalicky.lepsirozvrh.model.RozvrhRecord
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhCaption
import cz.vitskalicky.lepsirozvrh.prefs
import kotlinx.coroutines.launch
import org.joda.time.LocalDate
import org.joda.time.LocalTime

class HomeworkViewModel(app: Application) : AndroidViewModel(app) {
    val items = MutableLiveData<List<HomeworkItem>>(emptyList())
    val isLoading = MutableLiveData(false)

    val personalTasks: LiveData<List<PersonalTask>>

    private val activeAccountId: Long?

    init {
        val application = getApplication<MainApplication>()
        activeAccountId = application.prefs.long(PrefsConsts.ACTIVE_ACCOUNT_ID)
        personalTasks = if (activeAccountId != null) {
            application.rozvrhDb.personalTaskDao().getAllForAccount(activeAccountId)
        } else {
            MutableLiveData(emptyList())
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
            val descriptionsById = fetchHomeworkDescriptions(application, accountId)
            items.value = rozvrh?.extractHomework(descriptionsById) ?: emptyList()
            isLoading.value = false
        }
    }

    private suspend fun fetchHomeworkDescriptions(application: MainApplication, accountId: Long): Map<String, String> {
        return try {
            val account = application.accountRepository.getAccount(accountId) ?: return emptyMap()
            val webservice = application.accountRepository.getHomeworkWebservice(account) ?: return emptyMap()
            webservice.getHomeworks().Homeworks
                .filter { it.Content.isNotBlank() }
                .associate { it.ID to Html.fromHtml(it.Content, Html.FROM_HTML_MODE_LEGACY).toString().trim() }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

fun Rozvrh.extractHomework(descriptionsById: Map<String, String> = emptyMap()): List<HomeworkItem> =
    days.flatMap { day ->
        captions.indices.flatMap { ci ->
            val caption: RozvrhCaption? = captions.getOrNull(ci)
            (day.blocks.getOrNull(ci) ?: emptyList()).flatMap { lesson ->
                if (lesson.homeworkIds.isEmpty()) emptyList()
                else lesson.homeworkIds.map { id ->
                    val desc = descriptionsById[id] ?: lesson.homeworkDescriptions.firstOrNull()
                    HomeworkItem(lesson.subjectName, lesson.subjectAbbrev, desc, day.date, caption?.beginTime)
                }
            }
        }
    }.sortedByDescending { it.date }
