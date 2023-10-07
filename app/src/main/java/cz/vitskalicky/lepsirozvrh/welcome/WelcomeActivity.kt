package cz.vitskalicky.lepsirozvrh.welcome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LepsirozvrhTheme {
                Scaffold {
                    Column(
                        Modifier.padding(it)
                    ) {
                        Spacer(Modifier.height(16.dp))
                        Image(painterResource(R.mipmap.ic_launcher), stringResource(R.string.alt_icon))
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.app_name))

                    }
                }
            }
        }
    }
}
