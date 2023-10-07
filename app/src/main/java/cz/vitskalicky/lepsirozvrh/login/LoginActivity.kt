package cz.vitskalicky.lepsirozvrh.login

import android.app.ActivityManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.mainActivity.MainActivity
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme
import kotlinx.coroutines.launch

/** Activity with the login form */
class LoginActivity : ComponentActivity() {
    val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // determine, whether this activity is the only one on back stack - used for showing or hiding the back arrow
        val appTasks = (getSystemService(ACTIVITY_SERVICE) as ActivityManager).appTasks
        val activitiesCount: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            appTasks.map { it.taskInfo.numActivities }.sum()
        } else {
            // did not find a reliable way to count previous activities on older API.
            // the few people with old phones will have to cope with missing back arrow
            1
        }
        val showAppBar = activitiesCount != 1

        setContent {
            val scope = rememberCoroutineScope()
            LepsirozvrhTheme(hasAppBar = showAppBar) {
                Scaffold(
                    topBar = if (!showAppBar) { // hide the app bar if this is the only activity on the back stack
                        {}
                    } else {
                        {
                            TopAppBar(
                                title = { Text(stringResource(R.string.login_add_title)) },
                                navigationIcon = {
                                    IconButton({
                                        finish()
                                    }) {
                                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                                    }
                                }
                            )
                        }
                    }
                ) { paddingValues ->
                    Box(Modifier.padding(paddingValues)) {
                        StatefulLoginForm(
                            viewModel.getLoginScreenStatusLD(),
                            { schoolInfo, username, password ->
                                scope.launch {
                                    val accountId: Long? = viewModel.login(schoolInfo, username, password)
                                    if (accountId != null) {
                                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        finishAffinity()
                                        startActivity(intent)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

    }
}