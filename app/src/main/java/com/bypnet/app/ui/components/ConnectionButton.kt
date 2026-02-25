package com.bypnet.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypnet.app.ui.theme.*

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED
}

@Composable
fun ConnectionButton(
    state: ConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == ConnectionState.CONNECTING) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (state == ConnectionState.CONNECTED) 0.7f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val buttonColor = when (state) {
        ConnectionState.DISCONNECTED -> StatusDisconnected
        ConnectionState.CONNECTING -> StatusConnecting
        ConnectionState.CONNECTED -> StatusConnected
    }

    val statusText = when (state) {
        ConnectionState.DISCONNECTED -> "TAP TO CONNECT"
        ConnectionState.CONNECTING -> "CONNECTING..."
        ConnectionState.CONNECTED -> "CONNECTED"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(160.dp)
        ) {
            // Outer glow ring
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                buttonColor.copy(alpha = glowAlpha * 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Middle ring
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                buttonColor.copy(alpha = 0.6f),
                                buttonColor.copy(alpha = 0.1f),
                                buttonColor.copy(alpha = 0.6f)
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Main button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = buttonColor.copy(alpha = 0.3f),
                        spotColor = buttonColor.copy(alpha = 0.5f)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                DarkCard,
                                DarkSurfaceVariant
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        color = buttonColor.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .clickable { onClick() }
            ) {
                Icon(
                    imageVector = if (state == ConnectionState.DISCONNECTED)
                        Icons.Filled.PlayArrow else Icons.Filled.Stop,
                    contentDescription = "Connect",
                    tint = buttonColor,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = statusText,
            color = buttonColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}
