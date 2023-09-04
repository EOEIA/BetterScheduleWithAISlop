package cz.vitskalicky.lepsirozvrh.accountPicker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import cz.vitskalicky.lepsirozvrh.activity.LoginActivity
import cz.vitskalicky.lepsirozvrh.activity.MainActivity
import kotlinx.coroutines.launch

class AccountPickerActivity : ComponentActivity() {

    val viewModel: AccountPickerViewModel by viewModels()
    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scrollState = rememberScrollState()
            val composableScope = rememberCoroutineScope()
            val accounts by viewModel.accountsLD.observeAsState();
            val currentAccount by viewModel.currentAccountIdLD.observeAsState()
            LepsirozvrhTheme {
                Scaffold(
                    topBar = {TopAppBar(
                        title = {Text(stringResource(R.string.account_picker_title))},
                    ) }
                ) { paddingValues: PaddingValues ->
                    Column(
                        Modifier
                            .verticalScroll(scrollState)
                            .fillMaxSize()
                            .padding(paddingValues),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(stringResource(R.string.account_picker_subtitle),
                            style = MaterialTheme.typography.subtitle1,
                            modifier = Modifier.padding(top = 32.dp, start = 24.dp, end = 24.dp))
                        Spacer(Modifier.size(16.dp))

                        for (item in accounts?: emptyList()){
                            val isSelected = item.id == currentAccount
                            Surface(
                                color = if (isSelected) MaterialTheme.colors.primarySurface else MaterialTheme.colors.surface,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                                    .clickable {
                                        composableScope.launch {
                                            switchAccount(item.id, currentAccount)
                                        }
                                    }
                            ) {
                                ListItem(
                                    text = { Text(item.fullName) },
                                    secondaryText = { Text(item.schoolName) },
                                    trailing = { Text(item.userTypeText) },
                                    icon = if (isSelected) {
                                        {
                                            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center){
                                                Icon(
                                                    Icons.Default.Check,
                                                    stringResource(R.string.account_picker_selected),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                        }
                                    } else {
                                        {
                                            Box() {}
                                        }
                                    }
                                )
                            }
                        }

                        Surface(
                            Modifier.padding(horizontal = 16.dp)
                                .clickable { addAccount() },
                            shape = MaterialTheme.shapes.medium,
                            ) {
                            ListItem(
                                text = { Text(stringResource(R.string.account_picker_add)) },
                                icon = {
                                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center){
                                        Icon(
                                            Icons.Default.PersonAdd,
                                            null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun switchAccount(newId: Long, oldId: Long?){
        if (oldId != newId) {
            viewModel.switchToAccount(newId);
            intent = Intent(this, MainActivity::class.java);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
        finish()

    }

    private fun addAccount(){
        intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
}