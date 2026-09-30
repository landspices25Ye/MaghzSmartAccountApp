package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AiMemoryDialog
import com.example.ui.components.AiSettingsDialog
import com.example.ui.components.VoiceTransactionDialog
import com.example.ui.components.chat.AiChatHeader
import com.example.ui.components.chat.AiChatInputBar
import com.example.ui.components.chat.AiChatMessageBubble
import com.example.ui.components.chat.AiQuickPromptsRow
import com.example.ui.viewmodel.AccountingViewModel

@Composable
fun AiChatScreen(viewModel: AccountingViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isThinking by viewModel.isAiThinking.collectAsStateWithLifecycle()
    val selectedModel by viewModel.aiPreferences.selectedModel.collectAsStateWithLifecycle()
    val activeMemoriesCount by viewModel.activeMemoriesCount.collectAsStateWithLifecycle()

    val parties by viewModel.parties.collectAsStateWithLifecycle()
    val cashBoxes by viewModel.cashBoxes.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val defaultCurrency by viewModel.defaultCurrency.collectAsStateWithLifecycle()
    val currencySymbol = defaultCurrency?.symbol?.ifBlank { "ر.س" } ?: "ر.س"

    var inputText by remember { mutableStateOf("") }
    var showAiSettings by remember { mutableStateOf(false) }
    var showMemoryDialog by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Automatically scroll to bottom on new messages
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            AiChatHeader(
                selectedModel = selectedModel,
                activeMemoriesCount = activeMemoriesCount,
                onOpenSettings = { showAiSettings = true },
                onOpenMemory = { showMemoryDialog = true },
                onClearChat = { showClearChatDialog = true }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.imePadding()) {
                AiQuickPromptsRow(
                    onPromptSelected = { prompt ->
                        viewModel.sendAiMessage(prompt)
                    }
                )
                AiChatInputBar(
                    inputText = inputText,
                    onInputTextChanged = { inputText = it },
                    onSendMessage = {
                        val textToSend = inputText.trim()
                        inputText = ""
                        viewModel.sendAiMessage(textToSend)
                    },
                    onVoiceClick = { showVoiceDialog = true },
                    isThinking = isThinking
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    AiChatMessageBubble(
                        message = msg,
                        parties = parties,
                        cashBoxes = cashBoxes,
                        transactions = transactions,
                        defaultCurrencySymbol = currencySymbol
                    )
                }

                if (isThinking) {
                    item {
                        com.example.ui.components.chat.AiThinkingIndicator()
                    }
                }
            }
        }
    }

    // Clear Chat Confirmation Dialog
    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = { Text("مسح المحادثة") },
            text = { Text("هل تريد مسح جميع رسائل الجلسة الحالية وبدء محادثة جديدة؟ (لن تتأثر الذاكرة والعمليات المسجلة).") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearChatDialog = false
                        viewModel.clearChatHistory()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("مسح")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Dialogs
    if (showAiSettings) {
        AiSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showAiSettings = false }
        )
    }

    if (showMemoryDialog) {
        AiMemoryDialog(
            viewModel = viewModel,
            onDismiss = { showMemoryDialog = false }
        )
    }

    if (showVoiceDialog) {
        VoiceTransactionDialog(
            viewModel = viewModel,
            onDismiss = { showVoiceDialog = false }
        )
    }
}
