package com.example.edgeaicore.ui.common

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@Composable
fun RichMessageContent(
    text: String,
    isUser: Boolean = false,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    SelectionContainer {
        Text(
            text = text,
            modifier = modifier,
            color = textColor,
            fontSize = 14.sp
        )
    }
}
