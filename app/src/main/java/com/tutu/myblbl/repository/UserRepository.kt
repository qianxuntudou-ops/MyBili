package com.tutu.myblbl.repository

import com.tutu.myblbl.model.BaseResponse
import com.tutu.myblbl.model.user.UserInfoModel
import com.tutu.myblbl.model.user.UserStatModel
import com.tutu.myblbl.model.user.UserSpaceInfo
import com.tutu.myblbl.model.user.UserDetailInfoModel
import com.tutu.myblbl.model.user.CheckRelationModel
import com.tutu.myblbl.model.user.GetFollowUserWrapper
import com.tutu.myblbl.model.video.HistoryListResponse
import com.tutu.myblbl.model.video.LaterWatchWrapper
import com.tutu.myblbl.model.video.UserDynamicResponse
import com.tutu.myblbl.model.video.AllDynamicResponse
import com.tutu.myblbl.network.NetworkManager
import com.tutu.myblbl.network.WbiGenerator
import com.tutu.myblbl.network.response.BaseBaseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository {
    
    private val apiService = NetworkManager.apiService
    
    suspend fun getUserDetailInfo(): BaseResponse<UserDetailInfoModel> {
        return apiService.getUserDetailInfo().also { response ->
            NetworkManager.syncUserSession(response, source = "getUserDetailInfo")
        }
    }
    
    suspend fun getUserStat(): BaseResponse<UserStatModel> {
        return apiService.getUserStat().also { response ->
            NetworkManager.handleAuthFailureCode(response.code, source = "getUserStat")
        }
    }

    suspend fun getRelationStat(mid: Long): BaseResponse<UserStatModel> {
        return apiService.getRelationStat(mid)
    }
    
    suspend fun getUserSpace(mid: Long): BaseResponse<UserSpaceInfo> {
        val params = mapOf("mid" to mid.toString())
        val wbiKeys = NetworkManager.getWbiKeys()
        val signedParams = WbiGenerator.generateWbiParams(params, wbiKeys.first, wbiKeys.second)
        return NetworkManager.syncAuthState(
            apiService.getUserSpace(signedParams),
            source = "getUserSpace"
        )
    }
    
    fun isLoggedIn(): Boolean {
        return NetworkManager.isLoggedIn()
    }
    
    suspend fun getHistory(viewAt: Long, pageSize: Int): Result<BaseResponse<HistoryListResponse>> =
        withContext(Dispatchers.IO) {
            runCatching {
                NetworkManager.syncAuthState(
                    apiService.getHistory(viewAt, pageSize),
                    source = "getHistory"
                )
            }
        }

    suspend fun getLaterWatch(): Result<BaseResponse<LaterWatchWrapper>> =
        withContext(Dispatchers.IO) {
            runCatching {
                NetworkManager.syncAuthState(
                    apiService.getLaterWatch(),
                    source = "getLaterWatch"
                )
            }
        }

    suspend fun getFollowing(
        mid: Long,
        page: Int = 1,
        pageSize: Int = 50
    ): Result<BaseResponse<GetFollowUserWrapper>> =
        withContext(Dispatchers.IO) {
            runCatching {
                NetworkManager.syncAuthState(
                    apiService.getFollowing(mid, page, pageSize),
                    source = "getFollowing"
                )
            }
        }

    suspend fun getFollower(
        mid: Long,
        page: Int = 1,
        pageSize: Int = 50
    ): Result<BaseResponse<GetFollowUserWrapper>> =
        withContext(Dispatchers.IO) {
            runCatching {
                NetworkManager.syncAuthState(
                    apiService.getFollower(mid, page, pageSize),
                    source = "getFollower"
                )
            }
        }

    suspend fun checkUserRelation(
        mid: Long
    ): Result<BaseResponse<CheckRelationModel>> =
        withContext(Dispatchers.IO) {
            runCatching {
                NetworkManager.syncAuthState(
                    apiService.checkUserRelation(mid.toString()),
                    source = "checkUserRelation"
                )
            }
        }

    suspend fun modifyRelation(
        fid: Long,
        action: Int
    ): Result<BaseBaseResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val params = mapOf(
                    "fid" to fid.toString(),
                    "act" to action.toString(),
                    "csrf" to NetworkManager.getCsrfToken()
                )
                NetworkManager.syncAuthState(
                    apiService.userRelationModify(params),
                    source = "modifyRelation"
                )
            }
        }

    suspend fun getUserDynamic(
        mid: Long,
        page: Int,
        pageSize: Int = 20
    ): Result<BaseResponse<UserDynamicResponse>> =
        withContext(Dispatchers.IO) {
            runCatching {
                NetworkManager.syncAuthState(
                    apiService.getUserDynamic(mid, page, pageSize),
                    source = "getUserDynamic"
                )
            }
        }

    suspend fun getAllDynamic(
        page: Int,
        offset: Long? = null
    ): Result<BaseResponse<AllDynamicResponse>> =
        withContext(Dispatchers.IO) {
            runCatching {
                NetworkManager.syncAuthState(
                    apiService.getAllDynamic(page, offset),
                    source = "getAllDynamic"
                )
            }
        }

    suspend fun refreshCurrentUserInfo(): Result<UserDetailInfoModel> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = apiService.getUserDetailInfo()
                val info = NetworkManager.syncUserSession(
                    response,
                    source = "refreshCurrentUserInfo"
                )
                    ?: throw IllegalStateException(
                        response.errorMessage.ifEmpty { "加载用户信息失败" }
                    )
                info
            }
        }

    suspend fun resolveCurrentUserMid(): Result<Long> =
        withContext(Dispatchers.IO) {
            runCatching {
                NetworkManager.getUserInfo()?.mid
                    ?.takeIf { it > 0L }
                    ?: refreshCurrentUserInfo().getOrThrow().mid.takeIf { it > 0L }
                    ?: throw IllegalStateException("未获取到当前用户信息")
            }
        }
}
