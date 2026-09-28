package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AiMemoryFact
import com.example.data.model.MemoryCategory
import com.example.ui.theme.MoneyExpenseRed
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.AccountingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMemoryDialog(
    viewModel: AccountingViewModel,
    onDismiss: () -> Unit
) {
    val memories by viewModel.aiMemories.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedCategoryFilter by remember { mutableStateOf<MemoryCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }
    var memoryToEdit by remember { mutableStateOf<AiMemoryFact?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    // Filter memories
    val filteredMemories = memories.filter { mem ->
        val matchesCategory = selectedCategoryFilter == null || mem.category == selectedCategoryFilter
        val matchesSearch = searchQuery.isBlank() ||
                mem.key.contains(searchQuery, ignoreCase = true) ||
                mem.fact.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "ذاكرة المحاسب",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ذاكرة المحاسب الذكي",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = PrimaryGreen,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${memories.count { it.isActive }} نشطة",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "الحقائق والقواعد والتفضيلات التي يتذكرها المحاسب ويتفاعل معك بناءً عليها",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Row {
                    if (memories.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearAllConfirm = true },
                            modifier = Modifier.testTag("clear_all_memories_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "مسح كل الذاكرة",
                                tint = MoneyExpenseRed
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Bar: Add new button + Search
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showAddForm = !showAddForm },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showAddForm) MaterialTheme.colorScheme.secondaryContainer else PrimaryGreen,
                        contentColor = if (showAddForm) MaterialTheme.colorScheme.onSecondaryContainer else Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("toggle_add_memory_btn")
                ) {
                    Icon(
                        imageVector = if (showAddForm) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showAddForm) "إلغاء" else "إضافة معلومة للذاكرة", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث في الذاكرة...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("search_memory_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Expandable Add Form
            AnimatedVisibility(
                visible = showAddForm,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                AddMemoryInlineCard(
                    onSave = { key, fact, category ->
                        viewModel.addAiMemory(key, fact, category)
                        showAddForm = false
                    },
                    onCancel = { showAddForm = false }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null },
                        label = { Text("الكل (${memories.size})", fontSize = 11.sp) }
                    )
                }
                items(MemoryCategory.values()) { cat ->
                    val count = memories.count { it.category == cat }
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = getCategoryIcon(cat),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = { Text("${cat.titleAr} ($count)", fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Memories List
            if (filteredMemories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (memories.isEmpty()) "ذاكرة المحاسب نظيفة حالياً" else "لا توجد نتائج مطابقة للبحث",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "أثناء المحادثة الصوتية أو النصية، سيقوم المحاسب بحفظ الحقائق تلقائياً، أو يمكنك إخباره مباشرة: 'تذكر أن المحل اسمه متجر الأمانة' أو الضغط على زر إضافة معلومة أعلاه.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredMemories, key = { it.id }) { memory ->
                        MemoryFactItemCard(
                            memory = memory,
                            onToggleActive = { isActive ->
                                viewModel.toggleAiMemoryActive(memory.id, isActive)
                            },
                            onEdit = { memoryToEdit = memory },
                            onDelete = { viewModel.deleteAiMemory(memory) }
                        )
                    }
                }
            }
        }
    }

    // Edit Memory Dialog
    if (memoryToEdit != null) {
        EditMemoryDialog(
            memory = memoryToEdit!!,
            onDismiss = { memoryToEdit = null },
            onSave = { updatedKey, updatedFact, updatedCategory, isActive ->
                viewModel.updateAiMemory(memoryToEdit!!, updatedKey, updatedFact, updatedCategory, isActive)
                memoryToEdit = null
            }
        )
    }

    // Confirm Clear All Dialog
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MoneyExpenseRed) },
            title = { Text("مسح كل ذاكرة المحاسب؟") },
            text = { Text("سيتم مسح جميع الحقائق والتوجيهات المحفوظة ولن يتذكر المحاسب القواعد السابقة.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllAiMemories()
                        showClearAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MoneyExpenseRed)
                ) {
                    Text("نعم، مسح الكل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun MemoryFactItemCard(
    memory: AiMemoryFact,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (memory.isActive) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (memory.isActive) PrimaryGreen.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("memory_card_${memory.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top row: Category badge + Auto/Manual tag + Toggle Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = getCategoryColor(memory.category).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(memory.category),
                                contentDescription = null,
                                tint = getCategoryColor(memory.category),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = memory.category.titleAr,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = getCategoryColor(memory.category)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        color = if (memory.isAutoLearned) Color(0xFF6750A4).copy(alpha = 0.12f) else Color(0xFF00668B).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (memory.isAutoLearned) "🤖 ذكاء اصطناعي" else "👤 يدوي",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (memory.isAutoLearned) Color(0xFF6750A4) else Color(0xFF00668B),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (memory.isActive) "مفعلة" else "معطلة",
                        fontSize = 11.sp,
                        color = if (memory.isActive) PrimaryGreen else MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = memory.isActive,
                        onCheckedChange = onToggleActive,
                        modifier = Modifier.size(32.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Key and Fact
            Text(
                text = memory.key,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (memory.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = memory.fact,
                style = MaterialTheme.typography.bodyMedium,
                color = if (memory.isActive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Timestamp + Edit & Delete Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateFormat.format(Date(memory.timestamp)),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = MoneyExpenseRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddMemoryInlineCard(
    onSave: (key: String, fact: String, category: MemoryCategory) -> Unit,
    onCancel: () -> Unit
) {
    var keyInput by remember { mutableStateOf("") }
    var factInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(MemoryCategory.ACCOUNTING_RULE) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "➕ إضافة قاعدة أو معلومة جديدة لذاكرة المحاسب",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Picker
            Text(text = "التصنيف:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(MemoryCategory.values()) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.titleAr, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("عنوان المعلومة (مثال: شريك الأرباح، بنزين سيارة التوزيع)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = factInput,
                onValueChange = { factInput = it },
                label = { Text("المعلومة أو التوجيه (مثال: خالد شريك بنسبة 30%، مصروفات البنزين تخصم من بنك الراجحي)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("إلغاء")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (factInput.isNotBlank()) {
                            val key = if (keyInput.isNotBlank()) keyInput.trim() else factInput.take(25)
                            onSave(key, factInput.trim(), selectedCategory)
                        }
                    },
                    enabled = factInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حفظ في الذاكرة")
                }
            }
        }
    }
}

@Composable
fun EditMemoryDialog(
    memory: AiMemoryFact,
    onDismiss: () -> Unit,
    onSave: (key: String, fact: String, category: MemoryCategory, isActive: Boolean) -> Unit
) {
    var keyInput by remember(memory) { mutableStateOf(memory.key) }
    var factInput by remember(memory) { mutableStateOf(memory.fact) }
    var selectedCategory by remember(memory) { mutableStateOf(memory.category) }
    var isActiveState by remember(memory) { mutableStateOf(memory.isActive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryGreen) },
        title = { Text("تعديل المعلومة في الذاكرة") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(MemoryCategory.values()) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.titleAr, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("عنوان المعلومة") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = factInput,
                    onValueChange = { factInput = it },
                    label = { Text("محتوى المعلومة المحفوظة") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تضمين في المحاسب (مفعلة)", fontSize = 12.sp)
                    Switch(
                        checked = isActiveState,
                        onCheckedChange = { isActiveState = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = PrimaryGreen)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (factInput.isNotBlank()) {
                        onSave(keyInput.trim().ifBlank { factInput.take(25) }, factInput.trim(), selectedCategory, isActiveState)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("حفظ التعديلات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

fun getCategoryIcon(category: MemoryCategory) = when (category) {
    MemoryCategory.PARTY_NOTE -> Icons.Default.Person
    MemoryCategory.BUSINESS_INFO -> Icons.Default.Store
    MemoryCategory.ACCOUNTING_RULE -> Icons.Default.Tune
    MemoryCategory.USER_PREFERENCE -> Icons.Default.FilterList
    MemoryCategory.GENERAL -> Icons.Default.Lightbulb
}

fun getCategoryColor(category: MemoryCategory) = when (category) {
    MemoryCategory.PARTY_NOTE -> Color(0xFF00668B)
    MemoryCategory.BUSINESS_INFO -> Color(0xFF984061)
    MemoryCategory.ACCOUNTING_RULE -> PrimaryGreen
    MemoryCategory.USER_PREFERENCE -> Color(0xFF6750A4)
    MemoryCategory.GENERAL -> Color(0xFF7D5260)
}
