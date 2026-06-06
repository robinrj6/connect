package com.example.connect.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReceivedEmojiCountsRow(
    totalCounts: Map<String, Int>,
    newCounts: Map<String, Int>,
    onOpenFriend: () -> Unit,
    showNewCounts: Boolean = true
) {
    val nonZeroTotals = totalCounts
        .filterValues { it > 0 }
        .toList()
        .sortedByDescending { it.second }

    if (nonZeroTotals.isEmpty()) {
        Text(
            text = "No reactions yet",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
            modifier = Modifier.clickable(onClick = onOpenFriend)
        )
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenFriend)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        ) {
        nonZeroTotals.forEach { (emoji, totalCount) ->
            val delta = newCounts[emoji] ?: 0
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                modifier = Modifier.clickable(onClick = onOpenFriend)

            ) {
                Box (
                    modifier = Modifier.padding(
                        horizontal = 8.dp,
                        vertical = 4.dp,
                    )
                ) {
                    Text(
                        text = "$emoji ×$totalCount",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    if (delta > 0 && showNewCounts) {
                        Text(
                            text = "+$delta",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.9f),
                            modifier = Modifier.offset( x=15.dp, y = -(10.dp))
                        )
                    }
                }
            }
        }
    }
}
