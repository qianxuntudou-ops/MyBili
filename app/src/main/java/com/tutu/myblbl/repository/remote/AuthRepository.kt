package com.tutu.myblbl.repository.remote

import com.tutu.myblbl.model.BaseResponse
import com.tutu.myblbl.model.user.ScanQrModel
import com.tutu.myblbl.model.user.SignInResultModel
import com.tutu.myblbl.network.api.ApiService
import com.tutu.myblbl.network.session.NetworkSessionGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val apiService: ApiService,
    private val sessionGateway: NetworkSessionGateway
) {

    suspend fun getQrCode(): Result<BaseResponse<ScanQrModel>> = withContext(Dispatchers.IO) {
        runCatching {
            apiService.getSignInQrCode()
        }
    }

    suspend fun checkSignInResult(qrcodeKey: String): Result<BaseResponse<SignInResultModel>> = withContext(Dispatchers.IO) {
        runCatching {
            apiService.checkSignInResult(qrcodeKey)
        }
    }

    fun getCsrfToken(): String = sessionGateway.getCsrfToken()

    fun isLoggedIn(): Boolean = sessionGateway.isLoggedIn()
}
