package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiMemeSuggester {
    private const val TAG = "GeminiMemeSuggester"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    private val moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Converte um Bitmap para Base64 JPEG em tamanho otimizado para API multimodal
     */
    fun bitmapToBase64(bitmap: Bitmap, maxDim: Int = 512, quality: Int = 80): String {
        val width = bitmap.width
        val height = bitmap.height
        val scale = if (width > maxDim || height > maxDim) {
            val max = width.coerceAtLeast(height).toFloat()
            maxDim / max
        } else 1.0f

        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        val scaled = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Analisa o conteúdo visual da foto/figurinha e gera legendas de memes brasileiras autênticas
     */
    suspend fun suggestMemeCaptions(bitmap: Bitmap): List<GeminiMemeSuggestion> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured. Falling back to local smart suggestions.")
            return@withContext getLocalFallbackSuggestions()
        }

        try {
            val base64Image = bitmapToBase64(bitmap)

            val prompt = """
                Analise esta imagem que será transformada em uma figurinha de WhatsApp (sticker).
                Gere exatamente 5 legendas de meme curtas, engraçadas e espirituosas em português do Brasil,
                baseadas na expressão facial, postura, objeto ou situação retratada na foto.
                
                Instruções:
                - Use gírias da internet brasileira (ex: "Não tankei", "Perdi tudo", "Rindo de nervoso", "Foco no objetivo", "Nem guindaste", "Passada").
                - Cada frase deve ter no máximo 5 a 6 palavras para caber perfeito em uma figurinha.
                - Responda no formato:
                [EMOJI] CATEGORIA: LEGENDA
                Exemplo:
                😂 Zueira: NÃO TANKEI ESSA
                🔥 Deboche: HABLA MAIS ALTO
                🤔 Dúvida: SERÁ QUE VALE A PENA?
                😴 Preguiça: SÓ AMANHÃ DE MANHÃ
                ❤️ Amor: MEU DENGUINHO
            """.trimIndent()

            val request = GeminiGenerateContentRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = prompt),
                            GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Image))
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.85f,
                    maxOutputTokens = 800
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(
                        GeminiPart(
                            text = "Você é um criador de memes brasileiro e especialista em figurinhas virais de WhatsApp."
                        )
                    )
                )
            )

            val response = service.generateMemeCaptions(apiKey, request)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (!rawText.isNullOrBlank()) {
                val parsed = parseSuggestions(rawText)
                if (parsed.isNotEmpty()) {
                    return@withContext parsed
                }
            }

            getLocalFallbackSuggestions()
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Gemini API: ${e.message}", e)
            getLocalFallbackSuggestions()
        }
    }

    private fun parseSuggestions(rawText: String): List<GeminiMemeSuggestion> {
        val list = mutableListOf<GeminiMemeSuggestion>()
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        for (line in lines) {
            val cleanLine = line.removePrefix("-").removePrefix("*").trim()
            // Pattern like: 😂 Categoria: Frase or 1. 😂 Categoria: Frase
            val regex = Regex("""^(\d+[\.\)]\s*)?([\p{So}\p{Sk}\u2000-\u32ff\uD83C-\uDBFF\uDC00-\uDFFF])?\s*([^:]+):\s*(.+)$""")
            val match = regex.find(cleanLine)

            if (match != null) {
                val emoji = match.groupValues[2].ifBlank { "✨" }
                val category = match.groupValues[3].trim()
                val caption = match.groupValues[4].trim().replace("\"", "").replace("'", "")
                list.add(GeminiMemeSuggestion(caption = caption, category = category, emoji = emoji))
            } else if (cleanLine.contains(":")) {
                val parts = cleanLine.split(":", limit = 2)
                val cat = parts[0].trim().take(15)
                val text = parts[1].trim().replace("\"", "")
                list.add(GeminiMemeSuggestion(caption = text, category = cat, emoji = "💬"))
            } else if (cleanLine.length in 3..40) {
                list.add(GeminiMemeSuggestion(caption = cleanLine, category = "IA Gemini", emoji = "🤖"))
            }
        }

        return if (list.isEmpty()) getLocalFallbackSuggestions() else list.take(6)
    }

    fun getLocalFallbackSuggestions(): List<GeminiMemeSuggestion> {
        return listOf(
            GeminiMemeSuggestion("NÃO TANKEI ESSA", "Humor", "😂"),
            GeminiMemeSuggestion("EU AVISEI NÉ", "Reação", "😏"),
            GeminiMemeSuggestion("MEU DEUS DO CÉU", "Surpresa", "😱"),
            GeminiMemeSuggestion("RINDO DE NERVOSO", "Desespero", "🥲"),
            GeminiMemeSuggestion("HABLA MESMO", "Deboche", "🔥"),
            GeminiMemeSuggestion("CADÊ O PIX?", "Finanças", "💸")
        )
    }
}
