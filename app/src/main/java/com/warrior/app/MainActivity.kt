package com.warrior.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.warrior.app.locale.AppLocaleHolder
import com.warrior.app.navigation.WarriorApp
import com.warrior.core.designsystem.theme.WarriorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Apply the user's display language (Season 2 / Phase 13).
        super.attachBaseContext(AppLocaleHolder.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WarriorTheme {
                WarriorApp()
            }
        }
    }
}
