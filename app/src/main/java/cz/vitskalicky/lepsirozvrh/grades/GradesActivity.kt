package cz.vitskalicky.lepsirozvrh.grades

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels

class GradesActivity : ComponentActivity() {
    private val viewModel: GradesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GradesScreen(viewModel, onBack = { finish() })
        }
    }
}
