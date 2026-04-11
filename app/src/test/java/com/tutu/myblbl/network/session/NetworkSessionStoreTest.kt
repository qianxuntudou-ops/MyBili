package com.tutu.myblbl.network.session

import com.tutu.myblbl.model.BaseResponse
import com.tutu.myblbl.model.user.UserDetailInfoModel
import com.tutu.myblbl.model.user.WbiImg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkSessionStoreTest {

    @Test
    fun updateUserSession_normalizes_avatar_and_extracts_wbi_keys() {
        val store = NetworkSessionStore(authInvalidCode = -101)

        store.updateUserSession(
            UserDetailInfoModel(
                face = "//i0.hdslb.com/avatar.jpg",
                wbiImg = WbiImg(
                    imgUrl = "https://i0.hdslb.com/bfs/wbi/abc123.png",
                    subUrl = "https://i0.hdslb.com/bfs/wbi/def456.png"
                )
            )
        )

        assertEquals("https://i0.hdslb.com/avatar.jpg", store.getUserInfo()?.face)
        assertEquals(Pair("abc123", "def456"), store.getWbiKeys())
    }

    @Test
    fun clearUserSession_resets_user_and_wbi_state() {
        val store = NetworkSessionStore(authInvalidCode = -101)
        store.updateUserSession(
            UserDetailInfoModel(
                face = "http://i0.hdslb.com/avatar.jpg",
                wbiImg = WbiImg(
                    imgUrl = "https://i0.hdslb.com/bfs/wbi/abc123.png",
                    subUrl = "https://i0.hdslb.com/bfs/wbi/def456.png"
                )
            )
        )

        store.clearUserSession()

        assertNull(store.getUserInfo())
        assertEquals(Pair("", ""), store.getWbiKeys())
    }

    @Test
    fun syncUserSession_clears_state_and_invokes_callback_on_auth_invalid() {
        val store = NetworkSessionStore(authInvalidCode = -101)
        var callbackInvoked = false
        store.updateUserSession(UserDetailInfoModel(face = "https://i0.hdslb.com/avatar.jpg"))

        val result = store.syncUserSession(
            response = BaseResponse(code = -101, message = "expired"),
            onAuthInvalid = { callbackInvoked = true }
        )

        assertNull(result)
        assertNull(store.getUserInfo())
        assertTrue(callbackInvoked)
    }
}
