package api

import retrofit2.http.GET
import retrofit2.http.Header

data class TranslationHistory(
    val translationHistoryId: Int,
    val translatedText: String,
    val audioUrl: String,
    val createdTime: String,
    val favorite: Boolean
)

interface HistoryApi {
    @GET("history") // 로그인한 사용자의 전체 번역 기록 조회
    suspend fun getTranslationHistory(@Header("Authorization") token: String): List<TranslationHistory>
}