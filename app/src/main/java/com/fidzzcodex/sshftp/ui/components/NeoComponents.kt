package com.fidzzcodex.sshftp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fidzzcodex.sshftp.ui.theme.SSHFTPTheme

// ─── Neo Shadow Modifier ──────────────────────────────────────────────────────
fun Modifier.neoShadow(
    shadowColor: Color = Color(0xFF1A1A1A),
    offset: Dp = 5.dp,
): Modifier = this.drawBehind {
    val px = offset.toPx()
    drawRect(color = shadowColor, topLeft = Offset(px, px), size = size)
}

// ─── Neo Border ───────────────────────────────────────────────────────────────
fun Modifier.neoBorder(
    width: Dp = 3.dp,
    color: Color = Color(0xFF1A1A1A),
): Modifier = this.border(width = width, color = color, shape = RoundedCornerShape(0.dp))

// ─── NeoButton ────────────────────────────────────────────────────────────────
@Composable
fun NeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF3366FF),
    textColor: Color = Color.White,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val colors = SSHFTPTheme.colors
    val typography = SSHFTPTheme.typography
    val dims = SSHFTPTheme.dimensions

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonScale",
    )
    val shadowOffset by animateDpAsState(
        targetValue = if (isPressed) 2.dp else dims.shadowOffset,
        label = "shadowOffset",
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .neoShadow(offset = shadowOffset)
            .neoBorder(width = dims.borderWidth, color = colors.border)
            .background(if (enabled) backgroundColor else Color(0xFFCCCCCC))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .height(dims.buttonHeight)
            .padding(horizontal = dims.paddingLarge),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = text.uppercase(),
                style = typography.label,
                color = textColor,
            )
        }
    }
}

// ─── NeoCard ──────────────────────────────────────────────────────────────────
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFF5F5F5),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = SSHFTPTheme.colors
    val dims = SSHFTPTheme.dimensions

    Column(
        modifier = modifier
            .neoShadow(offset = dims.shadowOffset)
            .neoBorder(width = dims.borderWidth, color = colors.border)
            .background(backgroundColor)
            .padding(dims.paddingMedium),
        content = content,
    )
}

// ─── NeoTextField ─────────────────────────────────────────────────────────────
@Composable
fun NeoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "",
    isPassword: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
) {
    val colors = SSHFTPTheme.colors
    val typography = SSHFTPTheme.typography
    val dims = SSHFTPTheme.dimensions

    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            Text(
                text = label.uppercase(),
                style = typography.label,
                color = colors.text,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neoShadow(offset = 3.dp)
                .neoBorder(dims.borderWidth, colors.border)
                .background(Color.White),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = singleLine,
                textStyle = typography.body.copy(color = colors.text),
                visualTransformation = if (isPassword && !passwordVisible)
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
                else
                    androidx.compose.ui.text.input.VisualTransformation.None,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                    ) {
                        if (leadingIcon != null) {
                            Icon(
                                imageVector = leadingIcon,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 8.dp),
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            if (value.isEmpty() && placeholder.isNotEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = typography.body,
                                    color = colors.textSecondary.copy(alpha = 0.5f),
                                )
                            }
                            innerTextField()
                        }
                        if (isPassword) {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff
                                                  else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                )
                            }
                        } else {
                            trailingIcon?.invoke()
                        }
                    }
                },
            )
        }
    }
}

// ─── NeoTopBar ────────────────────────────────────────────────────────────────
@Composable
fun NeoTopBar(
    title: String,
    subtitle: String = "",
    isConnected: Boolean = false,
    onSettingsClick: () -> Unit = {},
    navigationIcon: @Composable (() -> Unit)? = null,
) {
    val colors = SSHFTPTheme.colors
    val typography = SSHFTPTheme.typography
    val dims = SSHFTPTheme.dimensions

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.primary)
            .neoBorder(width = dims.borderWidth, color = colors.border)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            navigationIcon?.invoke()
            Spacer(Modifier.width(8.dp))
            Column {
                Text(text = title, style = typography.h3, color = Color.White)
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        if (isConnected) Color(0xFF00CC66) else Color(0xFFFF3366),
                        shape = RoundedCornerShape(50),
                    )
                    .border(1.dp, Color.White, RoundedCornerShape(50))
            )
            Text(
                text = if (isConnected) "CONNECTED" else "OFFLINE",
                style = typography.label,
                color = Color.White,
            )
        }
    }
}

// ─── NeoTabRow ────────────────────────────────────────────────────────────────
@Composable
fun NeoTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    icons: List<ImageVector> = emptyList(),
) {
    val colors = SSHFTPTheme.colors
    val dims = SSHFTPTheme.dimensions

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .neoBorder(dims.borderWidth, colors.border),
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (selected) colors.primary else Color.Transparent)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (icons.size > index) {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = tab,
                            tint = if (selected) Color.White else colors.text,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(
                        text = tab,
                        style = SSHFTPTheme.typography.label,
                        color = if (selected) Color.White else colors.text,
                    )
                }
            }
            if (index < tabs.size - 1) {
                Box(
                    modifier = Modifier
                        .width(dims.borderWidth)
                        .height(56.dp)
                        .background(colors.border),
                )
            }
        }
    }
}

// ─── LoadingSpinner ───────────────────────────────────────────────────────────
@Composable
fun NeoLoadingSpinner(color: Color = Color(0xFF3366FF)) {
    CircularProgressIndicator(
        modifier = Modifier.size(36.dp),
        color = color,
        strokeWidth = 4.dp,
    )
}

// ─── NeoChip ──────────────────────────────────────────────────────────────────
@Composable
fun NeoChip(text: String, color: Color = Color(0xFFFFCC00)) {
    val colors = SSHFTPTheme.colors
    Box(
        modifier = Modifier
            .neoBorder(2.dp, colors.border)
            .background(color)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = SSHFTPTheme.typography.label,
            color = colors.text,
        )
    }
}
