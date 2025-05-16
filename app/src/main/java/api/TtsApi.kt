package api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class TTSRequest(
    val sentence: String
)

data class TTSResponse(
    val sentence: String,
    val historyId: Int,
    val audio_base64: String
)

interface TtsApi {
    @POST("tts")
    suspend fun generateTTS(
        @Header("Authorization") token: String,
        @Body request: TTSRequest
    ): Response<TTSResponse>
}