package com.antago30.laboratory.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.antago30.laboratory.ui.theme.LaboratoryTheme
import com.antago30.laboratory.util.SettingsRepository
import com.antago30.laboratory.viewmodel.LabControlViewModel
import com.antago30.laboratory.viewmodel.LabControlViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: LabControlViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Создаём репозиторий и фабрику
        val settingsRepo = SettingsRepository(applicationContext)
        val factory = LabControlViewModelFactory(settingsRepo)

        // Получаем ViewModel через фабрику
        viewModel = ViewModelProvider(this, factory)[LabControlViewModel::class.java]
        viewModel.setAppContext(applicationContext)

        enableEdgeToEdge()
        setContent {
            LaboratoryTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    content = { padding ->
                        LaboratoryApp(
                            viewModel = viewModel,
                            modifier = Modifier.padding(padding)
                        )
                    }
                )
            }
        }
    }
}