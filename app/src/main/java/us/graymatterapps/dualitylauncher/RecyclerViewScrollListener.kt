package us.graymatterapps.dualitylauncher

import androidx.recyclerview.widget.RecyclerView

class RecyclerViewScrollListener : RecyclerView.OnScrollListener() {
    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)

        when (newState) {
            RecyclerView.SCROLL_STATE_IDLE -> {
                isScrolling = false
            }

            RecyclerView.SCROLL_STATE_DRAGGING -> {
                isScrolling = true
            }

            RecyclerView.SCROLL_STATE_SETTLING -> {
                isScrolling = true
            }
        }
    }
}