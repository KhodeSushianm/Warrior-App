package com.warrior.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.warrior.app.navigation.WarriorApp
import com.warrior.core.designsystem.theme.WarriorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WarriorTheme {
                WarriorApp()
            }
        }
    }
}
