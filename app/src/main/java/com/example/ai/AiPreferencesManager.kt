package com.example.ai

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GeminiModelOption(
    val id: String,
    val name: String,
    val description: String,
    val badge: String
)

class AiPreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "ai_accounting_prefs"
        private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
        private const val KEY_SELECTED_MODEL = "selected_gemini_model"
        private const val KEY_USE_CUSTOM_KEY = "use_custom_api_key"
        private const val KEY_TEMPERATURE = "ai_temperature"

        const val DEFAULT_MODEL = "gemini-3.5-flash"
        const val DEFAULT_TEMPERATURE = 0.1f

        val AVAILABLE_MODELS = listOf(
            GeminiModelOption(
                id = "gemini-3.5-flash",
                name = "Gemini 3.5 Flash",
                description = "النموذج الافتراضي الموصى به: فائق السرعة والدقة في فهم المعاملات والمحاسبة والتقارير",
                badge = "الافتراضي والموصى به"
            ),
            GeminiModelOption(
                id = "gemini-3.1-pro-preview",
                name = "Gemini 3.1 Pro",
                description = "نموذج التفكير والتحليل المتقدم: قدرات استنتاج استثنائية للاستشارات المالية والتحليلات العميقة",
                badge = "تحليل عميق وتفكير"
            ),
            GeminiModelOption(
                id = "gemini-3.1-flash-lite-preview",
                name = "Gemini 3.1 Flash Lite",
                description = "نموذج خفيف وسريع جداً: استجابة فورية للمهام البسيطة وتسجيل العمليات وتوفير الحصص",
                badge = "خفيف وفائق السرعة"
            )
        )

        @Volatile
        private var instance: AiPreferencesManager? = null

        fun getInstance(context: Context): AiPreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: AiPreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val _customApiKey = MutableStateFlow(prefs.getString(KEY_CUSTOM_API_KEY, "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _selectedModel = MutableStateFlow(
        prefs.getString(KEY_SELECTED_MODEL, null)?.let { saved ->
            if (saved == "gemini-3.5-flash") DEFAULT_MODEL else saved.ifBlank { DEFAULT_MODEL }
        } ?: DEFAULT_MODEL
    )
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _useCustomKey = MutableStateFlow(prefs.getBoolean(KEY_USE_CUSTOM_KEY, false))
    val useCustomKey: StateFlow<Boolean> = _useCustomKey.asStateFlow()

    private val _temperature = MutableStateFlow(prefs.getFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE))
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    fun setCustomApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString(KEY_CUSTOM_API_KEY, trimmed).apply()
        _customApiKey.value = trimmed
        if (trimmed.isNotBlank()) {
            setUseCustomKey(true)
        }
    }

    fun setSelectedModel(modelId: String) {
        val trimmed = modelId.trim()
        val validModel = if (trimmed.isNotBlank()) trimmed else DEFAULT_MODEL
        prefs.edit().putString(KEY_SELECTED_MODEL, validModel).apply()
        _selectedModel.value = validModel
    }

    fun setUseCustomKey(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USE_CUSTOM_KEY, enabled).apply()
        _useCustomKey.value = enabled
    }

    fun setTemperature(temp: Float) {
        val clamped = temp.coerceIn(0.0f, 1.0f)
        prefs.edit().putFloat(KEY_TEMPERATURE, clamped).apply()
        _temperature.value = clamped
    }

    fun getEffectiveApiKey(): String {
        val custom = _customApiKey.value.trim()
        if (_useCustomKey.value && custom.isNotBlank()) {
            return custom
        }
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return custom
    }

    fun getEffectiveModel(): String {
        val model = _selectedModel.value
        return if (model.isNotBlank()) model else DEFAULT_MODEL
    }

    fun hasValidKey(): Boolean {
        return getEffectiveApiKey().isNotBlank() && getEffectiveApiKey() != "MY_GEMINI_API_KEY"
    }

    fun clearCustomKey() {
        prefs.edit().remove(KEY_CUSTOM_API_KEY).putBoolean(KEY_USE_CUSTOM_KEY, false).apply()
        _customApiKey.value = ""
        _useCustomKey.value = false
    }
}
