package com.threadcraft.app.data.repository

import android.content.Context
import com.threadcraft.app.ThreadCraftApp
import com.threadcraft.app.data.local.AppDatabase
import com.threadcraft.app.data.local.SecurePrefs
import com.threadcraft.app.data.model.AiMessage
import com.threadcraft.app.data.model.AiRequest
import com.threadcraft.app.data.model.PostHistory
import com.threadcraft.app.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class AppRepository(private val prefs: SecurePrefs, context: Context? = null) {

    private val appContext = context?.applicationContext ?: ThreadCraftApp.instance
    private val database by lazy { AppDatabase.getDatabase(appContext) }
    private val dao get() = database.postHistoryDao()
    private val aiService = RetrofitClient.aiService
    private val threadsService = RetrofitClient.threadsService

    fun getAllPosts(): Flow<List<PostHistory>> = dao.getAll()

    suspend fun savePost(post: PostHistory): Long = dao.insert(post)

    suspend fun updatePost(post: PostHistory) = dao.update(post)

    suspend fun getPost(id: Long) = dao.getById(id)

    suspend fun deletePost(id: Long) = dao.deleteById(id)

    suspend fun getDueScheduled(now: Long) = dao.getDueScheduled(now)

    private fun resolveChatCompletionUrl(rawUrl: String): String {
        var clean = rawUrl.trim()
        if (clean.isBlank()) throw IllegalArgumentException("AI Base URL belum diisi")
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://$clean"
        }
        return if (clean.endsWith("/chat/completions")) {
            clean
        } else if (clean.endsWith("/")) {
            "${clean}chat/completions"
        } else {
            "$clean/chat/completions"
        }
    }

    private fun cleanApiKey(rawKey: String): String {
        val clean = rawKey.trim().replace("\r", "").replace("\n", "")
        if (clean.isBlank()) throw IllegalArgumentException("API Key belum diisi")
        return clean
    }

    private fun extractContentFromAiResponse(rawBody: String): String {
        val cleanBody = rawBody.trim().removePrefix("\uFEFF")

        // 1. Standard full JSON
        if (cleanBody.startsWith("{")) {
            val json = JSONObject(cleanBody)
            if (json.has("error")) {
                val errObj = json.optJSONObject("error")
                val msg = errObj?.optString("message") ?: json.optString("error")
                throw IOException("AI Error: $msg")
            }
            val choices = json.optJSONArray("choices")
            val firstChoice = choices?.optJSONObject(0)
            val message = firstChoice?.optJSONObject("message")
            val content = message?.optString("content")?.trim() ?: ""
            if (content.isNotBlank()) {
                return content
            }
            val delta = firstChoice?.optJSONObject("delta")
            val deltaContent = delta?.optString("content")?.trim() ?: ""
            if (deltaContent.isNotBlank()) {
                return deltaContent
            }
        }

        // 2. Server-Sent Events (SSE) streaming format (lines with data: {...})
        val sseLines = cleanBody.lines()
        val sseBuilder = StringBuilder()
        var foundSse = false

        for (line in sseLines) {
            val trimmed = line.trim()
            if (!trimmed.startsWith("data:")) continue
            val dataStr = trimmed.removePrefix("data:").trim()
            if (dataStr == "[DONE]") break
            if (dataStr.isBlank() || !dataStr.startsWith("{")) continue

            try {
                val chunk = JSONObject(dataStr)
                foundSse = true
                if (chunk.has("error")) {
                    val errObj = chunk.optJSONObject("error")
                    val msg = errObj?.optString("message") ?: chunk.optString("error")
                    throw IOException("AI Error: $msg")
                }
                val choices = chunk.optJSONArray("choices")
                val firstChoice = choices?.optJSONObject(0)
                val delta = firstChoice?.optJSONObject("delta")
                val message = firstChoice?.optJSONObject("message")
                val chunkText = delta?.optString("content")
                    ?: message?.optString("content")
                    ?: ""
                sseBuilder.append(chunkText)
            } catch (e: IOException) {
                throw e
            } catch (_: Exception) {
            }
        }

        if (foundSse) {
            val text = sseBuilder.toString().trim()
            if (text.isNotBlank()) return text
            throw IOException("Model AI merespons chunk streaming kosong")
        }

        if (cleanBody.startsWith("{")) {
            throw IOException("Model merespons JSON tanpa teks: ${cleanBody.take(200)}")
        }

        throw IOException("Server merespons bukan JSON: ${cleanBody.take(200)}")
    }

    suspend fun generateContent(
        productTitle: String,
        description: String,
        affiliateLink: String,
        style: String,
        numParts: Int?
    ): List<String> = withContext(Dispatchers.IO) {
        val styleInstruction = when (style) {
            "Cerpen Storytelling" -> """
                Tulis cerita pendek yang menarik dan emosional tentang seseorang yang mengalami masalah,
                lalu menemukan solusi melalui produk ini. Gunakan narasi orang pertama, buat pembaca
                merasa relate. Akhiri dengan ajakan halus untuk mencoba produk.
            """.trimIndent()
            "Review Pengalaman" -> """
                Tulis review pengalaman pribadi yang jujur dan detail. Mulai dari masalah yang dihadapi,
                proses menemukan produk, pengalaman menggunakan, pro dan kontra, lalu kesimpulan
                dan rekomendasi. Gunakan bahasa santai seperti ngobrol dengan teman.
            """.trimIndent()
            "Thread Tips" -> """
                Buat thread berisi tips dan trik yang berhubungan dengan kategori produk ini.
                Setiap bagian berisi 1 tips actionable. Sisipkan produk sebagai salah satu
                rekomendasi secara natural di bagian tengah atau akhir thread.
            """.trimIndent()
            "Perbandingan Produk" -> """
                Buat thread perbandingan yang fair antara beberapa opsi (bisa fiktif),
                posisikan produk ini sebagai pilihan terbaik berdasarkan value. Gunakan format
                yang mudah dibaca dengan poin-poin perbandingan yang jelas.
            """.trimIndent()
            else -> "Tulis konten promosi kreatif untuk produk ini."
        }

        val partCount = numParts ?: 5
        val systemPrompt = """
Kamu adalah copywriter profesional untuk media sosial Threads (mirip Twitter).
Tugasmu membuat konten thread yang menarik untuk affiliate marketing.

ATURAN KETAT:
1. Buat TEPAT $partCount bagian thread
2. Setiap bagian MAKSIMAL 480 karakter (sisakan ruang untuk numbering)
3. Bagian pertama harus hook yang bikin orang berhenti scroll
4. Bagian terakhir harus CTA dengan link affiliate
5. Gunakan bahasa Indonesia yang natural dan engaging
6. JANGAN gunakan hashtag berlebihan, maksimal 2-3 di bagian terakhir
7. Sisipkan link affiliate ($affiliateLink) HANYA di bagian terakhir
8. Setiap bagian harus bisa berdiri sendiri tapi tetap nyambung
9. Gunakan emoji secukupnya untuk emphasis, jangan berlebihan

FORMAT OUTPUT:
Tulis setiap bagian dipisahkan dengan tanda ===PART=== (tanpa numbering, numbering akan ditambahkan otomatis)

GAYA PENULISAN:
$styleInstruction
        """.trimIndent()

        val userPrompt = """
Produk: $productTitle
Deskripsi: $description
Link Affiliate: $affiliateLink
Jumlah bagian: $partCount
        """.trimIndent()

        val url = resolveChatCompletionUrl(prefs.aiBaseUrl)
        val apiKey = cleanApiKey(prefs.aiApiKey)
        val model = prefs.aiModel.trim().ifBlank { "gpt-4o-mini" }

        val jsonPayload = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
            put("temperature", 0.7)
            put("max_tokens", 2500)
        }.toString()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonPayload.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val response = RetrofitClient.okHttpClient.newCall(request).execute()
        val rawBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorMsg = try {
                val errJson = JSONObject(rawBody)
                val errObj = errJson.optJSONObject("error")
                errObj?.optString("message")?.ifBlank { null }
                    ?: errJson.optString("message").ifBlank { null }
                    ?: rawBody.take(400)
            } catch (_: Exception) {
                rawBody.take(400)
            }
            throw IOException("HTTP ${response.code}: $errorMsg")
        }

        val content = extractContentFromAiResponse(rawBody)

        val rawParts = content.split("===PART===")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val parts = if (rawParts.size > 1) {
            rawParts
        } else {
            content.split("\n\n")
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }

        parts.mapIndexed { index, part ->
            "${index + 1}/${parts.size} $part"
        }
    }

    suspend fun postToThreads(parts: List<String>): List<String> = withContext(Dispatchers.IO) {
        val userId = prefs.threadsUserId.ifBlank { "me" }
        val token = prefs.threadsAccessToken.trim()
        if (token.isBlank()) {
            throw IOException("Token Threads kosong. Silakan login Threads di tab Pengaturan.")
        }
        val postedIds = mutableListOf<String>()

        for ((index, rawText) in parts.withIndex()) {
            val text = if (rawText.length > 500) rawText.take(497) + "..." else rawText
            val replyToId = postedIds.lastOrNull()

            val createResponse = try {
                threadsService.createThread(
                    userId = userId,
                    text = text,
                    accessToken = token,
                    replyToId = replyToId
                )
            } catch (e: retrofit2.HttpException) {
                val errBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null } ?: ""
                val msg = try {
                    val obj = JSONObject(errBody).optJSONObject("error")
                    val m = obj?.optString("message") ?: errBody
                    val code = obj?.optInt("code")
                    "$m (code $code)"
                } catch (_: Exception) {
                    errBody.take(300)
                }
                throw IOException("Gagal buat container (Bagian ${index + 1}/${parts.size}): HTTP ${e.code()} - $msg")
            }

            delay(2500)

            val publishResponse = try {
                threadsService.publishThread(
                    userId = userId,
                    creationId = createResponse.id,
                    accessToken = token
                )
            } catch (e: retrofit2.HttpException) {
                val errBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null } ?: ""
                val msg = try {
                    val obj = JSONObject(errBody).optJSONObject("error")
                    val m = obj?.optString("message") ?: errBody
                    val code = obj?.optInt("code")
                    "$m (code $code)"
                } catch (_: Exception) {
                    errBody.take(300)
                }
                throw IOException("Gagal publish (Bagian ${index + 1}/${parts.size}): HTTP ${e.code()} - $msg")
            }

            postedIds.add(publishResponse.id)

            if (index < parts.size - 1) {
                delay(3000)
            }
        }

        postedIds
    }

    suspend fun testAiConnection(): String = withContext(Dispatchers.IO) {
        val rawBaseUrl = prefs.aiBaseUrl
        val rawApiKey = prefs.aiApiKey
        val model = prefs.aiModel.trim().ifBlank { "gpt-4o-mini" }

        val url = resolveChatCompletionUrl(rawBaseUrl)
        val apiKey = cleanApiKey(rawApiKey)

        val jsonPayload = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "Hello! Reply with 'OK'.")
                })
            })
            put("max_tokens", 20)
            put("temperature", 0.1)
        }.toString()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonPayload.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val response = RetrofitClient.okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorMsg = try {
                val errJson = JSONObject(responseBody)
                val errObj = errJson.optJSONObject("error")
                errObj?.optString("message")?.ifBlank { null }
                    ?: errJson.optString("message").ifBlank { null }
                    ?: responseBody.take(200)
            } catch (_: Exception) {
                responseBody.take(200)
            }
            throw IOException("HTTP ${response.code}: $errorMsg")
        }

        val content = extractContentFromAiResponse(responseBody)
        if (content.isNotBlank()) {
            return@withContext content
        }
        return@withContext "OK (HTTP 200)"
    }

    suspend fun exchangeToken(code: String): Pair<String, String> = withContext(Dispatchers.IO) {
        val redirectUri = prefs.redirectUri.ifBlank { "https://threadcraft.app/callback" }
        val response = threadsService.getAccessToken(
            clientId = prefs.threadsAppId.trim(),
            clientSecret = prefs.threadsAppSecret.trim(),
            redirectUri = redirectUri,
            code = code.trim()
        )
        var finalToken = response.access_token
        try {
            val longResponse = threadsService.exchangeLongLivedToken(
                clientSecret = prefs.threadsAppSecret.trim(),
                accessToken = response.access_token
            )
            if (longResponse.access_token.isNotBlank()) {
                finalToken = longResponse.access_token
            }
        } catch (e: Exception) {
            android.util.Log.w("AppRepository", "Failed long-lived token exchange, fallback to short-lived", e)
        }

        prefs.threadsAccessToken = finalToken
        prefs.threadsUserId = response.user_id.toString()

        val profile = threadsService.getProfile(accessToken = finalToken)
        prefs.threadsUsername = profile.username
        if (profile.id.isNotBlank()) {
            prefs.threadsUserId = profile.id
        }

        Pair(profile.username, prefs.threadsUserId)
    }
}
