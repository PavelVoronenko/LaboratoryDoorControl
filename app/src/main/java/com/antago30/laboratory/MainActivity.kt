package com.antago30.laboratory

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.ui.theme.LaboratoryTheme
import com.antago30.laboratory.util.SettingsRepository
import com.antago30.laboratory.viewmodel.labControlViewModel.LabControlViewModel
import com.antago30.laboratory.viewmodel.labControlViewModel.LabControlViewModelFactory
import com.antago30.laboratory.viewmodel.manageUsersViewModel.kt.UserManagementViewModel
import com.antago30.laboratory.viewmodel.manageUsersViewModel.kt.UserManagementViewModelFactory
import com.antago30.laboratory.viewmodel.settingsScreenViewModel.SettingsScreenViewModel
import com.antago30.laboratory.viewmodel.settingsScreenViewModel.SettingsScreenViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MainActivity : ComponentActivity() {

    private lateinit var labControlViewModel: LabControlViewModel
    private lateinit var settingsScreenViewModel: SettingsScreenViewModel
    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var connectionManager: BleConnectionManager
    private lateinit var appLifecycleObserver: AppLifecycleObserver

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val settingsRepo = SettingsRepository(applicationContext)

        connectionManager = BleConnectionManager(
            context = applicationContext,
            coroutineScope = appScope,
            settingsRepo = settingsRepo
        )

        // Создаём observer и сохраняем ссылку
        appLifecycleObserver = AppLifecycleObserver(connectionManager)
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)

        val labControlFactory = LabControlViewModelFactory(
            connectionManager = connectionManager,
            settingsRepo = settingsRepo
        )
        labControlViewModel = ViewModelProvider(this, labControlFactory)[LabControlViewModel::class.java]
        labControlViewModel.setAppContext(applicationContext)
        labControlViewModel.syncServiceState()

        val settingsFactory = SettingsScreenViewModelFactory(
            settingsRepo = settingsRepo,
            connectionManager = connectionManager
        )
        settingsScreenViewModel = ViewModelProvider(this, settingsFactory)[SettingsScreenViewModel::class.java]
        settingsScreenViewModel.setAppContext(applicationContext)

        val userManagementViewModel: UserManagementViewModel = ViewModelProvider(
            this,
            UserManagementViewModelFactory(connectionManager = connectionManager)
        )[UserManagementViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            LaboratoryTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    content = { padding ->
                        LaboratoryApp(
                            labControlViewModel = labControlViewModel,
                            settingsScreenViewModel = settingsScreenViewModel,
                            userManagementViewModel = userManagementViewModel,
                            connectionManager = connectionManager,
                            modifier = Modifier.padding(padding),
                        )
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        appScope.cancel()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(appLifecycleObserver)
    }
}