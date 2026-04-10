package com.tutu.myblbl.ui.view.player

import android.animation.ValueAnimator
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.tutu.myblbl.R
import java.util.Locale

class SecondsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val triangleContainer: LinearLayout
    private val tvSeconds: TextView
    private val icon1: ImageView
    private val icon2: ImageView
    private val icon3: ImageView

    var animator1: ValueAnimator? = null
    private var animator2: ValueAnimator? = null
    private var animator3: ValueAnimator? = null
    private var animator4: ValueAnimator? = null
    private var animator5: ValueAnimator? = null

    private var cycleDuration: Long = 750L
    private var currentProgressStr: String = ""
    private var seconds: Int = 0
    var isForward: Boolean = true
        private set
    private var iconRes: Int = R.drawable.ic_play_triangle

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.yt_seconds_view, this, true)
        triangleContainer = view.findViewById(R.id.triangle_container)
        tvSeconds = view.findViewById(R.id.tv_seconds)
        icon1 = view.findViewById(R.id.icon_1)
        icon2 = view.findViewById(R.id.icon_2)
        icon3 = view.findViewById(R.id.icon_3)

        initAnimators()
        setForward(true)
    }

    private fun initAnimators() {
        val duration = cycleDuration / 5

        animator1 = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            addUpdateListener { anim ->
                val value = anim.animatedValue as Float
                icon1.alpha = value
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: android.animation.Animator) {
                    icon1.alpha = 0f
                    icon2.alpha = 0f
                    icon3.alpha = 0f
                }
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    animator2?.start()
                }
            })
        }

        animator2 = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            addUpdateListener { anim ->
                val value = anim.animatedValue as Float
                icon2.alpha = value
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: android.animation.Animator) {
                    icon1.alpha = 1f
                    icon2.alpha = 0f
                    icon3.alpha = 0f
                }
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    animator3?.start()
                }
            })
        }

        animator3 = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            addUpdateListener { anim ->
                val value = anim.animatedValue as Float
                icon1.alpha = 1f - value
                icon3.alpha = value
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: android.animation.Animator) {
                    icon1.alpha = 1f
                    icon2.alpha = 1f
                    icon3.alpha = 0f
                }
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    animator4?.start()
                }
            })
        }

        animator4 = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            addUpdateListener { anim ->
                val value = anim.animatedValue as Float
                icon2.alpha = 1f - value
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: android.animation.Animator) {
                    icon1.alpha = 0f
                    icon2.alpha = 1f
                    icon3.alpha = 1f
                }
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    animator5?.start()
                }
            })
        }

        animator5 = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            addUpdateListener { anim ->
                val value = anim.animatedValue as Float
                icon3.alpha = 1f - value
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: android.animation.Animator) {
                    icon1.alpha = 0f
                    icon2.alpha = 0f
                    icon3.alpha = 1f
                }
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    animator1?.start()
                }
            })
        }
    }

    fun cancel() {
        animator1?.cancel()
        animator2?.cancel()
        animator3?.cancel()
        animator4?.cancel()
        animator5?.cancel()
        icon1.alpha = 0f
        icon2.alpha = 0f
        icon3.alpha = 0f
    }

    fun m() {
        animator1?.cancel()
        animator2?.cancel()
        animator3?.cancel()
        animator4?.cancel()
        animator5?.cancel()
        icon1.alpha = 0f
        icon2.alpha = 0f
        icon3.alpha = 0f
    }

    fun start() {
        cancel()
        animator1?.start()
    }

    fun setCurrentProgressStr(str: String) {
        currentProgressStr = str
    }

    fun getCurrentProgressStr(): String {
        return currentProgressStr
    }

    fun getCycleDuration(): Long {
        return cycleDuration
    }

    fun getIcon(): Int {
        return iconRes
    }

    fun getSeconds(): Int {
        return seconds
    }

    fun getTextView(): TextView {
        return tvSeconds
    }

    fun setCycleDuration(duration: Long) {
        val newDuration = duration / 5
        animator1?.duration = newDuration
        animator2?.duration = newDuration
        animator3?.duration = newDuration
        animator4?.duration = newDuration
        animator5?.duration = newDuration
        cycleDuration = duration
    }

    fun setForward(forward: Boolean) {
        triangleContainer.rotation = if (forward) 0f else 180f
        isForward = forward
    }

    fun setIcon(resId: Int) {
        if (resId > 0) {
            icon1.setImageResource(resId)
            icon2.setImageResource(resId)
            icon3.setImageResource(resId)
        }
        iconRes = resId
    }

    fun setSeconds(sec: Int) {
        val isEmpty = TextUtils.isEmpty(currentProgressStr)
        if (isEmpty) {
            val quantity = if (sec == 1) R.plurals.quick_seek_x_second else R.plurals.quick_seek_x_second
            tvSeconds.text = context.resources.getQuantityString(quantity, sec, sec)
        } else {
            val quantity = if (sec == 1) R.plurals.quick_seek_x_second else R.plurals.quick_seek_x_second
            val secondsText = context.resources.getQuantityString(quantity, sec, sec)
            tvSeconds.text = String.format(Locale.getDefault(), "%s(%s)", secondsText, currentProgressStr)
        }
        seconds = sec
    }

    fun setSpeedText(speed: Float) {
        val speedStr = if (speed == speed.toInt().toFloat()) {
            "${speed.toInt()}x"
        } else {
            String.format(Locale.getDefault(), "%.1fx", speed)
        }
        tvSeconds.text = speedStr
        seconds = 0
    }
}
