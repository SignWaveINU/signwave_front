package api

import retrofit2.http.GET


data class TranslationHistory(
    val translationHistoryId: Int,
    val translatedText: String,
    val favorite: Boolean
)

interface HistoryApi {
    @GET("translation/history") // ← 엔드포인트에 맞게 수정하세요
    suspend fun getTranslationHistory(): List<TranslationHistory>
}