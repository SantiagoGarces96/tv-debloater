package tv.clean.debloater

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tv.clean.debloater.ui.DebloaterApp
import tv.clean.debloater.ui.theme.TvDebloaterTheme
import tv.clean.debloater.vm.DebloaterViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TvDebloaterTheme {
                val vm: DebloaterViewModel = viewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                Surface(modifier = Modifier.fillMaxSize()) {
                    Scaffold { padding ->
                        DebloaterApp(
                            state = state,
                            vm = vm,
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }
        }
    }
}
