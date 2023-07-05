package cz.vitskalicky.lepsirozvrh.schoolsDatabase

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.compose.*
import com.fasterxml.jackson.databind.JsonMappingException
import cz.vitskalicky.lepsirozvrh.KotlinUtils
import cz.vitskalicky.lepsirozvrh.MainApplication
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class SchoolPickerViewModel(application: Application): AndroidViewModel(application){
    private val app = application as MainApplication
    private val db = app.schoolsDb
    private val webservice = app.schoolsWebservice;
    fun getPaged(): ()->PagingSource<Int, SchoolInfo>{
        return db.schoolDAO().queryAllSchools().asPagingSourceFactory()
    }

    suspend fun refresh(){
//        statusLD.value = StatusInfo.loading()

        val allSchools: List<SchoolInfo>? = try {
            webservice.fetchSchools()
        }catch (e: JsonMappingException){
            val f = RuntimeException("Failed to parse schools list", e)
//            app().sendReport(f)
//            statusLD.value = StatusInfo.error(StatusInfo.Specification.ERROR_UNEXPECTED_RESPONSE, R.string.schools_info_connection_failed)
            null
        }catch (e : IOException){
//            statusLD.value = StatusInfo.error(StatusInfo.Specification.ERROR_UNREACHABLE, R.string.schools_info_connection_failed)
            if (!KotlinUtils.isOnline()){
//                statusLD.value = StatusInfo.error(StatusInfo.Specification.ERROR_UNREACHABLE, R.string.no_internet)
            }
            null
        }catch (e: HttpException) {
//            statusLD.value = StatusInfo.error(StatusInfo.Specification.ERROR_UNEXPECTED_RESPONSE, R.string.schools_info_connection_failed)
            if (!KotlinUtils.isOnline()) {
//                statusLD.value = StatusInfo.error(StatusInfo.Specification.ERROR_UNREACHABLE, R.string.no_internet)
            }
            null
        }catch (e: CancellationException) {
            throw e;
        }catch (e: Exception){
            val f = RuntimeException("Failed to load schools list: ${e.message}", e)
//            app().sendReport(f)
//            statusLD.value = StatusInfo.error(StatusInfo.Specification.ERROR_UNEXPECTED_RESPONSE, R.string.schools_info_connection_failed)
            null
        }

        if (allSchools != null){
            if (allSchools.size > 0) {
                db.replaceSchools(allSchools)
//                SharedPrefs.setStringPreference(app(), R.string.PREFS_LAST_SCHOOLS_LIST_UPDATE, ISODateTimeFormat.dateTime().print(
//                    DateTime.now()))
//                statusLD.value = StatusInfo.success()
            }else{
                val f = RuntimeException("Schools list is empty")
//                app().sendReport(f)
//                statusLD.value = StatusInfo.error(StatusInfo.Specification.ERROR_UNEXPECTED_RESPONSE, R.string.schools_info_connection_failed)
            }
        }
    }
}

@Composable
fun SchoolList2(){
    val viewModel: SchoolPickerViewModel = viewModel()

    SchoolList(viewModel)
}

@Composable
fun SchoolList(viewModel: SchoolPickerViewModel){
    val ps = viewModel.getPaged() ;
    val pager = remember{Pager<Int, SchoolInfo>(
        PagingConfig(pageSize = 20, enablePlaceholders = true),
        0,
        ps
    )}
    val lazyPagingItems: LazyPagingItems<SchoolInfo> = pager.flow.collectAsLazyPagingItems()
    val scope = rememberCoroutineScope()
    Button(onClick = {scope.launch { viewModel.refresh()}}){ Text("Refresh") }
    LazyColumn{
        if (lazyPagingItems.loadState.refresh == LoadState.Loading) {
            item {
                Column(
                    modifier = Modifier.fillParentMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        items(
            count = lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey {it.id},
            contentType = lazyPagingItems.itemContentType { "contentType" }
        ){ index: Int ->
            val item = lazyPagingItems[index]
            Text(item?.name?: "null")
        }

        if (lazyPagingItems.loadState.append == LoadState.Loading) {
            item {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
    }
}