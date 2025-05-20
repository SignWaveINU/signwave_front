package api

import retrofit2.http.*

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

    @DELETE("history/favorite/{id}")
    suspend fun removeFavorite(
        @Header("Authorization") token: String,
        @Path("id") historyId: Int
    ): FavoriteResponse

    @GET("history/favorites")
    suspend fun getFavorites(
        @Header("Authorization") token: String
    ): List<TranslationHistory>
}