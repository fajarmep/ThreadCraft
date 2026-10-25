package com.threadcraft.app.data.repository

import android.content.Context
import android.net.Uri
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class UploadedMedia(val url: String, val isVideo: Boolean)

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

    suspend fun claimForPosting(id: Long): Boolean = dao.claimForPosting(id) > 0

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
        tone: String = "Santai & Akrab",
        numParts: Int?
    ): List<String> = withContext(Dispatchers.IO) {
        val hasAffiliate = affiliateLink.isNotBlank()

        val toneInstruction = when (tone) {
            "Kasual & Hangat" -> "Gunakan bahasa kasual, ramah, hangat, dan bersahabat. Tutur kata santun namun dekat tanpa kaku."
            "Formal & Edukatif" -> "Gunakan bahasa Indonesia baku yang rapi, objektif, berbobot, dan profesional layaknya artikel literasi atau esai bermutu."
            "Emosional & Reflektif" -> "Gunakan bahasa yang puitis, penuh penghayatan mendalam, menyentuh hati, dan mengajak pembaca merenung secara personal."
            "Humoris & Cerdas" -> "Gunakan gaya bahasa santai yang jenaka, menghibur, celetukan cerdas, dan sedikit humor/satir ringan khas percakapan netizen."
            "Tegas & To The Point" -> "Gunakan kalimat lugas, padat, berani, to-the-point tanpa basa-basi berlebihan, langsung menusuk ke inti persoalan."
            else -> "Gunakan bahasa santai, akrab, lincah, dan mengalir natural khas obrolan media sosial Threads/Twitter."
        }

        val styleInstruction = if (hasAffiliate) {
            when (style) {
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
        } else {
            when (style) {
                "Cerpen Storytelling" -> """
                    Tulis cerita pendek yang mendalam, emosional, dan inspiratif seputar topik ini.
                    Gunakan narasi hidup (bisa sudut pandang orang pertama atau ketiga) sehingga
                    pembaca terbawa alur dan merasa terhubung. Bagian akhir berisi hikmah, perenungan hangat,
                    atau pertanyaan santai yang memicu pembaca berbagi cerita di kolom komentar.
                """.trimIndent()
                "Review Pengalaman" -> """
                    Tulis refleksi cerita atau studi kasus pengalaman nyata yang jujur dan kaya wawasan seputar topik ini.
                    Mulai dari latar belakang situasi, proses yang dilalui, pelajaran penting yang dipetik,
                    hingga perspektif berharga untuk pembaca. Gunakan gaya bertutur santai seperti berbagi kepada sahabat.
                """.trimIndent()
                "Thread Tips" -> """
                    Buat thread edukatif dan praktis berisi tips, wawasan, atau langkah-langkah bermanfaat seputar topik ini.
                    Setiap bagian menyajikan poin penting yang actionable dan berbobot tanpa ada unsur jualan sama sekali.
                """.trimIndent()
                "Perbandingan Produk" -> """
                    Buat thread eksplorasi dan analisis komparatif yang objektif mengenai topik ini dari berbagai sudut pandang.
                    Bantu pembaca melihat gambaran utuh, kelebihan, kekurangan, dan pertimbangan logis tanpa memihak secara komersial.
                """.trimIndent()
                else -> "Tulis thread artikel yang mengalir, edukatif, dan memberikan nilai nyata bagi pembaca."
            }
        }

        val partCount = numParts ?: 5
        val systemPrompt = if (hasAffiliate) {
            """
Kamu adalah copywriter profesional untuk media sosial Threads (mirip Twitter).
Tugasmu membuat konten thread yang menarik untuk affiliate marketing.

ATURAN KETAT:
1. Buat TEPAT $partCount bagian thread
2. Setiap bagian MAKSIMAL 480 karakter
3. JANGAN tambahkan penomoran seperti 1/5, 2/5, Part 1, atau Bagian 1 di awal teks bagian manapun
4. Bagian pertama harus hook yang bikin orang berhenti scroll
5. Bagian terakhir harus CTA dengan link affiliate yang diikuti kalimat penutup setelahnya
6. Gunakan bahasa Indonesia yang natural dan engaging
7. JANGAN gunakan hashtag (#) sama sekali. Dilarang menulis hashtag di seluruh bagian thread
8. Sisipkan link affiliate ($affiliateLink) di bagian terakhir, tapi DILARANG meletakkan link di ujung paling akhir naskah. Posisikan link di tengah naskah dan WAJIB sertakan 1-2 kalimat penutup setelah link (contohnya: pengingat klaim voucher/promo, tips singkat, atau ajakan diskusi santai).
9. Setiap bagian harus bisa berdiri sendiri tapi tetap nyambung
10. Gunakan emoji secukupnya untuk emphasis, jangan berlebihan

FORMAT OUTPUT:
Tulis setiap bagian murni hanya isi postingan tanpa label nomor apapun, dipisahkan dengan tanda ===PART===

GAYA FORMAT:
$styleInstruction

GAYA BAHASA & NADA BICARA (TONE):
$toneInstruction
            """.trimIndent()
        } else {
            """
Kamu adalah penulis dan pencerita ulung untuk media sosial Threads (mirip Twitter).
Tugasmu membuat thread artikel/cerita murni yang mendalam, edukatif, dan menarik pembaca TANPA ada unsur jualan atau affiliate.

ATURAN KETAT:
1. Buat TEPAT $partCount bagian thread
2. Setiap bagian MAKSIMAL 480 karakter
3. JANGAN tambahkan penomoran seperti 1/5, 2/5, Part 1, atau Bagian 1 di awal teks bagian manapun
4. Bagian pertama harus hook yang kuat dan bikin orang penasaran untuk membaca lanjutannya
5. Bagian terakhir adalah kesimpulan yang bermakna, refleksi mendalam, pesan moral, atau pertanyaan terbuka yang mengajak audiens berdiskusi santai di kolom komentar. DILARANG membuat ajakan membeli barang, mengklik link, atau cek bio.
6. Gunakan bahasa Indonesia yang luwes, natural, dan menyentuh emosi atau logika pembaca
7. JANGAN gunakan hashtag (#) sama sekali. Dilarang menulis hashtag di seluruh bagian thread
8. DILARANG MENYISIPKAN LINK APAPUN. DILARANG menyuruh klik link, membeli, atau promosi. Ini murni artikel/konten bernilai tinggi (value-first content).
9. Setiap bagian harus bisa berdiri sendiri tapi tetap menyambung runtut dari awal sampai akhir
10. Gunakan emoji secukupnya untuk penekanan rasa, jangan berlebihan

FORMAT OUTPUT:
Tulis setiap bagian murni hanya isi postingan tanpa label nomor apapun, dipisahkan dengan tanda ===PART===

GAYA FORMAT:
$styleInstruction

GAYA BAHASA & NADA BICARA (TONE):
$toneInstruction
            """.trimIndent()
        }

        val userPrompt = if (hasAffiliate) {
            """
Produk: $productTitle
Deskripsi: $description
Link Affiliate: $affiliateLink
Gaya Bahasa: $tone
Jumlah bagian: $partCount
            """.trimIndent()
        } else {
            """
Topik / Judul: $productTitle
Poin Bahasan / Ringkasan: $description
Gaya Bahasa: $tone
Jumlah bagian: $partCount
Mode: Artikel Murni (Non-komersial / Tanpa Link)
            """.trimIndent()
        }

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

        val cleaned = parts.map { cleanPartText(it) }.filter { it.isNotBlank() }
        val finalParts = if (cleaned.isNotEmpty()) cleaned.toMutableList() else parts.toMutableList()

        if (affiliateLink.isNotBlank() && finalParts.isNotEmpty()) {
            val lastIdx = finalParts.size - 1
            var lastPart = finalParts[lastIdx]
            val trimmedLink = affiliateLink.trim()
            if (!lastPart.contains(trimmedLink)) {
                lastPart = "$lastPart\n\nCek produknya di sini ya: $trimmedLink\nYuk jangan sampai kehabisan promonya!"
            } else if (lastPart.trimEnd().endsWith(trimmedLink)) {
                lastPart = "${lastPart.trimEnd()}\nYuk manfaatkan promo dan diskonnya sekarang!"
            }
            finalParts[lastIdx] = cleanPartText(lastPart)
        }

        finalParts
    }

    private fun cleanPartText(rawText: String): String {
        val withoutNumbering = rawText.replaceFirst(
            Regex("""^\s*(\d+\s*/\s*\d+|bagian\s*\d+|part\s*\d+|\d+[.)])\s*[:\-\.]?\s*""", RegexOption.IGNORE_CASE),
            ""
        ).trim()
        val withoutHashtags = withoutNumbering.replace(Regex("""#[a-zA-Z0-9_\p{L}]+"""), "").trim()
        val clean = withoutHashtags.replace(Regex("""[ \t]+"""), " ")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
        return if (clean.length > 500) clean.take(497) + "..." else clean
    }

    private suspend fun uploadMediaToPublicUrl(uri: Uri): UploadedMedia = withContext(Dispatchers.IO) {
        val contentResolver = appContext.contentResolver
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IOException("Gagal membuka file media")

        val bytes = inputStream.use { it.readBytes() }
        val rawMime = contentResolver.getType(uri) ?: ""
        val isVideo = rawMime.startsWith("video", ignoreCase = true) ||
            uri.toString().endsWith(".mp4", ignoreCase = true) ||
            uri.toString().endsWith(".mov", ignoreCase = true)

        val extension = when {
            isVideo -> "mp4"
            rawMime.contains("png", ignoreCase = true) -> "png"
            rawMime.contains("webp", ignoreCase = true) -> "webp"
            else -> "jpg"
        }
        val filename = "tc_${System.currentTimeMillis()}.$extension"
        val mediaType = (if (isVideo) "video/mp4" else if (rawMime.isNotBlank()) rawMime else "image/jpeg").toMediaType()

        val fastClient = RetrofitClient.okHttpClient.newBuilder()
            .connectTimeout(25, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val userAgent = "Mozilla/5.0 (Linux; Android 14; Mobile; ThreadsCraft)"

        // 1. Primary: pomf.lain.la (not blocked by Indonesian ISPs, permanent hosting)
        try {
            val reqBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("files[]", filename, bytes.toRequestBody(mediaType))
                .build()

            val req = Request.Builder()
                .url("https://pomf.lain.la/upload.php")
                .header("User-Agent", userAgent)
                .post(reqBody)
                .build()

            val resp = fastClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: ""
                val json = JSONObject(body)
                val files = json.optJSONArray("files")
                val url = files?.optJSONObject(0)?.optString("url")
                if (!url.isNullOrBlank()) {
                    return@withContext UploadedMedia(url.replace("\\/", "/"), isVideo)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("AppRepository", "pomf.lain.la upload failed: ${e.message}")
        }

        // 2. Fallback 1: uguu.se (Fast global CDN)
        try {
            val reqBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("files[]", filename, bytes.toRequestBody(mediaType))
                .build()

            val req = Request.Builder()
                .url("https://uguu.se/upload")
                .header("User-Agent", userAgent)
                .post(reqBody)
                .build()

            val resp = fastClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: ""
                val json = JSONObject(body)
                val files = json.optJSONArray("files")
                val url = files?.optJSONObject(0)?.optString("url")
                if (!url.isNullOrBlank()) {
                    return@withContext UploadedMedia(url.replace("\\/", "/"), isVideo)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("AppRepository", "uguu.se upload failed: ${e.message}")
        }

        // 3. Fallback 2: catbox.moe
        try {
            val reqBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("reqtype", "fileupload")
                .addFormDataPart("fileToUpload", filename, bytes.toRequestBody(mediaType))
                .build()

            val req = Request.Builder()
                .url("https://catbox.moe/user/api.php")
                .header("User-Agent", userAgent)
                .post(reqBody)
                .build()

            val resp = fastClient.newCall(req).execute()
            val rawResponse = resp.body?.string()?.trim() ?: ""
            if (resp.isSuccessful && rawResponse.startsWith("http")) {
                return@withContext UploadedMedia(rawResponse, isVideo)
            }
        } catch (e: Exception) {
            android.util.Log.w("AppRepository", "catbox.moe upload failed: ${e.message}")
        }

        throw IOException("Gagal upload media ke server hosting. Semua server upload gagal diakses.")
    }

    private suspend fun waitForContainerReady(containerId: String, token: String) {
        var attempts = 0
        while (attempts < 30) {
            delay(3000)
            attempts++
            try {
                val statusResp = threadsService.getContainerStatus(containerId = containerId, accessToken = token)
                val st = statusResp.status?.uppercase()
                if (st == "FINISHED") {
                    return
                }
                if (st == "ERROR" || st == "EXPIRED") {
                    throw IOException("Status media Threads gagal: $st (${statusResp.error_message ?: "Unknown error"})")
                }
            } catch (e: Exception) {
                if (e is IOException && e.message?.startsWith("Status media Threads gagal") == true) {
                    throw e
                }
                if (attempts >= 25) return
            }
        }
    }

    suspend fun postToThreads(parts: List<String>, mediaUris: List<Uri> = emptyList()): List<String> = withContext(Dispatchers.IO) {
        val userId = prefs.threadsUserId.ifBlank { "me" }
        val token = prefs.threadsAccessToken.trim()
        if (token.isBlank()) {
            throw IOException("Token Threads kosong. Silakan login Threads di tab Pengaturan.")
        }
        val postedIds = mutableListOf<String>()

        val publicMediaList = mutableListOf<UploadedMedia>()
        if (mediaUris.isNotEmpty()) {
            for (uri in mediaUris) {
                try {
                    val uploaded = uploadMediaToPublicUrl(uri)
                    publicMediaList.add(uploaded)
                } catch (e: Exception) {
                    throw IOException("Gagal upload media ke server: ${e.message}")
                }
            }
        }

        for ((index, rawText) in parts.withIndex()) {
            val text = cleanPartText(rawText)
            val replyToId = postedIds.lastOrNull()

            val createResponse = try {
                if (index == 0 && publicMediaList.size == 1) {
                    val m = publicMediaList.first()
                    if (m.isVideo) {
                        threadsService.createThread(
                            userId = userId,
                            mediaType = "VIDEO",
                            text = text,
                            videoUrl = m.url,
                            accessToken = token
                        )
                    } else {
                        threadsService.createThread(
                            userId = userId,
                            mediaType = "IMAGE",
                            text = text,
                            imageUrl = m.url,
                            accessToken = token
                        )
                    }
                } else if (index == 0 && publicMediaList.size > 1) {
                    val itemIds = mutableListOf<String>()
                    for (m in publicMediaList.take(10)) {
                        val itemContainer = if (m.isVideo) {
                            threadsService.createThread(
                                userId = userId,
                                mediaType = "VIDEO",
                                videoUrl = m.url,
                                isCarouselItem = true,
                                accessToken = token
                            )
                        } else {
                            threadsService.createThread(
                                userId = userId,
                                mediaType = "IMAGE",
                                imageUrl = m.url,
                                isCarouselItem = true,
                                accessToken = token
                            )
                        }
                        itemIds.add(itemContainer.id)
                        waitForContainerReady(itemContainer.id, token)
                        delay(1000)
                    }
                    delay(3000)
                    threadsService.createThread(
                        userId = userId,
                        mediaType = "CAROUSEL",
                        children = itemIds.joinToString(","),
                        text = text,
                        accessToken = token
                    )
                } else {
                    threadsService.createThread(
                        userId = userId,
                        mediaType = "TEXT",
                        text = text,
                        accessToken = token,
                        replyToId = replyToId
                    )
                }
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

            if (index == 0 && publicMediaList.isNotEmpty()) {
                waitForContainerReady(createResponse.id, token)
            } else {
                delay(2500)
            }

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
