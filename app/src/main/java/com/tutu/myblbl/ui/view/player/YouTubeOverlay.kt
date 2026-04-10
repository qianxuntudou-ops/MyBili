package com.tutu.myblbl.ui.view.player

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.TextViewCompat
import androidx.media3.common.Player
import com.tutu.myblbl.R
import com.tutu.myblbl.utils.NumberUtils
import kotlin.math.abs

class YouTubeOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val rootConstraintLayout: ConstraintLayout
    private val secondsView: SecondsView
    private val circleClipTapView: CircleClipTapView
    private val progressBar: ProgressBar

    private var player: Player? = null
    private var playerView: MyPlayerView? = null
    private var callback: Callback? = null
    private var overlayShowing = false

    private val hideOverlayRunnable = Runnable {
        hideOverlayImmediately()
    }

    var seekSeconds: Int = 10
        set(value) {
            field = value
            secondsView.let {
                // Do nothing for now, handled separately
            }
        }

    interface Callback {
        fun onAnimationStart()
        fun onAnimationEnd()
        fun shouldForward(player: Player, playerView: MyPlayerView, x: Float): Boolean?
    }

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.yt_overlay, this, true)
        rootConstraintLayout = view.findViewById(R.id.root_constraint_layout)
        secondsView = view.findViewById(R.id.seconds_view)
        circleClipTapView = view.findViewById(R.id.circle_clip_tap_view)
        progressBar = view.findViewById(R.id.progress_bar)

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.YouTubeOverlay, 0, 0)
            circleClipTapView.setAnimationDuration(a.getInt(R.styleable.YouTubeOverlay_yt_animationDuration, 650).toLong())
            seekSeconds = a.getInt(R.styleable.YouTubeOverlay_yt_seekSeconds, 10)
            secondsView.setCycleDuration(a.getInt(R.styleable.YouTubeOverlay_yt_iconAnimationDuration, 750).toLong())
            circleClipTapView.setArcSize(a.getDimensionPixelSize(R.styleable.YouTubeOverlay_yt_arcSize, context.resources.getDimensionPixelSize(R.dimen.px100)).toFloat())
            circleClipTapView.setCircleColor(a.getColor(R.styleable.YouTubeOverlay_yt_tapCircleColor, ContextCompat.getColor(context, R.color.dtpv_yt_tap_circle_color)))
            circleClipTapView.setCircleBackgroundColor(a.getColor(R.styleable.YouTubeOverlay_yt_backgroundCircleColor, ContextCompat.getColor(context, R.color.dtpv_yt_background_circle_color)))
            val textAppearance = a.getResourceId(R.styleable.YouTubeOverlay_yt_textAppearance, R.style.YTOSecondsTextAppearance)
            TextViewCompat.setTextAppearance(secondsView.getTextView(), textAppearance)
            val iconRes = a.getResourceId(R.styleable.YouTubeOverlay_yt_icon, R.drawable.ic_play_triangle)
            secondsView.m()
            secondsView.setIcon(iconRes)
            a.recycle()
        } else {
            circleClipTapView.setArcSize(context.resources.getDimensionPixelSize(R.dimen.px100).toFloat())
            circleClipTapView.setCircleColor(ContextCompat.getColor(context, R.color.dtpv_yt_tap_circle_color))
            circleClipTapView.setCircleBackgroundColor(ContextCompat.getColor(context, R.color.dtpv_yt_background_circle_color))
            circleClipTapView.setAnimationDuration(650L)
            secondsView.setCycleDuration(750L)
            seekSeconds = 10
            TextViewCompat.setTextAppearance(secondsView.getTextView(), R.style.YTOSecondsTextAppearance)
        }

        secondsView.setForward(true)
        m(true)

        circleClipTapView.setPerformAtEnd(null)
    }

    fun setPlayer(player: Player?) {
        this.player = player
    }

    fun setPlayerView(playerView: MyPlayerView?) {
        this.playerView = playerView
    }

    fun setCallback(callback: Callback?) {
        this.callback = callback
    }

    fun setIconDrawables(@Suppress("UNUSED_PARAMETER") forward: Drawable?, @Suppress("UNUSED_PARAMETER") rewind: Drawable?) {
    }

    private fun m(forward: Boolean) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(rootConstraintLayout)
        if (forward) {
            constraintSet.clear(secondsView.id, ConstraintSet.START)
            constraintSet.connect(secondsView.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        } else {
            constraintSet.clear(secondsView.id, ConstraintSet.END)
            constraintSet.connect(secondsView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        }
        secondsView.visibility = View.VISIBLE
        secondsView.m()
        val valueAnimator = secondsView.animator1
        valueAnimator?.start()
        constraintSet.applyTo(rootConstraintLayout)
        rootConstraintLayout.requestLayout()
    }

    fun show(forward: Boolean, x: Float, y: Float) {
        removeCallbacks(hideOverlayRunnable)
        ensureOverlayVisible()
        secondsView.cancel()
        secondsView.setForward(forward)
        secondsView.x = x - secondsView.width / 2
        secondsView.y = y - secondsView.height / 2
        secondsView.visibility = View.VISIBLE
        secondsView.start()

        seekTo(forward)
        scheduleHide()
    }

    private fun seekTo(forward: Boolean) {
        val currentPlayer = player ?: return
        val currentPos = currentPlayer.currentPosition
        val seekAmount = if (forward) seekSeconds * 1000L else -seekSeconds * 1000L
        val newPos = (currentPos + seekAmount).coerceIn(0L, currentPlayer.duration)
        currentPlayer.seekTo(newPos)
        playerView?.syncDanmakuPosition(newPos, forceSeek = true)
        updateProgress(newPos, currentPlayer.duration)
    }

    fun handleDoubleTap(x: Float, y: Float) {
        val currentPlayer = player ?: return
        val currentPlayerView = playerView ?: return

        val shouldForward = callback?.shouldForward(currentPlayer, currentPlayerView, x)
        if (shouldForward == null) {
            return
        }

        val currentVisibility = visibility
        if (currentVisibility != View.VISIBLE) {
            callback?.onAnimationStart()
            secondsView.visibility = View.VISIBLE
            m(shouldForward)
        }

        val isRewind = !shouldForward
        if (isRewind) {
            if (secondsView.isForward) {
                m(false)
                secondsView.setForward(false)
                secondsView.setSeconds(0)
            }
            circleClipTapView.animate {
                circleClipTapView.setTapPosition(x, y)
            }
            currentPlayer.currentPosition.let { pos ->
                secondsView.setCurrentProgressStr(NumberUtils.formatDuration((pos / 1000) + seekSeconds))
            }
            secondsView.setSeconds(secondsView.getSeconds() + seekSeconds)
            o(currentPlayer.currentPosition - (seekSeconds * 1000L))
        } else {
            if (!secondsView.isForward) {
                m(true)
                secondsView.setForward(true)
                secondsView.setSeconds(0)
            }
            circleClipTapView.animate {
                circleClipTapView.setTapPosition(x, y)
            }
            currentPlayer.currentPosition.let { pos ->
                secondsView.setCurrentProgressStr(NumberUtils.formatDuration((pos / 1000) + seekSeconds))
            }
            secondsView.setSeconds(secondsView.getSeconds() + seekSeconds)
            o(currentPlayer.currentPosition + (seekSeconds * 1000L))
        }

        currentPlayer.duration.let { duration ->
            progressBar.max = duration.toInt()
            progressBar.progress = currentPlayer.currentPosition.toInt()
        }
    }

    private fun o(positionMs: Long?) {
        if (positionMs == null) {
            return
        }
        val currentPlayer = player ?: return
        if (positionMs <= 0L) {
            currentPlayer.seekTo(0L)
            return
        }
        if (positionMs >= currentPlayer.duration) {
            currentPlayer.seekTo(currentPlayer.duration)
            return
        }
        playerView?.keepInDoubleTapMode()
        currentPlayer.seekTo(positionMs)
        playerView?.syncDanmakuPosition(positionMs, forceSeek = true)
    }

    fun showSwipeSeek(targetPositionMs: Long, durationMs: Long, deltaMs: Long) {
        removeCallbacks(hideOverlayRunnable)
        ensureOverlayVisible()

        val forward = deltaMs >= 0
        val directionChanged = secondsView.isForward != forward
        resetSecondsViewPosition()
        if (directionChanged) {
            m(forward)
            secondsView.setForward(forward)
        }

        secondsView.cancel()
        secondsView.visibility = View.VISIBLE
        secondsView.setCurrentProgressStr(NumberUtils.formatDuration(targetPositionMs / 1000L))
        secondsView.setSeconds(abs(deltaMs / 1000L).toInt())
        updateProgress(targetPositionMs, durationMs)
    }

    fun showControllerSeek(targetPositionMs: Long, durationMs: Long, deltaMs: Long) {
        removeCallbacks(hideOverlayRunnable)
        ensureOverlayVisible()

        val forward = deltaMs >= 0L
        val directionChanged = secondsView.isForward != forward
        val shouldRestartIndicators = !overlayShowing || secondsView.visibility != View.VISIBLE || directionChanged

        resetSecondsViewPosition()
        if (directionChanged) {
            m(forward)
            secondsView.setForward(forward)
            secondsView.setSeconds(0)
        } else if (shouldRestartIndicators) {
            secondsView.m()
            secondsView.animator1?.start()
        }

        val overlayX = if (forward) width * 0.75f else width * 0.25f
        val overlayY = height / 2f
        circleClipTapView.animate {
            circleClipTapView.setTapPosition(overlayX, overlayY)
        }

        secondsView.visibility = View.VISIBLE
        secondsView.setCurrentProgressStr(NumberUtils.formatDuration(targetPositionMs / 1000L))
        val deltaSeconds = abs(deltaMs / 1000L).toInt().coerceAtLeast(1)
        val displaySeconds = if (directionChanged || shouldRestartIndicators) {
            deltaSeconds
        } else {
            secondsView.getSeconds() + deltaSeconds
        }
        secondsView.setSeconds(displaySeconds)
        updateProgress(targetPositionMs, durationMs)
        scheduleHide(minIndicatorDisplayDurationMs())
    }

    fun finishSwipeSeek() {
        scheduleHide(180L)
    }

    fun finishControllerSeek() {
        scheduleHide(minIndicatorDisplayDurationMs())
    }

    fun showSpeedSeek(forward: Boolean, speed: Float) {
        removeCallbacks(hideOverlayRunnable)
        ensureOverlayVisible()

        val directionChanged = secondsView.isForward != forward
        val shouldRestart = !overlayShowing || directionChanged
        resetSecondsViewPosition()
        if (directionChanged) {
            m(forward)
            secondsView.setForward(forward)
        } else if (shouldRestart) {
            secondsView.m()
            secondsView.animator1?.start()
        }
        secondsView.visibility = View.VISIBLE
        secondsView.setSpeedText(speed)
        val overlayX = if (forward) width * 0.75f else width * 0.25f
        circleClipTapView.animate {
            circleClipTapView.setTapPosition(overlayX, height / 2f)
        }
        val currentPlayer = player ?: return
        updateProgress(currentPlayer.currentPosition, currentPlayer.duration)
    }

    fun finishSpeedSeek() {
        hideOverlayImmediately()
    }

    fun cancelSwipeSeek() {
        hideOverlayImmediately()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(hideOverlayRunnable)
        secondsView.cancel()
        super.onDetachedFromWindow()
    }

    private fun ensureOverlayVisible() {
        if (overlayShowing) {
            return
        }
        overlayShowing = true
        visibility = View.VISIBLE
        callback?.onAnimationStart()
    }

    private fun hideOverlayImmediately() {
        if (!overlayShowing) {
            return
        }
        overlayShowing = false
        secondsView.cancel()
        secondsView.setSeconds(0)
        secondsView.visibility = View.INVISIBLE
        visibility = View.GONE
        callback?.onAnimationEnd()
    }

    private fun scheduleHide(delayMs: Long = 650L) {
        removeCallbacks(hideOverlayRunnable)
        postDelayed(hideOverlayRunnable, delayMs)
    }

    private fun minIndicatorDisplayDurationMs(): Long {
        return secondsView.getCycleDuration().coerceAtLeast(650L) + 100L
    }

    private fun updateProgress(positionMs: Long, durationMs: Long) {
        val safeDuration = durationMs.coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val safePosition = positionMs.coerceAtLeast(0L).coerceAtMost(safeDuration.toLong()).toInt()
        progressBar.max = safeDuration
        progressBar.progress = safePosition
    }

    private fun centerSecondsView() {
        val updatePosition = {
            secondsView.x = ((width - secondsView.width) / 2f).coerceAtLeast(0f)
            secondsView.y = ((height - secondsView.height) / 2f).coerceAtLeast(0f)
        }
        if (width == 0 || height == 0 || secondsView.width == 0 || secondsView.height == 0) {
            post(updatePosition)
        } else {
            updatePosition()
        }
    }

    private fun resetSecondsViewPosition() {
        secondsView.translationX = 0f
        secondsView.translationY = 0f
    }
}
