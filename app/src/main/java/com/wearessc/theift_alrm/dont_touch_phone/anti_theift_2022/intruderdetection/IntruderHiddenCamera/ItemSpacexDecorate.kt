package com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.intruderdetection.IntruderHiddenCamera

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

class ItemSpacexDecorate(private val space: Int) : ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect, view: View,
        parent: RecyclerView, state: RecyclerView.State
    ) {
        outRect.left = space
        outRect.right = space
        outRect.bottom = space
    }
}