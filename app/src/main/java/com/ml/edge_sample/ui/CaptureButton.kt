package com.ml.edge_sample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Manual capture button - circular gray button for manual document capture.
 *
 * This button appears when auto-capture is disabled, allowing users to
 * manually trigger document capture.
 *
 * @param enabled Whether the button is enabled (clickable)
 * @param onClick Callback when button is clicked
 * @param modifier Modifier for the button
 */
@Composable
fun CaptureButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(if (enabled) Color.Gray.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.3f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Inner circle
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (enabled) Color.White else Color.LightGray)
        )
    }
}
