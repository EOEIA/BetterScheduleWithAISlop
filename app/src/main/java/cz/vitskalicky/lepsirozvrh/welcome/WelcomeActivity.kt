package cz.vitskalicky.lepsirozvrh.welcome

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cz.vitskalicky.lepsirozvrh.*
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.mainActivity.MainActivity
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LepsirozvrhTheme {
                Scaffold {
                    Column(
                        Modifier.padding(it).fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(32.dp))
                        Box(Modifier.sizeIn(maxWidth = 100.dp)) {
                            Image(
                                painterResource(R.drawable.icon_rounded_with_background),
                                stringResource(R.string.alt_icon)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.h2, textAlign = TextAlign.Center )
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.app_moto), textAlign = TextAlign.Center )

                        Spacer(Modifier.weight(1.0f))

                        var sentryChecked: Boolean by rememberSaveable{ mutableStateOf(true) }
                        if (BuildConfig.ALLOW_SENTRY) {
                            Row(modifier = Modifier.fillMaxWidth().clickable { sentryChecked = !sentryChecked }) {
                                Checkbox(checked = sentryChecked,onCheckedChange = {sentryChecked = it})
                                Text(stringResource(R.string.welcome_send_crash))
                            }
                        }
                        Spacer(Modifier.size(8.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.PRIVACY_POLICY_LINK)))
                                startActivity(browserIntent)
                            }){
                                Text(stringResource(R.string.privacy_policy))
                            }
                            Button(onClick = {next(sentryChecked)}){
                                Text(stringResource(R.string.start))
                            }
                        }
                    }
                }
            }
        }
    }


    private fun next(enableSentry: Boolean){
        val checkedEnableSentry = enableSentry && BuildConfig.ALLOW_SENTRY;
        val app = applicationContext as MainApplication;
        if (checkedEnableSentry) app.enableSentry() else app.disableSentry();

        prefs.edit { putBoolean(PrefsConsts.SEEN_WELCOME, true) }

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
