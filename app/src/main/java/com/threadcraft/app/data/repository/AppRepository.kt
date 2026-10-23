package com.threadcraft.app.data.repository

import com.threadcraft.app.ThreadCraftApp
import com.threadcraft.app.data.local.SecurePrefs
import com.threadcraft.app.data.model.AiMessage
import com.threadcraft.app.data.model.AiRequest
import com.threadcraft.app.data.model.PostHistory
import com.threadcraft.app.data.remote.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

class AppRepository(private val prefs: SecurePrefs) {

    private val dao = ThreadCraftApp.instance.database.postHistoryDao()
    private val aiService = RetrofitClient.aiService
    private val threadsService = RetrofitClient.threadsService

    fun getAllPosts(): Flow<List<PostHistory>> = dao.getAll()

    suspend fun savePost(post: PostHistory): Long = dao.insert(post)

    suspend fun updatePost(post: PostHistory) = dao.update(post)

    suspend fun getPost(id: Long) = dao.getById(id)

    suspend fun deletePost(id: Long) = dao.deleteById(id)

    suspend fun getDueScheduled(now: Long) = dao.getDueScheduled(now)

    suspend fun generateContent(
        productTitle: String,
        description: String,
        affiliateLink: String,
        style: String,
        numParts: Int?
    ): List<String> {
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

        val baseUrl = prefs.aiBaseUrl.trimEnd('/')
        val url = "$baseUrl/chat/completions"

        val response = aiService.generateCompletion(
            url = url,
            authHeader = "Bearer ${prefs.aiApiKey}",
            request = AiRequest(
                model = prefs.aiModel,
                messages = listOf(
                    AiMessage("system", systemPrompt),
                    AiMessage("user", userPrompt)
                )
            )
        )

        val content = response.choices.firstOrNull()?.message?.content ?: ""
        val parts = content.split("===PART===")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return parts.mapIndexed { index, part ->
            "${index + 1}/${parts.size} $part"
        }
    }

    suspend fun postToThreads(parts: List<String>): List<String> {
        val userId = prefs.threadsUserId
        val token = prefs.threadsAccessToken
        val postedIds = mutableListOf<String>()

        for ((index, text) in parts.withIndex()) {
            val replyToId = postedIds.lastOrNull()

            val createResponse = threadsService.createThread(
                userId = userId,
                text = text,
                accessToken = token,
                replyToId = replyToId
            )

            // Wait for container to be ready
            delay(2000)

            val publishResponse = threadsService.publishThread(
                userId = userId,
                creationId = createResponse.id,
                accessToken = token
            )

            postedIds.add(publishResponse.id)

            // Rate limit between posts
            if (index < parts.size - 1) {
                delay(3000)
            }
        }

        return postedIds
    }

    suspend fun exchangeToken(code: String): Pair<String, String> {
        val redirectUri = "https://threadcraft.app/callback"
        val response = threadsService.getAccessToken(
            clientId = prefs.threadsAppId,
            clientSecret = prefs.threadsAppSecret,
            redirectUri = redirectUri,
            code = code
        )
        prefs.threadsAccessToken = response.access_token
        prefs.threadsUserId = response.user_id.toString()

        val profile = threadsService.getProfile(accessToken = response.access_token)
        prefs.threadsUsername = profile.username

        return Pair(profile.username, response.user_id.toString())
    }
}
