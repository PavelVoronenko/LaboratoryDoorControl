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
import com.antago30.laboratory.viewmodel.factory.LabControlViewModelFactory
import com.antago30.laboratory.viewmodel.SettingsScreenViewModel
import com.antago30.laboratory.viewmodel.factory.SettingsScreenViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var labControlViewModel: LabControlViewModel
    private lateinit var settingsScreenViewModel: SettingsScreenViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsRepo = SettingsRepository(applicationContext)

        val labControlFactory = LabControlViewModelFactory()
        labControlViewModel = ViewModelProvider(this, labControlFactory)[LabControlViewModel::class.java]
        labControlViewModel.setAppContext(applicationContext)

        val settingsFactory = SettingsScreenViewModelFactory(settingsRepo)
        settingsScreenViewModel = ViewModelProvider(this, settingsFactory)[SettingsScreenViewModel::class.java]
        settingsScreenViewModel.setAppContext(applicationContext)

        enableEdgeToEdge()
        setContent {
            LaboratoryTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    content = { padding ->
                        LaboratoryApp(
                            labControlViewModel = labControlViewModel,
                            settingsScreenViewModel = settingsScreenViewModel,
                            modifier = Modifier.padding(padding)
                        )
                    }
                )
            }
        }
    }
}