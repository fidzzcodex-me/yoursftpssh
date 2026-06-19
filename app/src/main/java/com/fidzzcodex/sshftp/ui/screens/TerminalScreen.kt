package com.fidzzcodex.sshftp.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidzzcodex.sshftp.ui.AppViewModel
import com.fidzzcodex.sshftp.ui.components.*
import com.fidzzcodex.sshftp.ui.theme.NeoColors
import com.fidzzcodex.sshftp.ui.theme.SSHFTPTheme
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream

data class TermLine(val text: String, val type: LineType = LineType.OUTPUT)
enum class LineType { OUTPUT, INPUT, ERROR, SYSTEM }

@Composable
fun TerminalScreen(vm: AppViewModel) {
    val state by vm.uiState.collectAsState()

    if (!state.isConnected) {
        NotConnectedBanner()
        return
    }

    val lines = remember { mutableStateListOf<TermLine>() }
    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    var shellOut by remember { mutableStateOf<InputStream?>(null) }
    var shellIn by remember { mutableStateOf<OutputStream?>(null) }
    val history = remember { mutableStateListOf<String>() }
    var historyIdx by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // Open shell on first composition
    LaunchedEffect(state.isConnected) {
        if (state.isConnected) {
            val manager = vm.getSSHManager() ?: return@LaunchedEffect
            val (inp, out) = manager.openShell() ?: return@LaunchedEffect
            shellOut = inp
            shellIn = out
            lines.add(TermLine("Connected to ${state.connectedUser}@${state.connectedHost}", LineType.SYSTEM))
            lines.add(TermLine("─".repeat(40), LineType.SYSTEM))

            // Read output in background
            withContext(Dispatchers.IO) {
                val buffer = ByteArray(4096)
                var sb = StringBuilder()
                try {
                    while (true) {
                        val n = inp.read(buffer)
                        if (n == -1) break
                        val chunk = String(buffer, 0, n, Charsets.UTF_8)
                        sb.append(chunk)
                        // flush lines
                        val text = sb.toString()
                        val newlineIdx = text.lastIndexOf('\n')
                        if (newlineIdx >= 0) {
                            val toProcess = text.substring(0, newlineIdx + 1)
                            sb = StringBuilder(text.substring(newlineIdx + 1))
                            val newLines = toProcess.split("\n")
                                .filter { it.isNotEmpty() }
                                .map { TermLine(stripAnsi(it)) }
                            withContext(Dispatchers.Main) {
                                lines.addAll(newLines)
                                scope.launch {
                                    listState.animateScrollToItem(maxOf(0, lines.size - 1))
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        lines.add(TermLine("Connection closed.", LineType.SYSTEM))
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { vm.getSSHManager()?.closeShell() }
    }

    fun sendCommand(cmd: String) {
        if (cmd.isEmpty()) return
        shellIn?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    it.write((cmd + "\n").toByteArray(Charsets.UTF_8))
                    it.flush()
                } catch (_: Exception) {}
            }
        }
        if (cmd.isNotBlank()) {
            history.remove(cmd)
            history.add(0, cmd)
            if (history.size > 100) history.removeLastOrNull()
        }
        historyIdx = -1
        inputText = TextFieldValue("")
    }

    fun sendRaw(bytes: ByteArray) {
        shellIn?.let { scope.launch(Dispatchers.IO) { runCatching { it.write(bytes); it.flush() } } }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(NeoColors.Black)
    ) {
        // Terminal output
        Box(Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(lines) { line ->
                    Text(
                        text = line.text,
                        style = SSHFTPTheme.typography.mono.copy(
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        ),
                        color = when (line.type) {
                            LineType.OUTPUT -> Color(0xFF00FF88)
                            LineType.INPUT  -> Color(0xFFFFCC00)
                            LineType.ERROR  -> NeoColors.Red
                            LineType.SYSTEM -> Color(0xFF888888)
                        },
                        softWrap = true,
                    )
                }
                // Input line
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("$ ", style = SSHFTPTheme.typography.mono.copy(fontSize = 13.sp),
                            color = NeoColors.Blue)
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            textStyle = SSHFTPTheme.typography.mono.copy(
                                fontSize = 13.sp, color = NeoColors.Yellow,
                            ),
                            cursorBrush = SolidColor(NeoColors.Yellow),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            singleLine = true,
                        )
                    }
                }
            }
        }

        // Custom key row
        TerminalKeyRow(
            onCtrlC   = { sendRaw(byteArrayOf(3)) },
            onCtrlD   = { sendRaw(byteArrayOf(4)) },
            onTab     = { sendRaw(byteArrayOf(9)) },
            onEsc     = { sendRaw(byteArrayOf(27)) },
            onUp      = {
                if (history.isNotEmpty()) {
                    historyIdx = minOf(historyIdx + 1, history.size - 1)
                    val cmd = history[historyIdx]
                    inputText = TextFieldValue(cmd, TextRange(cmd.length))
                }
            },
            onDown    = {
                if (historyIdx > 0) {
                    historyIdx--
                    val cmd = history[historyIdx]
                    inputText = TextFieldValue(cmd, TextRange(cmd.length))
                } else {
                    historyIdx = -1
                    inputText = TextFieldValue("")
                }
            },
            onEnter   = { sendCommand(inputText.text) },
            onArrowLeft  = { sendRaw(byteArrayOf(27, 91, 68)) },
            onArrowRight = { sendRaw(byteArrayOf(27, 91, 67)) },
        )

        // Function key row
        FunctionKeyRow { key ->
            when (key) {
                "F1"  -> sendRaw(byteArrayOf(27, 79, 80))
                "F2"  -> sendRaw(byteArrayOf(27, 79, 81))
                "F3"  -> sendRaw(byteArrayOf(27, 79, 82))
                "F4"  -> sendRaw(byteArrayOf(27, 79, 83))
                "F5"  -> sendRaw(byteArrayOf(27, 91, 49, 53, 126))
                "F6"  -> sendRaw(byteArrayOf(27, 91, 49, 55, 126))
                "F7"  -> sendRaw(byteArrayOf(27, 91, 49, 56, 126))
                "F8"  -> sendRaw(byteArrayOf(27, 91, 49, 57, 126))
                "F9"  -> sendRaw(byteArrayOf(27, 91, 50, 48, 126))
                "F10" -> sendRaw(byteArrayOf(27, 91, 50, 49, 126))
                "F11" -> sendRaw(byteArrayOf(27, 91, 50, 51, 126))
                "F12" -> sendRaw(byteArrayOf(27, 91, 50, 52, 126))
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(200)
        runCatching { focusRequester.requestFocus() }
    }
}

@Composable
fun TerminalKeyRow(
    onCtrlC: () -> Unit,
    onCtrlD: () -> Unit,
    onTab: () -> Unit,
    onEsc: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onArrowLeft: () -> Unit,
    onArrowRight: () -> Unit,
    onEnter: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E))
            .border(1.dp, Color(0xFF333333))
            .horizontalScroll(rememberScrollState())
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val keys = listOf(
            "ESC" to onEsc,
            "CTRL+C" to onCtrlC,
            "CTRL+D" to onCtrlD,
            "TAB" to onTab,
            "↑" to onUp,
            "↓" to onDown,
            "←" to onArrowLeft,
            "→" to onArrowRight,
            "ENTER" to onEnter,
        )
        keys.forEach { (label, action) ->
            TermKey(label = label, onClick = action,
                highlight = label == "ENTER" || label == "CTRL+C")
        }
    }
}

@Composable
fun FunctionKeyRow(onKey: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF181818))
            .horizontalScroll(rememberScrollState())
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        (1..12).forEach { n ->
            TermKey(label = "F$n", onClick = { onKey("F$n") }, small = true)
        }
    }
}

@Composable
fun TermKey(label: String, onClick: () -> Unit, highlight: Boolean = false, small: Boolean = false) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .height(if (small) 28.dp else 36.dp)
            .defaultMinSize(minWidth = if (small) 32.dp else 44.dp)
            .background(
                when {
                    isPressed  -> NeoColors.Blue
                    highlight  -> Color(0xFF2A2A2A)
                    else       -> Color(0xFF252525)
                }
            )
            .border(1.dp, if (highlight) NeoColors.Blue.copy(.5f) else Color(0xFF444444))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = SSHFTPTheme.typography.monoSmall.copy(fontSize = if (small) 10.sp else 11.sp),
            color = if (highlight) NeoColors.Yellow else Color(0xFFCCCCCC),
        )
    }
}

private fun stripAnsi(text: String): String =
    text.replace(Regex("\u001B\\[[;\\d]*[A-Za-z]"), "")
        .replace(Regex("\u001B\\(B"), "")
        .replace("\r", "")
