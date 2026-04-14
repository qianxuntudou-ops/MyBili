package com.tutu.myblbl.core.ui.navigation

import androidx.fragment.app.Fragment
import com.tutu.myblbl.ui.activity.MainActivity

fun Fragment.navigateBackFromUi(skipNextFocusRestore: Boolean = false) {
    val hostActivity = activity as? MainActivity
    if (hostActivity != null) {
        if (skipNextFocusRestore) {
            hostActivity.skipNextFocusRestore()
        }
        hostActivity.closeTopOverlayFromUi()
        return
    }

    if (parentFragmentManager.backStackEntryCount > 0) {
        parentFragmentManager.popBackStack()
        return
    }

    activity?.finish()
}
