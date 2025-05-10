package api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// 로그인 요청 데이터 클래스
data class LoginRequest(
    val email: String,
    val password: String
)

// 로그인 응답 데이터 클래스
data class LoginResponse(
    val token: String  // 로그인 후 반환되는 토큰
)

interface LoginApi {
    @POST("auth/login")  // 실제 서버의 로그인 API 경로로 수정해야 합니다
    fun login(@Body request: LoginRequest): Call<LoginResponse>
}