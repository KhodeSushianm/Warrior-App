package com.warrior.feature.progress

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.warrior.core.designsystem.components.DeltaText
import com.warrior.core.designsystem.components.WarriorBadge
import com.warrior.core.designsystem.components.WarriorCard
import com.warrior.core.designsystem.components.WarriorTopBar
import com.warrior.core.designsystem.theme.TextMuted

@Composable
fun ProgressScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
    ) {
        WarriorTopBar(title = "Progress")
        WarriorCard {
            Text(
                "WEEK OVER WEEK · SAMPLE",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
            Spacer(Modifier.height(10.dp))
            Text("Sessions 3 (+1) · Time 2h 45m (+35m) · Rounds 19 (+15)", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            DeltaText(delta = 35, unit = "m")
        }
        Spacer(Modifier.height(12.dp))
        WarriorBadge(text = "PHASE 7–8 · ENGINE + CHARTS")
    }
}
