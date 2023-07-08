package cz.vitskalicky.lepsirozvrh.schoolsDatabase

import android.app.Application
import android.webkit.URLUtil
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.*
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.fasterxml.jackson.databind.JsonMappingException
import cz.vitskalicky.lepsirozvrh.*
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.model.StatusInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class SchoolPickerViewModel(application: Application): AndroidViewModel(application){
    private val app = application as MainApplication
    private val db = app.schoolsDb
    private val webservice = app.schoolsWebservice;

    private val queryLD = MutableLiveData<String>("")
    private val displayedPagerLiveData = queryLD.distinctUntilChanged().map {query: String -> if (query.isBlank()) allSchoolsPager else getSearchPager(query)  }

    private val statusLD = MutableLiveData<StatusInfo>(StatusInfo.unknown())

    /** The search query */
    var query: String
        get() = queryLD.value ?: ""
        set(value){ queryLD.value = value}
    /** LiveData of what should be displayed */
    val pageLD: LiveData<Pager<Int, SchoolInfo>>
        get() = displayedPagerLiveData
    /** Status of loading the data from the internet */
    val status: LiveData<StatusInfo>
        get() = statusLD

    private val allSchoolsPager: Pager<Int, SchoolInfo> by lazy {
        Pager<Int, SchoolInfo>(
            PagingConfig(pageSize = 20, enablePlaceholders = true),
            0,
            db.schoolDAO().queryAllSchools().asPagingSourceFactory()
        )
    }
    private fun getSearchPager(query: String): Pager<Int,SchoolInfo> {
        return Pager<Int, SchoolInfo>(
            PagingConfig(pageSize = 20, enablePlaceholders = true),
            0,
            db.schoolDAO().search(query.simplified().replace(" ","* ") + "*" ).asPagingSourceFactory()
        )
    }

    suspend fun refresh(){
        statusLD.value = StatusInfo.loading()

        val allSchools: List<SchoolInfo>? = try {
            webservice.fetchSchools()
        }catch (e: JsonMappingException){
            val f = RuntimeException("Failed to parse schools list", e)
            app.sendReport(f)
            statusLD.value = StatusInfo.error(StatusInfo.Specification.ERROR_UNEXPECTED_RESPONSE, R.string.schools_info_connection_failed)
            null
        }catch (e : IOException){
            statusLD.value = StatusInfo.error(StatusInfo.Specification.ERROR_UNREACHABLE, R.string.schools_info_connection_failed)
            if (!KotlinUtils.isOnline()){
                statusLD.value = StatusInfo.error(StatusInfo.Specification.ERROR_UNREACHABLE, R.string.no_internet)
            }
            null
        }catch (e: HttpException) {
            statusLD.value = StatusInfo.error(StatusInfo.Specification.ERROR_UNEXPECTED_RESPONSE, R.string.schools_info_connection_failed)
            if (!KotlinUtils.isOnline()) {
                statusLD.value = StatusInfo.error(StatusInfo.Specification.ERROR_UNREACHABLE, R.string.no_internet)
            }
            null
        }catch (e: CancellationException) {
            throw e;
        }catch (e: Exception){
            val f = RuntimeException("Failed to load schools list: ${e.message}", e)
            app.sendReport(f)
            statusLD.value = StatusInfo.error(StatusInfo.Specification.ERROR_UNEXPECTED_RESPONSE, R.string.schools_info_connection_failed)
            null
        }

        if (allSchools != null){
            if (allSchools.size > 0) {
                db.replaceSchools(allSchools)
                SharedPrefsKt(app).putOne(
                    PrefsConsts.PREFS_LAST_SCHOOLS_LIST_UPDATE,
                    DateTime.now().toString(ISODateTimeFormat.dateTime())
                );
                statusLD.value = StatusInfo.success()
            }else{
                val f = RuntimeException("Schools list is empty")
                app.sendReport(f)
                statusLD.value = StatusInfo.error(StatusInfo.Specification.ERROR_UNEXPECTED_RESPONSE, R.string.schools_info_connection_failed)
            }
        }
    }

    init {
        viewModelScope.launch {
            val lastUpdate: DateTime? = SharedPrefsKt(app).string(PrefsConsts.PREFS_LAST_SCHOOLS_LIST_UPDATE).takeUnless { it.isNullOrBlank() }?.let{ ISODateTimeFormat.dateTime().parseDateTime(it)}
            if (lastUpdate == null || lastUpdate.isBefore(DateTime.now().withMillisOfDay(0)) || app.schoolsDb.schoolDAO().countAllSchools() == 0){
                //refresh if never refreshed or not refreshed today yet or there are no schools in database for some reason
                refresh()
            }else{
                statusLD.value = StatusInfo.success()
            }
        }
    }
}

@Composable
fun SchoolList(onSelect: (SchoolInfo) -> Unit, viewModel: SchoolPickerViewModel = viewModel()){

    var showDialog by rememberSaveable{mutableStateOf(false)}
    var manualText:String by rememberSaveable{ mutableStateOf("") }
    var dialogError: Boolean by rememberSaveable{ mutableStateOf(false) }

    if (showDialog){
        AlertDialog(
            onDismissRequest = {
                showDialog = false
            },
            confirmButton = {TextButton({
                if (!URLUtil.isValidUrl(manualText)){
                    dialogError = true;
                    return@TextButton
                }
                val si = SchoolInfo(
                    id = "",
                    name = "",
                    url = manualText,
                    isManual = true
                )
                onSelect(si)
            }){ Text(stringResource(R.string.ok)) } },
            dismissButton = {TextButton({
                showDialog = false
            }){ Text(stringResource(R.string.cancel)) } },
            title = { Text(stringResource(R.string.use_url_dialog_title)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.use_url_dialog_hint),
                        style = MaterialTheme.typography.body2
                    )
                    Spacer(Modifier.size(8.dp))
                    OutlinedTextField(
                        value =  manualText,
                        onValueChange = {
                            manualText = it;
                            dialogError = false
                        },
                        isError = dialogError
                    )
                    if (dialogError){
                        Text(
                            stringResource(R.string.use_url_invalid),
                            color = MaterialTheme.colors.error.copy(alpha = ContentAlpha.medium),
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                        )
                    }
                }

            }
        )
    }

    SchoolList(viewModel, onSelect,{
        manualText=it
        dialogError = false
        showDialog = true;
    })
}


/** List of schools with search box
 * */
@Composable
fun SchoolList(viewModel: SchoolPickerViewModel, onSelect: (SchoolInfo) -> Unit, onManual: (searchQuery: String) -> Unit){
    val pager by viewModel.pageLD.observeAsState();
    val status by viewModel.status.observeAsState()
    val scope = rememberCoroutineScope()

    val lazyPagingItems = pager?.flow?.collectAsLazyPagingItems()

    val scrollState = rememberLazyListState()

    Column {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text(stringResource(R.string.search)) },
            value = viewModel.query,
            onValueChange = {
                viewModel.query = it
                scope.launch {
                    scrollState.scrollToItem(0)
                }
            }
        )

//        Button(onClick = {
//            scope.launch { viewModel.refresh()}}){ Text("Refresh")
//        }

        if (status?.status == StatusInfo.Status.ERROR){
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.WifiOff, null)
                Text(stringResource(status?.errMessage?: R.string.no_internet))
                TextButton(onClick = {
                    scope.launch {
                        viewModel.refresh()
                    }
                }){
                    Text(stringResource(R.string.schools_retry))
                }
                Spacer(modifier = Modifier.weight(3f))
            }
        }else LazyColumn(
            modifier = Modifier
                .weight(1f),
            state = scrollState,
        ) {
            if (status?.status == StatusInfo.Status.LOADING) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }else if (lazyPagingItems != null) {
                items(
                    count = lazyPagingItems.itemCount,
                    key = lazyPagingItems.itemKey { it.id },
                    contentType = lazyPagingItems.itemContentType { "contentType" }
                ) { index: Int ->
                    val item = lazyPagingItems[index]
                    if (item != null){
                        Column(
                            modifier = Modifier.clickable {
                                onSelect(item)
                            }
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            Text(
                                item.name,
                                style = MaterialTheme.typography.body1
                            )
                            Text(
                                item.url,
                                style = MaterialTheme.typography.caption
                            )
                        }
                    }
                }
                if (lazyPagingItems.loadState.refresh is LoadState.NotLoading){
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    onManual(viewModel.query)
                                }
                                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            Text(
                                stringResource(R.string.use_url),
                                style = MaterialTheme.typography.body1,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.NavigateNext, null)
                        }

                    }

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
    }
}