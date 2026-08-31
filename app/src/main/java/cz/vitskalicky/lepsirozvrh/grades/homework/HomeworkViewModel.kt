package cz.vitskalicky.lepsirozvrh.grades.homework

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cz.vitskalicky.lepsirozvrh.BuildConfig
import cz.vitskalicky.lepsirozvrh.DebugUtils
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.PrefsConsts
import cz.vitskalicky.lepsirozvrh.Utils
import cz.vitskalicky.lepsirozvrh.model.RozvrhRecord
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhCaption
import cz.vitskalicky.lepsirozvrh.prefs
import kotlinx.coroutines.launch

class HomeworkViewModel(app: Application) : AndroidViewModel(app) {
    val items = MutableLiveData<List<HomeworkItem>>(emptyList())
    val isLoading = MutableLiveData(false)

    init { loadHomework() }

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
