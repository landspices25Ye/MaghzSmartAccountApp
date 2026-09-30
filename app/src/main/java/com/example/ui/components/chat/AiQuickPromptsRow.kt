package com.example.ui.components.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryGreen

@Composable
fun AiQuickPromptsRow(
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prompts = listOf(
        "كم رصيدي اليوم وصافي ثروتي؟",
        "كشف حساب حركة اليوم",
        "صرفت 60 ريال وقود من الصندوق",
        "استلمت 500 من العميل محمد",
        "ماذا تتذكر عني في ذاكرتك؟",
        "أعلى بنود المصروفات هذا الشهر",
        "تذكر أن خالد شريكي بنسبة 30%"
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(prompts) { prompt ->
            AssistChip(
                onClick = { onPromptSelected(prompt) },
                label = { Text(prompt, fontSize = 11.sp) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = AssistChipDefaults.assistChipBorder(
                    borderColor = PrimaryGreen.copy(alpha = 0.3f),
                    enabled = true
                )
            )
        }
    }
}
