package api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

data class SubmitResponse(
    val sentence: String
)

interface ApiService {
    @Multipart
    @POST("gesture-to-sentence")
    suspend fun submitLandmarks(
        @Header("Authorization") token: String,
        @Part csvFile: MultipartBody.Part
    ): Response<SubmitResponse>
}
