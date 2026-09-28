package com.example.ai

import com.example.BuildConfig
import com.example.data.model.PartyType
import com.example.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAccountingService(
    private val preferencesManager: AiPreferencesManager? = null
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeAccountingMessage(
        userInput: String,
        existingParties: List<String>,
        existingCashBoxes: List<String>,
        defaultCashBoxName: String = "الصندوق الرئيسي",
        availableExpenseCategories: List<String> = emptyList(),
        availableCurrencies: List<String> = emptyList(),
        defaultCurrencySymbol: String = "ر.س",
        activeMemories: List<com.example.data.model.AiMemoryFact> = emptyList(),
        conversationHistory: List<Pair<String, Boolean>> = emptyList(), // Pair of (text, isUser)
        partiesWithBalances: List<String> = emptyList(),
        cashBoxesWithBalances: List<String> = emptyList(),
        customApiKey: String? = null,
        modelOverride: String? = null,
        temperatureOverride: Float? = null
    ): ParsedAccountingIntent? = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.ifBlank { null }
            ?: preferencesManager?.getEffectiveApiKey()
            ?: BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext null
        }

        val selectedModel = modelOverride?.ifBlank { null }
            ?: preferencesManager?.getEffectiveModel()
            ?: AiPreferencesManager.DEFAULT_MODEL

        val temp = temperatureOverride?.toDouble()
            ?: preferencesManager?.temperature?.value?.toDouble()
            ?: 0.1

        try {
            val categoriesStr = if (availableExpenseCategories.isNotEmpty()) availableExpenseCategories.joinToString(", ") else "إيجار, فواتير ومرافق, رواتب وأجور, نقل ومواصلات وبنزين, صيانة وتشغيل, ضيافة وبوفيه ومأكولات, بضاعة ومشتريات, تسويق وإعلانات, نثرية ومصروفات عامة"
            val currenciesStr = if (availableCurrencies.isNotEmpty()) availableCurrencies.joinToString(", ") else "ر.س (SAR), $ (USD), ر.ي (YER), ج.م (EGP), د.إ (AED), د.ك (KWD)"
            val partiesDetails = if (partiesWithBalances.isNotEmpty()) partiesWithBalances.joinToString("; ") else existingParties.joinToString(", ")
            val boxesDetails = if (cashBoxesWithBalances.isNotEmpty()) cashBoxesWithBalances.joinToString("; ") else existingCashBoxes.joinToString(", ")

            val memorySection = if (activeMemories.isNotEmpty()) {
                """
                🧠 ذاكرة المحاسب الذكي طويلة المدى (حقائق وقواعد محفوظة مسبقاً):
                ${activeMemories.joinToString("\n") { "• [${it.category.titleAr}] ${it.key}: ${it.fact}" }}
                (اعتمد هذه القواعد والحقائق بشكل طبيعي في فهم سياق العمليات والردود).
                """.trimIndent()
            } else {
                "🧠 ذاكرة المحاسب: لا توجد قواعد خاصة محفوظة بعد. احفظ أي حقائق يطلبها المستخدم."
            }

            val systemInstructionText = """
                أنت "المحاسب المالي الذكي" الخبير والمستشار المالي الودود لتطبيق محاسبة شخصي وتجاري متكامل.
                تتفاعل مع المستخدم بصورة طبيعية جداً، وتفهم السياق وتتذكر تاريخ المحادثة والحقائق المحفوظة في الذاكرة.

                سياق العمل والأرصدة الحالية:
                • الصناديق/الخزائن والأرصدة: $boxesDetails (الصندوق الافتراضي: $defaultCashBoxName)
                • الأطراف (العملاء والموردين) والأرصدة: $partiesDetails
                • بنود المصروفات المعتمدة: $categoriesStr
                • العملات المتاحة: $currenciesStr (العملة الافتراضية: $defaultCurrencySymbol)

                $memorySection

                قدراتك ومهامك:
                1. "TRANSACTION": تسجيل عملية مالية (قبض، صرف، دين لعميل، دين لمورد، مصروف، إيراد، تحويل).
                   - تنبأ ببند المصروف الأنسب تلقائياً من قائمة بنود المصروفات المعتمدة إذا كانت العملية EXPENSE.
                   - استخدم الصندوق الافتراضي والعملة الافتراضية إذا لم يحدد المستخدم غيرها، ما لم توجد قاعدة في الذاكرة تنص على خلاف ذلك.
                2. "QUERY" أو "ADVICE": استعلامات مالية، تحليل الأرصدة، تقديم نصائح للسيولة، كشف الحسابات، الإجابة عن أسئلة المستخدم حول وضعه المالي أو ديونه.
                3. "QUESTION": إذا كان الطلب غامضاً أو يحتاج تفصيلاً، اطرح سؤالاً توضيحياً ودياً.
                4. "REMEMBER": إذا طلب المستخدم تذكر أو حفظ معلومة (مثل "تذكر أن...", "احفظ عندك أن...", "أنا اسمي...", "محمد هو شريكي...", "دائماً ادفع للمورد فلان من بنك كذا") أو ذكر حقيقة مهمة عن نشاطه، قم بصياغة رد ودؤوب يؤكد حفظها وتعبئة كائن newMemory.
                5. "CHAT": محادثة طبيعية واستفسارات عامة وتحية ومناقشات مالية.

                أنواع العمليات المتاحة (transactionType):
                • CUSTOMER_RECEIPT: قبض نقدية من عميل (يزيد الصندوق وينقص دين العميل).
                • SUPPLIER_PAYMENT: دفع نقدية لمورد (ينقص الصندوق وينقص ما علينا للمورد).
                • CUSTOMER_NEW_DEBIT: دين جديد على عميل (ما لنا عنده، بيع آجل أو تسليف).
                • SUPPLIER_NEW_CREDIT: دين جديد من مورد (ما علينا له، شراء آجل).
                • EXPENSE: مصروف تشغيلي أو عام (ينقص الصندوق).
                • INCOME: إيراد نقدي خارجي (يزيد الصندوق).
                • TRANSFER: تحويل بين صندوقين (تحديد cashBoxName كمصدر و targetCashBoxName كوجهة).

                يجب أن يكون الرد بصيغة JSON حصراً بدون markdown code block بهذا الشكل:
                {
                  "action": "TRANSACTION" أو "QUERY" أو "QUESTION" أو "CHAT" أو "REMEMBER" أو "ADVICE",
                  "transactionType": "CUSTOMER_RECEIPT" أو "EXPENSE" أو null,
                  "partyName": "اسم الطرف إن وجد",
                  "partyType": "CUSTOMER" أو "SUPPLIER" أو null,
                  "cashBoxName": "اسم الصندوق أو $defaultCashBoxName",
                  "targetCashBoxName": "اسم الصندوق المحول له إن وجد",
                  "category": "بند المصروف المتنبأ به",
                  "currency": "$defaultCurrencySymbol أو العملة المحددة",
                  "amount": 0.0,
                  "description": "بيان مختصر وواضح",
                  "replyMessage": "رسالتك التفاعلية الذكية للمستخدم باللغة العربية بطريقة مهنية واضحة ومباشرة",
                  "newMemory": {
                    "category": "PARTY_NOTE" أو "BUSINESS_INFO" أو "ACCOUNTING_RULE" أو "USER_PREFERENCE" أو "GENERAL",
                    "key": "عنوان المعلومة",
                    "fact": "المعلومة التي يجب تذكرها مستقبلاً"
                  } (اختياري - فقط إذا تضمنت المحادثة معلومة جديدة لحفظها)
                }
            """.trimIndent()

            val contentsArray = JSONArray()

            // Add previous conversation turns for multi-turn contextual memory
            val recentTurns = conversationHistory.takeLast(10)
            for ((msgText, isUserTurn) in recentTurns) {
                if (msgText.isNotBlank()) {
                    contentsArray.put(JSONObject().apply {
                        put("role", if (isUserTurn) "user" else "model")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", msgText) })
                        })
                    })
                }
            }

            // Current turn
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", userInput) })
                })
            })

            val requestJson = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstructionText) })
                    })
                })
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", temp)
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$selectedModel:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext null
            }

            val respBody = response.body?.string() ?: return@withContext null
            val jsonRoot = JSONObject(respBody)
            val candidates = jsonRoot.optJSONArray("candidates") ?: return@withContext null
            if (candidates.length() == 0) return@withContext null

            val candidate = candidates.getJSONObject(0)
            val parts = candidate.getJSONObject("content").getJSONArray("parts")
            val rawText = parts.getJSONObject(0).getString("text")

            val parsedJson = JSONObject(rawText)
            val action = parsedJson.optString("action", "TRANSACTION")
            val typeStr = parsedJson.optString("transactionType", "")
            val type = try {
                if (typeStr.isNotBlank() && typeStr != "null") TransactionType.valueOf(typeStr) else null
            } catch (e: Exception) {
                null
            }

            val pTypeStr = parsedJson.optString("partyType", "")
            val partyType = try {
                if (pTypeStr.isNotBlank() && pTypeStr != "null") PartyType.valueOf(pTypeStr) else null
            } catch (e: Exception) {
                null
            }

            // Extract learned memory if present
            val newMemoryObj = parsedJson.optJSONObject("newMemory")
            val memCategory = newMemoryObj?.optString("category", "GENERAL")?.ifBlank { "GENERAL" }
            val memKey = newMemoryObj?.optString("key", "")?.ifBlank { null }
            val memFact = newMemoryObj?.optString("fact", "")?.ifBlank { null }

            ParsedAccountingIntent(
                action = action,
                transactionType = type,
                partyName = parsedJson.optString("partyName", "").takeIf { it.isNotBlank() && it != "null" },
                partyType = partyType,
                cashBoxName = parsedJson.optString("cashBoxName", defaultCashBoxName).takeIf { it.isNotBlank() && it != "null" },
                targetCashBoxName = parsedJson.optString("targetCashBoxName", "").takeIf { it.isNotBlank() && it != "null" },
                category = parsedJson.optString("category", "").takeIf { it.isNotBlank() && it != "null" } ?: "",
                currency = parsedJson.optString("currency", defaultCurrencySymbol).takeIf { it.isNotBlank() && it != "null" } ?: defaultCurrencySymbol,
                amount = parsedJson.optDouble("amount", 0.0),
                description = parsedJson.optString("description", "").takeIf { it != "null" } ?: "",
                replyMessage = parsedJson.optString("replyMessage", "تمت معالجة طلبك بنجاح"),
                learnedMemoryCategory = memCategory,
                learnedMemoryKey = memKey,
                learnedMemoryFact = memFact
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun testConnection(apiKey: String, model: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank() || trimmedKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("مفتاح API غير صالح أو فارغ. يرجى إدخال مفتاح تم إنشاؤه من Google AI Studio."))
        }

        try {
            val testPrompt = "Test accounting model connection. Reply with a short JSON: {\"status\":\"ok\"}"
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", testPrompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("maxOutputTokens", 50)
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$trimmedKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val start = System.currentTimeMillis()
            val response = client.newCall(request).execute()
            val latency = System.currentTimeMillis() - start

            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                val errorMsg = try {
                    val j = JSONObject(errBody)
                    j.optJSONObject("error")?.optString("message") ?: "رمز الخطأ: ${response.code}"
                } catch (e: Exception) {
                    "رمز الخطأ: ${response.code}"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            Result.success("تم الاتصال بنجاح بنموذج $model خلال ${latency}ms ✓")
        } catch (e: Exception) {
            Result.failure(Exception("فشل الاتصال: ${e.localizedMessage}"))
        }
    }

    suspend fun transcribeAudioBytes(
        audioBytes: ByteArray,
        mimeType: String = "audio/mp4",
        customApiKey: String? = null
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.ifBlank { null }
            ?: preferencesManager?.getEffectiveApiKey()
            ?: BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext null
        }

        try {
            val base64Audio = android.util.Base64.encodeToString(audioBytes, android.util.Base64.NO_WRAP)
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", mimeType)
                                    put("data", base64Audio)
                                })
                            })
                            put(JSONObject().apply {
                                put("text", "Listen carefully to this voice recording in Arabic or English. Transcribe the exact spoken words accurately into text. Reply with ONLY the transcribed text, without any conversational preamble or markdown formatting.")
                            })
                        })
                    })
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val model = preferencesManager?.getEffectiveModel() ?: AiPreferencesManager.DEFAULT_MODEL
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val respBody = response.body?.string() ?: return@withContext null
            val jsonResp = JSONObject(respBody)
            val candidates = jsonResp.optJSONArray("candidates") ?: return@withContext null
            val candidate = candidates.optJSONObject(0) ?: return@withContext null
            val content = candidate.optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null
            val textPart = parts.optJSONObject(0) ?: return@withContext null
            textPart.optString("text", "").trim().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
