package cz.vitskalicky.lepsirozvrh.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cz.vitskalicky.lepsirozvrh.compose.Rozvrhpreview
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LepsirozvrhTheme {
                Rozvrhpreview()
            }
        }
    }
}