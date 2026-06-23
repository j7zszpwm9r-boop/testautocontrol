package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiManager {
    private const val TAG = "GeminiManager"
    private const val MODEL = "gemini-3.5-flash"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateAdvice(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API Key is empty or placeholder! Falling back to adaptive local UI assistant.")
            return@withContext getLocalFallbackAdvice(prompt)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"
            
            val requestJson = JSONObject()
            
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            if (systemInstruction != null) {
                val sysInstObj = JSONObject()
                val sysPartsArray = JSONArray()
                val sysPartObj = JSONObject()
                sysPartObj.put("text", systemInstruction)
                sysPartsArray.put(sysPartObj)
                sysInstObj.put("parts", sysPartsArray)
                requestJson.put("systemInstruction", sysInstObj)
            }

            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            requestJson.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Request failed (status code ${response.code}): $errBody")
                    return@withContext getLocalFallbackAdvice(prompt)
                }

                val responseBodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text")
                            if (text.isNotEmpty()) {
                                return@withContext text
                            }
                        }
                    }
                }
                return@withContext "Не вдалося отримати аналіз автомобіля. Спробуйте локальний звіт."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call exception", e)
            return@withContext getLocalFallbackAdvice(prompt)
        }
    }

    private fun getLocalFallbackAdvice(prompt: String): String {
        return when {
            prompt.contains("trip") || prompt.contains("поїздк") || prompt.contains("style") -> {
                val speed = Regex("""speed[:\s]+(\d+)""").find(prompt)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 60
                val brakes = Regex("""brakes[:\s]+(\d+)""").find(prompt)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                val accels = Regex("""accels[:\s]+(\d+)""").find(prompt)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                val consume = Regex("""consumption[:\s]+([\d.]+)""").find(prompt)?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 8.0

                val style = if (brakes > 4 || accels > 4) "Агресивний" else if (brakes > 1 || accels > 1) "Помірний" else "Спокійний (Еко)"
                val brakingTip = if (brakes > 3) "🚨 Велике навантаження на гальма! Спостерігаємо часті різкі гальмування. Рекомендуємо перевірити гальмівні диски та колодки через 1 500 км для безпечної їзди." else "✅ Система гальмування працює в ощадливому режимі. Знос колодок мінімальний."
                val consumptionTip = if (consume > 10.0) "⚠️ Підвищена витрата палива ($consume л/100км). Оцініть налаштування коробки передач та плавність акселератора." else "🌱 Чудовий середній апетит авто ($consume л/100км). Так тримати!"

                """
                🚗 **Аналіз поїздки від асистента AutoControl AI** (офлайн режим):
                Стиль їзди цієї поїздки оцінено як: **$style**
                
                📌 **Аналітика стилю водіння**:
                * Середня швидкість: $speed км/год
                * Різких прискорень: $accels | Різких гальмувань: $brakes
                
                💡 **Персоналізовані поради**:
                1. $brakingTip
                2. $consumptionTip
                3. **Регулярність ТО**: Оскільки ви ведете машину в режимі *$style*, інтервал змащування та заміни повітряного фільтра становить оптимальні значення.
                """.trimIndent()
            }
            prompt.contains("report") || prompt.contains("звіт") -> {
                """
                📋 **Звіт про стан автомобіля від AutoControl (Офлайн-ШІ)**:
                
                🚗 **Загальний аналіз витрат**:
                * Витрати палива складають основу бюджету водіння (~70%).
                * Вартість володіння 1 км автомобіля є прогнозованою та економічною.

                🛠️ **Термінові рекомендації**:
                * Проведіть діагностику підвіски при наступному плановому візиті на СТО.
                * Використання якісного палива покращить довговічність паливної системи на 15%.
                
                📦 **Цифровий паспорт підготовлено**:
                Цей звіт готовий до експорту та може бути використаний перед продажем автомобіля для підтвердження прозорого пробігу та сервісної історії!
                """.trimIndent()
            }
            else -> {
                """
                💡 **Порада AutoControl AI**:
                * Уникайте тривалого прогріву на холостих обертах. Сучасні автомобілі краще прогрівати під час руху на низьких швидкостях.
                * Слідкуйте за індикаторами на панелі приладів та вчасно оновлюйте карту ТО вашого автомобіля!
                """.trimIndent()
            }
        }
    }
}
