package cz.vitskalicky.lepsirozvrh.grades.homework

import android.app.Application
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
        val accountId = application.prefs.long(PrefsConsts.ACTIVE_ACCOUNT_ID) ?: return

        viewModelScope.launch {
            isLoading.value = true
            val monday = Utils.getCurrentMonday()
            val rozvrh = application.repository.getCachedRozvrh(RozvrhRecord.Key(accountId, monday))
            items.value = rozvrh?.extractHomework() ?: emptyList()
            isLoading.value = false
        }
    }
}

fun Rozvrh.extractHomework(): List<HomeworkItem> =
    days.flatMap { day ->
        captions.indices.flatMap { ci ->
            val caption: RozvrhCaption? = captions.getOrNull(ci)
            (day.blocks.getOrNull(ci) ?: emptyList()).flatMap { lesson ->
                if (lesson.homeworkIds.isEmpty()) emptyList()
                else {
                    val descs = lesson.homeworkDescriptions.takeIf { it.isNotEmpty() } ?: lesson.homeworkIds
                    descs.map { desc ->
                        HomeworkItem(lesson.subjectName, lesson.subjectAbbrev, desc, day.date, caption?.beginTime)
                    }
                }
            }
        }
    }.sortedByDescending { it.date }
