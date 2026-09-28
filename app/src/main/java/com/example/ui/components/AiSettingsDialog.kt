package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.AiPreferencesManager
import com.example.ui.theme.MoneyExpenseRed
import com.example.ui.theme.MoneyIncomeGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.AccountingViewModel
import kotlinx.coroutines.launch

@Composable
fun AiSettingsDialog(
    viewModel: AccountingViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsManager = viewModel.aiPreferences

    val currentKey by prefsManager.customApiKey.collectAsStateWithLifecycle()
    val currentModel by prefsManager.selectedModel.collectAsStateWithLifecycle()
    val currentUseCustom by prefsManager.useCustomKey.collectAsStateWithLifecycle()
    val currentTemp by prefsManager.temperature.collectAsStateWithLifecycle()

    var apiKeyInput by remember(currentKey) { mutableStateOf(currentKey) }
    var selectedModelId by remember(currentModel) { mutableStateOf(currentModel) }
    var useCustomKeyEnabled by remember(currentUseCustom) { mutableStateOf(currentUseCustom) }
    var temperatureValue by remember(currentTemp) { mutableFloatStateOf(currentTemp) }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultSuccess by remember { mutableStateOf<String?>(null) }
    var testResultError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val finalModel = selectedModelId.trim().ifBlank { AiPreferencesManager.DEFAULT_MODEL }
                    prefsManager.setCustomApiKey(apiKeyInput.trim())
                    prefsManager.setSelectedModel(finalModel)
                    prefsManager.setUseCustomKey(useCustomKeyEnabled || apiKeyInput.isNotBlank())
                    prefsManager.setTemperature(temperatureValue)
                    Toast.makeText(context, "تم حفظ إعدادات الذكاء الاصطناعي بنجاح", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                modifier = Modifier.testTag("save_ai_settings_button")
            ) {
                Text("حفظ التغييرات", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "إعدادات الذكاء الاصطناعي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Google AI Studio & Models",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "مفتاح Google AI Studio API Key",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "احصل على مفتاحك مجاناً وبسرعة من منصة جوجل للذكاء الاصطناعي لتشغيل كافة ميزات المحاسب الذكي بحسابك الخاص.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // AI Studio link button
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("الحصول على مفتاح من aistudio.google.com", fontSize = 12.sp)
                }

                // API Key Field
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "مفتاح API الخاص بك:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            testResultSuccess = null
                            testResultError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input_field"),
                        placeholder = { Text("AIzaSy...", fontSize = 13.sp) },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (apiKeyInput.isNotBlank()) {
                                    IconButton(onClick = { apiKeyInput = "" }) {
                                        Icon(Icons.Default.Delete, contentDescription = "مسح", modifier = Modifier.size(18.dp))
                                    }
                                }
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "إظهار/إخفاء",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Test Connection Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val keyToTest = apiKeyInput.trim().ifBlank { prefsManager.getEffectiveApiKey() }
                            val modelToTest = selectedModelId.trim().ifBlank { AiPreferencesManager.DEFAULT_MODEL }
                            isTestingConnection = true
                            testResultSuccess = null
                            testResultError = null
                            scope.launch {
                                val result = viewModel.testGeminiConnection(keyToTest, modelToTest)
                                isTestingConnection = false
                                result.onSuccess { msg ->
                                    testResultSuccess = msg
                                }.onFailure { err ->
                                    testResultError = err.localizedMessage ?: "فشل الاتصال"
                                }
                            }
                        },
                        enabled = !isTestingConnection && (apiKeyInput.isNotBlank() || prefsManager.hasValidKey()),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("test_ai_connection_button")
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("جاري الاختبار...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اختبار الاتصال والمفتاح", fontSize = 12.sp)
                        }
                    }

                    if (currentKey.isNotBlank()) {
                        TextButton(
                            onClick = {
                                prefsManager.clearCustomKey()
                                apiKeyInput = ""
                                Toast.makeText(context, "تم مسح المفتاح والعودة للافتراضي", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("استعادة الافتراضي", fontSize = 11.sp, color = MoneyExpenseRed)
                        }
                    }
                }

                // Test Results Feedback
                testResultSuccess?.let { successMsg ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MoneyIncomeGreen.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MoneyIncomeGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(successMsg, fontSize = 12.sp, color = MoneyIncomeGreen, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                testResultError?.let { errorMsg ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MoneyExpenseRed.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = MoneyExpenseRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(errorMsg, fontSize = 12.sp, color = MoneyExpenseRed, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Section 2: Model Selection & Custom Model Input
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نموذج الذكاء الاصطناعي (Model):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (selectedModelId.trim() != AiPreferencesManager.DEFAULT_MODEL) {
                            TextButton(
                                onClick = { selectedModelId = AiPreferencesManager.DEFAULT_MODEL },
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("الافتراضي", fontSize = 11.sp, color = PrimaryGreen)
                            }
                        }
                    }
                    Text(
                        text = "يمكنك كتابة اسم أي نموذج تريده بحرية، أو الاختيار من النماذج السريعة أدناه:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Custom model name editable text field
                    OutlinedTextField(
                        value = selectedModelId,
                        onValueChange = {
                            selectedModelId = it
                            testResultSuccess = null
                            testResultError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("model_name_input_field"),
                        label = { Text("اسم النموذج (Custom Model Name)") },
                        placeholder = { Text("مثال: gemini-3.5-flash-lite", fontSize = 13.sp) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (selectedModelId.isNotBlank()) {
                                IconButton(onClick = { selectedModelId = "" }) {
                                    Icon(Icons.Default.Delete, contentDescription = "مسح اسم النموذج", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "النماذج المعتمدة بالقائمة (انقر لاختيار النموذج):",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    AiPreferencesManager.AVAILABLE_MODELS.forEach { model ->
                        val isSelected = selectedModelId.trim().equals(model.id, ignoreCase = true)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedModelId = model.id },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PrimaryGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryGreen) else null,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = model.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = model.badge,
                                                fontSize = 10.sp,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = model.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        lineHeight = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "المعرف: ${model.id}",
                                        fontSize = 10.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = PrimaryGreen.copy(alpha = 0.85f)
                                    )
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryGreen),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Precision / Temperature
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "درجة الدقة (Temperature):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = String.format("%.2f", temperatureValue),
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "القيمة المنخفضة (0.10) تضمن أعلى دقة والتزام بالأرقام المحاسبية الصارمة.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Slider(
                        value = temperatureValue,
                        onValueChange = { temperatureValue = it },
                        valueRange = 0.0f..1.0f,
                        steps = 9
                    )
                }

                // Security Note
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "يتم حفظ المفتاح محلياً على جهازك بشكل آمن ولا يشارك مع أي جهة خارجية.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    )
}
