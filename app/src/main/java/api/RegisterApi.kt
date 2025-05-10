import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class RegisterRequest(
    val email: String,
    val nickname: String,
    val password: String
)

data class RegisterResponse(
    val id: Int,
    val email: String,
    val nickname: String
)
interface RegisterApi {
    @POST("auth/signup") // 이 경로는 서버에 맞게 수정하세요
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>
}
