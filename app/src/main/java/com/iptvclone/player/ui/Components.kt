package com.iptvclone.player.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Clique + destaque visual de foco (essencial para controle remoto / D-pad em Android TV). */
@Composable
fun Modifier.tvClickable(
    shape: Shape = RoundedCornerShape(10.dp),
    onClick: () -> Unit,
): Modifier {
    var focused by remember { mutableStateOf(false) }
    val border = if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier
    return this
        .onFocusChanged { focused = it.isFocused }
        .then(border)
        .clip(shape)
        .clickable(onClick = onClick)
}
