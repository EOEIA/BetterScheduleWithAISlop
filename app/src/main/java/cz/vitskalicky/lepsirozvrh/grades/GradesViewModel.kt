package cz.vitskalicky.lepsirozvrh.grades

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cz.vitskalicky.lepsirozvrh.BuildConfig
import cz.vitskalicky.lepsirozvrh.DebugUtils
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.PrefsConsts
import cz.vitskalicky.lepsirozvrh.bakaAPI.marks.MarkSubject
import cz.vitskalicky.lepsirozvrh.notification.GradeNotification
import cz.vitskalicky.lepsirozvrh.prefs
import kotlinx.coroutines.launch

enum class SubjectSortOrder { NAME, AVERAGE_BEST, AVERAGE_WORST }
enum class GradesViewMode { BY_SUBJECT, BY_DATE }

class GradesViewModel(app: Application) : AndroidViewModel(app) {

    val subjects = MutableLiveData<List<MarkSubject>>(emptyList())
    val isLoading = MutableLiveData(false)
    val error = MutableLiveData<String?>(null)
    val subjectSortOrder = MutableLiveData(SubjectSortOrder.NAME)
    val gradesViewMode = MutableLiveData(GradesViewMode.BY_SUBJECT)
    /** Mark IDs not yet acknowledged by the user in the UI. */
    val newMarkIds = MutableLiveData<Set<String>>(emptySet())

    init {
        loadGrades()
    }

    fun loadGrades() {
        val application = getApplication<MainApplication>()
        if (BuildConfig.DEBUG && application.prefs.boolean(PrefsConsts.DEBUG_DEMO_MODE) == true) {
            val demo = DebugUtils.getDemoGrades()
            subjects.value = demo
            updateNewMarkIds(demo)
            isLoading.value = false
            error.value = null
            return
        }
        val accountId = application.prefs.long(PrefsConsts.ACTIVE_ACCOUNT_ID) ?: run {
            error.value = "No account selected"
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                val account = application.accountRepository.getAccount(accountId) ?: run {
                    error.value = "Not logged in"
                    isLoading.value = false
                    return@launch
                }
                val webservice = application.accountRepository.getMarksWebservice(account) ?: run {
                    error.value = "Could not connect to server"
                    isLoading.value = false
                    return@launch
                }
                val loaded = webservice.getMarks().Subjects
                subjects.value = loaded
                updateNewMarkIds(loaded)
                try {
                    GradeNotification.maybeNotifyNewGrades(application, loaded)
                } catch (_: Exception) { }
            } catch (e: Exception) {
                error.value = e.message ?: "Error loading grades"
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun updateNewMarkIds(allSubjects: List<MarkSubject>) {
        val prefs = getApplication<MainApplication>().prefs
        val seenRaw = prefs.string(PrefsConsts.GRADES_SEEN_IDS)
        val allIds = allSubjects.flatMap { s -> s.Marks.map { it.Id } }.toSet()
        if (seenRaw == null) {
            // First load ever — seed all current IDs as seen so nothing highlights on clean install
            prefs.putOne(PrefsConsts.GRADES_SEEN_IDS, allIds.joinToString(","))
            newMarkIds.value = emptySet()
        } else {
            val seen = seenRaw.split(",").filter { it.isNotBlank() }.toSet()
            newMarkIds.value = allIds - seen
        }
    }

    /** Call when a user opens or views a subject; marks all its grade IDs as seen after a delay. */
    fun markSubjectAsSeen(subject: MarkSubject) {
        val prefs = getApplication<MainApplication>().prefs
        val seenRaw = prefs.string(PrefsConsts.GRADES_SEEN_IDS) ?: ""
        val seen = seenRaw.split(",").filter { it.isNotBlank() }.toMutableSet()
        val subjectIds = subject.Marks.map { it.Id }.toSet()
        seen.addAll(subjectIds)
        prefs.putOne(PrefsConsts.GRADES_SEEN_IDS, seen.joinToString(","))
        newMarkIds.value = (newMarkIds.value ?: emptySet()) - subjectIds
    }
}
