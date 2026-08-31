package cz.vitskalicky.lepsirozvrh.grades.homework

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels

class HomeworkActivity : ComponentActivity() {
    private val viewModel: HomeworkViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HomeworkScreen(viewModel, onBack = { finish() }) }
    }
}
