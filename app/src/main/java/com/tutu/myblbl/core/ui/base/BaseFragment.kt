package com.tutu.myblbl.core.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.tutu.myblbl.R
import com.tutu.myblbl.core.ui.navigation.navigateBackFromUi
import com.tutu.myblbl.ui.activity.MainActivity

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding: VB get() = _binding!!

    protected var rootView: View? = null
    protected var contentContainer: FrameLayout? = null
    protected var viewError: ConstraintLayout? = null
    protected var textError: AppCompatTextView? = null
    protected var imageError: AppCompatImageView? = null
    protected var buttonRetry: AppCompatTextView? = null
    protected var topBar: LinearLayoutCompat? = null
    protected var buttonBack: AppCompatImageView? = null
    protected var textMainTitle: AppCompatTextView? = null
    protected var loadingProgressBar: ProgressBar? = null
    protected var mainActivity: MainActivity? = null

    abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB
    abstract fun initView()
    open fun initData() {}
    open fun initObserver() {}
    open fun initArguments() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (activity is MainActivity) {
            mainActivity = activity as MainActivity
        }
        initArguments()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            val view = inflater.inflate(R.layout.fragment_base, container, false)
            rootView = view
            contentContainer = view.findViewById(R.id.contentContainer)
            textError = view.findViewById(R.id.text_error)
            imageError = view.findViewById(R.id.image_error)
            buttonRetry = view.findViewById(R.id.button_retry)
            topBar = view.findViewById(R.id.top_bar)
            buttonBack = view.findViewById(R.id.button_back)
            textMainTitle = view.findViewById(R.id.text_main_title)
            loadingProgressBar = view.findViewById(R.id.loading_progress_bar)
            viewError = view.findViewById(R.id.view_error)
            topBar?.visibility = if (isTopBarVisible()) View.VISIBLE else View.GONE
            buttonRetry?.setOnClickListener { onRetryClick() }
            buttonBack?.setOnClickListener {
                navigateBackFromUi()
            }
            _binding = getViewBinding(inflater, contentContainer)
            if (binding.root.parent == null) {
                contentContainer?.addView(binding.root)
            }
            initView()
        }
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObserver()
        initData()
    }

    open fun isTopBarVisible(): Boolean = false

    open fun onRetryClick() {}

    protected open fun showContent() {
        if (!isAdded) return
        viewError?.visibility = View.GONE
        contentContainer?.visibility = View.VISIBLE
    }

    protected fun showLoading(show: Boolean) {
        if (!isAdded) return
        loadingProgressBar?.visibility = if (show) View.VISIBLE else View.GONE
    }

    protected fun showNetError() {
        if (!isAdded) return
        showErrorImage(R.drawable.net_error, getString(R.string.net_error))
    }

    protected fun showEmpty() {
        if (!isAdded) return
        showErrorImage(R.drawable.empty, getString(R.string.empty))
    }

    protected fun showError(message: String?) {
        if (!isAdded) return
        showErrorImage(R.drawable.net_error, message ?: "")
    }

    private fun showErrorImage(imageResId: Int, text: String) {
        viewError?.visibility = View.VISIBLE
        imageError?.setImageResource(imageResId)
        textError?.text = text
        buttonRetry?.requestFocus()
        contentContainer?.visibility = View.GONE
    }

    fun setMainTitle(title: String) {
        textMainTitle?.text = title
    }

    protected open fun openInHostContainer(fragment: Fragment, addToBackStack: Boolean = true) {
        mainActivity?.openInHostContainer(fragment, addToBackStack)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainActivity = null
        viewError = null
        textError = null
        imageError = null
        buttonRetry = null
        loadingProgressBar = null
        topBar = null
        buttonBack = null
        textMainTitle = null
        contentContainer?.removeAllViews()
        contentContainer = null
        _binding = null
        rootView = null
    }
}
