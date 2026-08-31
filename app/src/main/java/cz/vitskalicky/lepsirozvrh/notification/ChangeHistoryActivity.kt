package cz.vitskalicky.lepsirozvrh.notification

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme
import org.joda.time.Instant
import org.joda.time.format.DateTimeFormat

class ChangeHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val entries = remember { ChangeHistory.getEntries(applicationContext) }
            val dtFmt = remember { DateTimeFormat.mediumDateTime() }

            LepsirozvrhTheme(tintStatusBar = true, hasAppBar = true) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.change_history_title)) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                                }
                            }
                        )
                    }
                ) { padding ->
                    if (entries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(padding)
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.change_history_empty),
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.padding(padding),
                            contentPadding = PaddingValues(8.dp, 4.dp, 8.dp, 16.dp)
                        ) {
                            items(entries) { entry ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    elevation = 2.dp
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                Instant.ofEpochMilli(entry.timestamp).toDateTime().toString(dtFmt),
                                                style = MaterialTheme.typography.caption,
                                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                entry.monday,
                                                style = MaterialTheme.typography.caption,
                                                color = MaterialTheme.colors.primary
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        entry.lines.forEach { line ->
                                            Text(
                                                "• $line",
                                                style = MaterialTheme.typography.body2,
                                                modifier = Modifier.padding(vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
