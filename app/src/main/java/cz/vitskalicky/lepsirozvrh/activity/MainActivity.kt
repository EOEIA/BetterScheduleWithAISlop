package cz.vitskalicky.lepsirozvrh.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import cz.vitskalicky.lepsirozvrh.compose.RozvrhWithControls
import cz.vitskalicky.lepsirozvrh.fragment.MainActivityViewModel
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LepsirozvrhTheme {
                RozvrhWithControls(viewModel)
            }
        }
    }
}