package com.warrior.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.warrior.core.designsystem.components.DeltaText
import com.warrior.core.designsystem.components.WarriorBadge
import com.warrior.core.designsystem.components.WarriorButton
import com.warrior.core.designsystem.components.WarriorCard
import com.warrior.core.designsystem.components.WarriorTopBar
import com.warrior.core.designsystem.theme.HeavyBag
import com.warrior.core.designsystem.theme.MittWork
import com.warrior.core.designsystem.theme.TextMuted

@Composable
fun HomeScreen(
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
    ) {
        WarriorTopBar(title = "WARRIOR", actions = { WarriorBadge("SKELETON · PHASE 1") })

        WarriorCard {
            Text(
                "THIS WEEK · SEP 19 – SEP 25",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("2h 45m", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.width(8.dp))
                Text(
                    "training time",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 8.dp),
                )
                DeltaText(delta = 35, unit = "m")
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat(value = "3", label = "SESSIONS", delta = 1)
                MiniStat(value = "19", label = "ROUNDS", delta = 15)
                MiniStat(value = "8", label = "AVG INTENSITY", delta = 1)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("RECENT SESSIONS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(Modifier.height(8.dp))
        WarriorCard {
            SessionRow(color = HeavyBag, title = "Heavy Bag · 6 rounds", sub = "Thu, Sep 24 · 55m · int 8")
            SessionRow(color = MittWork, title = "Mitt Work · 5 rounds +1", sub = "Tue, Sep 22 · 50m · int 7")
        }

        Spacer(Modifier.height(16.dp))
        WarriorButton(text = "+ Log Workout", onClick = onStartWorkout)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RowScope.MiniStat(value: String, label: String, delta: Int) {
    WarriorCard(modifier = Modifier.weight(1f)) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        DeltaText(delta = delta)
    }
}

@Composable
private fun SessionRow(color: Color, title: String, sub: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp),
    ) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}
