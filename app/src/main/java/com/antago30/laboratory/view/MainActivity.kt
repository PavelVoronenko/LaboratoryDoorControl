package com.antago30.laboratory.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.antago30.laboratory.ui.theme.LaboratoryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LaboratoryTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LabControlScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}