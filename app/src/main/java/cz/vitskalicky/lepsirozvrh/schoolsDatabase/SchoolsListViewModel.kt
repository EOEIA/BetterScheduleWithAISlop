package cz.vitskalicky.lepsirozvrh.schoolsDatabase

import android.app.Application
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.fasterxml.jackson.databind.JsonMappingException
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.SharedPrefs
import cz.vitskalicky.lepsirozvrh.model.Resource
import cz.vitskalicky.lepsirozvrh.model.StatusInfo
import io.sentry.Sentry
import kotlinx.coroutines.launch
import okio.IOException
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import retrofit2.HttpException

class SchoolsListViewModel(
        application: Application,
) : AndroidViewModel(application){
    private fun app() = getApplication<MainApplication>()
    private val db = app().schoolsDb
    private val dao = db.schoolDAO()
    private val webservice = app().schoolsWebservice

    val allSchools: LiveData<PagedList<SchoolInfo>> = LivePagedListBuilder(dao.queryAllSchools(),50).build()
    private val query = MutableLiveData("")
    val queriedSchools: LiveData<PagedList<SchoolInfo>> = query.switchMap {
        LivePagedListBuilder(
                if (it.isBlank()){
                    dao.queryAllSchools()
                }else{
                    dao.search(it)
                },
                50
        ).build()
    }
    private val statusLD: MutableLiveData<StatusInfo> = MutableLiveData(StatusInfo.unknown())

    fun getStatusLD(): LiveData<StatusInfo> { return statusLD }

    fun setQuery(queryString: String){
        val newQuery = queryString.simplified().replace(" ","* ") + "*"
        query.value = newQuery
    }

    init {
        viewModelScope.launch {
            val lastUpdate: LocalDateTime? = SharedPrefs.getStringPreference(app(), R.string.PREFS_LAST_SCHOOLS_LIST_UPDATE).takeUnless { it.isNullOrBlank() }?.let{ ISODateTimeFormat.dateTime().parseLocalDateTime(it)}
            if (lastUpdate == null || lastUpdate.isBefore(LocalDateTime.now().withMillisOfDay(0)) || dao.countAllSchools() == 0){
                //refresh if never refreshed or not refreshed today yet or there are no schools in database for some reason
                statusLD.value = StatusInfo.loading()

                val allSchools: List<SchoolInfo>? = try {
                    webservice.fetchSchools()
                }catch (e: JsonMappingException){
                    val f = RuntimeException("Failed to parse schools list", e)
                    app().sendReport(f)
                    statusLD.value = StatusInfo.unexpectedResponse()
                    null
                }catch (e : IOException){
                    statusLD.value = StatusInfo.unreachable()
                    null
                }catch (e: HttpException){
                    statusLD.value = StatusInfo.unexpectedResponse()
                    null
                }catch (e: Exception){
                    val f = RuntimeException("Failed to load schools list", e)
                    app().sendReport(f)
                    statusLD.value = StatusInfo.unexpectedResponse()
                    null
                }

                if (allSchools != null){
                    if (allSchools.size > 0) {
                        db.replaceSchools(allSchools)
                        statusLD.value = StatusInfo.success()
                    }else{
                        val f = RuntimeException("Schools list is empty")
                        app().sendReport(f)
                        statusLD.value = StatusInfo.unexpectedResponse()
                    }
                }

            }else{
                statusLD.value = StatusInfo.success()
            }
        }
    }
}