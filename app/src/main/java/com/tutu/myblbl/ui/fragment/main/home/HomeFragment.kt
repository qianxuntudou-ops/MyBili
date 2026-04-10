package com.tutu.myblbl.ui.fragment.main.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.widget.ViewPager2
import com.tutu.myblbl.databinding.FragmentHomeBinding
import com.tutu.myblbl.ui.activity.MainActivity
import com.tutu.myblbl.ui.fragment.main.MainTabFocusTarget
import com.tutu.myblbl.utils.AppLog
import com.tutu.myblbl.utils.enableTouchNavigation
import com.tutu.myblbl.utils.focusNearestTabTo
import com.tutu.myblbl.utils.getHomeDefaultStartPageIndex
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class HomeFragment : Fragment(), MainTabFocusTarget {

    companion object {
        private const val TAG = "MainEntryFocus"

        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HomeFragmentStateAdapter
    private var tabMediator: TabLayoutMediator? = null
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = HomeFragmentStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        tabMediator = TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = adapter.getPageTitle(position)
        }.also { it.attach() }
        binding.tabLayout.enableTouchNavigation(
            viewPager = binding.viewPager,
            matchLegacyViewPagerAnimation = true,
            onNavigateDown = ::focusCurrentPagePrimaryContent,
            onNavigateLeft = ::focusLeftFunctionArea
        )

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = Unit

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) {
                postTopTabEvent(tab.position)
            }
        })
        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                (adapter.getCurrentFragment(position) as? HomeTabPage)?.onTabSelected()
            }
        }.also { callback ->
            binding.viewPager.registerOnPageChangeCallback(callback)
        }

        binding.viewPager.currentItem = getDefaultTabIndex()
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        pageChangeCallback?.let { binding.viewPager.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = null
        tabMediator?.detach()
        tabMediator = null
        binding.viewPager.adapter = null
        super.onDestroyView()
        _binding = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: String) {
        if (isHidden || view == null) {
            return
        }
        when (event) {
            "selectTab0" -> {
                (adapter.getCurrentFragment(binding.viewPager.currentItem) as? HomeTabPage)?.onTabSelected()
            }
        }
    }

    private fun getDefaultTabIndex(): Int {
        return requireContext().getHomeDefaultStartPageIndex(
            maxIndex = adapter.itemCount - 1,
            defaultIndex = 0
        )
    }

    private fun postTopTabEvent(position: Int) {
        EventBus.getDefault().post("clickTopTab$position")
    }

    fun focusCurrentTab(anchorView: View? = view?.findFocus() ?: activity?.currentFocus): Boolean {
        val currentBinding = _binding ?: return false
        return currentBinding.tabLayout.focusNearestTabTo(anchorView)
    }

    override fun focusEntryFromMainTab(): Boolean {
        return focusEntryFromMainTab(anchorView = null, preferSpatialEntry = false)
    }

    override fun focusEntryFromMainTab(anchorView: View?, preferSpatialEntry: Boolean): Boolean {
        val handled = focusCurrentPagePrimaryContent(anchorView, preferSpatialEntry)
        AppLog.d(
            TAG,
            "HomeFragment.focusEntryFromMainTab: currentItem=${binding.viewPager.currentItem} handled=$handled preferSpatialEntry=$preferSpatialEntry anchor=${anchorView?.javaClass?.simpleName ?: "null"} focus=${view?.findFocus()?.javaClass?.simpleName ?: "null"}"
        )
        return handled
    }

    private fun focusCurrentPagePrimaryContent(anchorView: View? = null, preferSpatialEntry: Boolean = false): Boolean {
        return getCurrentTabPage()?.focusPrimaryContent(anchorView, preferSpatialEntry) == true
    }

    private fun focusLeftFunctionArea(): Boolean {
        return (activity as? MainActivity)?.focusLeftFunctionArea() == true
    }

    private fun getCurrentTabPage(): HomeTabPage? {
        val currentItem = binding.viewPager.currentItem
        val fragmentTag = "f${adapter.getItemId(currentItem)}"
        return childFragmentManager.findFragmentByTag(fragmentTag) as? HomeTabPage
    }
}
