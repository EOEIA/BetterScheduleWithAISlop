package cz.vitskalicky.lepsirozvrh.activity

import android.content.Intent
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
        viewModel.getAccountIdLD().observe(this@MainActivity){
            if (it == null){
                // todo start account picker activity
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        setContent {
            LepsirozvrhTheme {
                RozvrhWithControls(viewModel)
            }
        }
    }
}