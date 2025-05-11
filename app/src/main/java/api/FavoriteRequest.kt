package api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class FavoriteRequest(
    val translationHistoryId: Int
)

data class FavoriteResponse(
    val memberId: Int
)

interface FavoriteApi {
    @POST("history/favorite")
    suspend fun addFavorite(
        @Header("Authorization") token: String,
        @Body request: FavoriteRequest
    ): FavoriteResponse
}