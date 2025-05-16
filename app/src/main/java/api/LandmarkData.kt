package api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class LandmarkData(
    val sequence: List<List<Float>>
)

data class SubmitResponse(
    val sentence: String
)

interface ApiService {
    @POST("gesture-to-sentence")
    suspend fun submitLandmarks(
        @Header("Authorization") token: String,
        @Body landmarkData: LandmarkData
    ): Response<SubmitResponse>
}
