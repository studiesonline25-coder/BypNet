package com.bypnet.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypnet.app.ui.theme.*

@Composable
fun PayloadEditor(
    payload: String,
    onPayloadChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "GET / HTTP/1.1[crlf]Host: [host][crlf][crlf]"
) {
    val shape = RoundedCornerShape(12.dp)

    Column(modifier = modifier) {
        Text(
            text = "PAYLOAD / REQUEST HEADER",
            color = TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 200.dp)
                .clip(shape)
                .background(DarkCard)
                .border(0.5.dp, DarkBorder, shape)
                .padding(12.dp)
        ) {
            if (payload.isEmpty()) {
                Text(
                    text = placeholder,
                    color = TextTertiary.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 20.sp
                )
            }
            BasicTextField(
                value = payload,
                onValueChange = onPayloadChange,
                textStyle = TextStyle(
                    color = Cyan400,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 20.sp
                ),
                cursorBrush = SolidColor(Cyan400),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Variable hint chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 6.dp)
        ) {
            val variables = listOf("[host]", "[port]", "[sni]", "[cookie]", "[crlf]")
            variables.forEach { variable ->
                Text(
                    text = variable,
                    color = Cyan400.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Cyan400.copy(alpha = 0.08f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
