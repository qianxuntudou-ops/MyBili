package com.tutu.myblbl.repository.remote

import com.tutu.myblbl.model.BaseResponse
import com.tutu.myblbl.model.user.ScanQrModel
import com.tutu.myblbl.model.user.SignInResultModel
import com.tutu.myblbl.network.NetworkManager
import com.tutu.myblbl.network.api.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val apiService: ApiService = NetworkManager.apiService) {

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

    fun getCsrfToken(): String = NetworkManager.getCsrfToken()

    fun isLoggedIn(): Boolean = NetworkManager.isLoggedIn()
}
