package com.tutu.myblbl.repository.remote

import com.tutu.myblbl.model.BaseResponse
import com.tutu.myblbl.model.common.CollectionResultModel
import com.tutu.myblbl.model.favorite.CheckFavoriteModel
import com.tutu.myblbl.model.favorite.FavoriteFolderDetailWrapper
import com.tutu.myblbl.model.favorite.FavoriteFoldersWrapper
import com.tutu.myblbl.model.favorite.FolderDetailModel
import com.tutu.myblbl.network.api.ApiService
import com.tutu.myblbl.network.security.NetworkSecurityGateway
import com.tutu.myblbl.network.session.NetworkSessionGateway

@Suppress("SpellCheckingInspection")
class FavoriteRepository(
    private val apiService: ApiService,
    private val sessionGateway: NetworkSessionGateway,
    private val securityGateway: NetworkSecurityGateway
) {

    private companion object {
        const val WEB_ACTION_SPMID = "333.788.0.0"
        const val WEB_ACTION_FROM_SPMID = "333.1007.tianma.1-2-2.click"
        const val WEB_ACTION_STATISTICS = "{\"appId\":100,\"platform\":5}"
    }

    @Suppress("unused")
    suspend fun checkFavorite(aid: Long?): Result<BaseResponse<CheckFavoriteModel>> =
        runCatching {
            sessionGateway.executeWithRiskControlRetry(
                key = "check_fav_$aid",
                source = "favorite.checkFavorite"
            ) {
                sessionGateway.syncAuthState(
                    apiService.checkFavorite(aid),
                    source = "favorite.checkFavorite"
                )
            }
        }

    suspend fun getFavoriteFolders(upMid: Long, rid: Long? = null): Result<BaseResponse<FavoriteFoldersWrapper>> =
        runCatching {
            sessionGateway.executeWithRiskControlRetry(
                key = "fav_folders_$upMid",
                source = "favorite.getFavoriteFolders"
            ) {
                sessionGateway.syncAuthState(
                    apiService.getFavoriteFolders(upMid, rid = rid),
                    source = "favorite.getFavoriteFolders"
                )
            }
        }

    suspend fun getFavoriteFolderInfo(mediaId: Long): Result<BaseResponse<FolderDetailModel>> =
        runCatching {
            sessionGateway.executeWithRiskControlRetry(
                key = "fav_info_$mediaId",
                source = "favorite.getFavoriteFolderInfo"
            ) {
                sessionGateway.syncAuthState(
                    apiService.getFavoriteFolderInfo(mediaId),
                    source = "favorite.getFavoriteFolderInfo"
                )
            }
        }

    suspend fun getFavoriteFolderDetail(
        mediaId: Long,
        page: Int,
        pageSize: Int
    ): Result<BaseResponse<FavoriteFolderDetailWrapper>> =
        runCatching {
            sessionGateway.executeWithRiskControlRetry(
                key = "fav_detail_${mediaId}_$page",
                source = "favorite.getFavoriteFolderDetail"
            ) {
                sessionGateway.syncAuthState(
                    apiService.getFavoriteFolderDetail(mediaId, page, pageSize),
                    source = "favorite.getFavoriteFolderDetail"
                )
            }
        }

    suspend fun addFavorite(rid: Long, addMediaIds: String): Result<BaseResponse<CollectionResultModel>> =
        runCatching {
            securityGateway.ensureHealthyForPlay()
            val csrf = sessionGateway.requireCsrfToken()
                ?: return Result.success(BaseResponse(code = -111, message = "csrf token is blank"))
            sessionGateway.executeWithRiskControlRetry(
                key = "fav_add_$rid",
                source = "favorite.addFavorite"
            ) {
                sessionGateway.syncAuthState(
                    apiService.dealFavorite(buildFavoriteDealForm(
                        rid = rid,
                        addMediaIds = addMediaIds,
                        delMediaIds = null,
                        csrf = csrf
                    )),
                    source = "favorite.addFavorite"
                )
            }
        }

    suspend fun removeFavorite(rid: Long, delMediaIds: String): Result<BaseResponse<CollectionResultModel>> =
        runCatching {
            securityGateway.ensureHealthyForPlay()
            val csrf = sessionGateway.requireCsrfToken()
                ?: return Result.success(BaseResponse(code = -111, message = "csrf token is blank"))
            sessionGateway.executeWithRiskControlRetry(
                key = "fav_remove_$rid",
                source = "favorite.removeFavorite"
            ) {
                sessionGateway.syncAuthState(
                    apiService.dealFavorite(buildFavoriteDealForm(
                        rid = rid,
                        addMediaIds = null,
                        delMediaIds = delMediaIds,
                        csrf = csrf
                    )),
                    source = "favorite.removeFavorite"
                )
            }
        }

    private fun buildFavoriteDealForm(
        rid: Long,
        addMediaIds: String?,
        delMediaIds: String?,
        csrf: String
    ): Map<String, String> {
        return linkedMapOf(
            "rid" to rid.toString(),
            "type" to "2",
            "add_media_ids" to addMediaIds.orEmpty(),
            "del_media_ids" to delMediaIds.orEmpty(),
            "platform" to "web",
            "from_spmid" to WEB_ACTION_FROM_SPMID,
            "spmid" to WEB_ACTION_SPMID,
            "statistics" to WEB_ACTION_STATISTICS,
            "csrf" to csrf
        )
    }
}
