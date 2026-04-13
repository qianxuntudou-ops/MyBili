package com.tutu.myblbl.ui.dialog

import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatDialog
import androidx.core.content.ContextCompat
import com.tutu.myblbl.R
import com.tutu.myblbl.databinding.DialogVideoCardMenuBinding
import com.tutu.myblbl.model.video.VideoModel
import com.tutu.myblbl.network.session.NetworkSessionGateway
import com.tutu.myblbl.repository.VideoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class VideoCardMenuDialog(
    context: Context,
    private val video: VideoModel,
    private val onDislikeVideo: (() -> Unit)? = null,
    private val onDislikeUp: ((String) -> Unit)? = null
) : AppCompatDialog(context, R.style.DialogTheme), KoinComponent {

    private val binding = DialogVideoCardMenuBinding.inflate(LayoutInflater.from(context))
    private val videoRepository: VideoRepository by inject()
    private val sessionGateway: NetworkSessionGateway by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var isInWatchLater = false

    init {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        setCanceledOnTouchOutside(true)
        binding.textTitle.text = video.title.take(50)
        initListeners()
        refreshWatchLaterState()
        setOnShowListener {
            binding.buttonWatchLater.requestFocus()
        }
    }

    private fun initListeners() {
        binding.root.setOnKeyListener { _, keyCode, event ->
            if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU)
                && event.action == KeyEvent.ACTION_UP
            ) {
                dismiss()
                true
            } else {
                false
            }
        }

        binding.buttonWatchLater.setOnClickListener {
            if (!checkLogin()) return@setOnClickListener
            if (isInWatchLater) {
                removeWatchLater()
            } else {
                addWatchLater()
            }
        }

        binding.buttonDislikeVideo.setOnClickListener {
            if (!checkLogin()) return@setOnClickListener
            scope.launch {
                runCatching {
                    videoRepository.dislikeFeed(video.aid, REASON_ID_NOT_INTERESTED)
                }.onSuccess { response ->
                    if (response.isSuccess) {
                        toast(context.getString(R.string.toast_dislike_video_success))
                        onDislikeVideo?.invoke()
                        dismiss()
                    } else {
                        toast(context.getString(R.string.toast_dislike_video_failed))
                    }
                }.onFailure {
                    toast(context.getString(R.string.toast_dislike_video_failed))
                }
            }
        }

        binding.buttonDislikeUp.setOnClickListener {
            if (!checkLogin()) return@setOnClickListener
            val upName = video.authorName
            scope.launch {
                runCatching {
                    videoRepository.dislikeFeed(video.aid, REASON_ID_DISLIKE_UP)
                }.onSuccess { response ->
                    if (response.isSuccess) {
                        toast(context.getString(R.string.toast_dislike_up_success))
                        onDislikeUp?.invoke(upName)
                        dismiss()
                    } else {
                        toast(context.getString(R.string.toast_dislike_up_failed))
                    }
                }.onFailure {
                    toast(context.getString(R.string.toast_dislike_up_failed))
                }
            }
        }
    }

    private fun addWatchLater() {
        scope.launch {
            runCatching {
                videoRepository.addWatchLater(video.aid, video.bvid)
            }.onSuccess { response ->
                if (response.isSuccess) {
                    isInWatchLater = true
                    renderWatchLaterState()
                    toast(context.getString(R.string.toast_add_watch_later_success))
                } else {
                    val msg = response.errorMessage
                    if (msg.contains("90001") || msg.contains("上限") || msg.contains("已满")) {
                        toast(context.getString(R.string.toast_watch_later_full))
                    } else {
                        toast(context.getString(R.string.toast_add_watch_later_failed))
                    }
                }
            }.onFailure {
                toast(context.getString(R.string.toast_add_watch_later_failed))
            }
        }
    }

    private fun removeWatchLater() {
        scope.launch {
            runCatching {
                videoRepository.removeWatchLater(video.aid)
            }.onSuccess { response ->
                if (response.isSuccess) {
                    isInWatchLater = false
                    renderWatchLaterState()
                    toast(context.getString(R.string.toast_remove_watch_later_success))
                } else {
                    toast(context.getString(R.string.toast_remove_watch_later_failed))
                }
            }.onFailure {
                toast(context.getString(R.string.toast_remove_watch_later_failed))
            }
        }
    }

    private fun refreshWatchLaterState() {
        if (!sessionGateway.isLoggedIn()) return
        scope.launch {
            runCatching {
                videoRepository.checkWatchLater(video.aid)
            }.onSuccess { inList ->
                isInWatchLater = inList
                renderWatchLaterState()
            }
        }
    }

    private fun renderWatchLaterState() {
        if (isInWatchLater) {
            binding.textWatchLater.text = context.getString(R.string.menu_already_in_watch_later)
            binding.textWatchLater.setTextColor(
                ContextCompat.getColor(context, R.color.pink)
            )
        } else {
            binding.textWatchLater.text = context.getString(R.string.menu_add_watch_later)
            binding.textWatchLater.setTextColor(
                ContextCompat.getColor(context, R.color.textColor)
            )
        }
    }

    private fun checkLogin(): Boolean {
        if (!sessionGateway.isLoggedIn()) {
            toast(context.getString(R.string.toast_need_login))
            return false
        }
        return true
    }

    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun dismiss() {
        scope.cancel()
        super.dismiss()
    }

    companion object {
        private const val REASON_ID_NOT_INTERESTED = 1
        private const val REASON_ID_DISLIKE_UP = 4
    }
}
