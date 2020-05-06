package com.icoo.ssgsag_android.ui.main.feed.category

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.icoo.ssgsag_android.R
import com.icoo.ssgsag_android.base.BaseFragment
import com.icoo.ssgsag_android.databinding.FragmentCategoryFeedBinding
import com.icoo.ssgsag_android.ui.main.feed.adapter.FeedRecyclerViewAdapter
import com.icoo.ssgsag_android.ui.main.feed.FeedViewModel
import com.icoo.ssgsag_android.util.extensionFunction.setSafeOnClickListener
import com.icoo.ssgsag_android.util.view.NonScrollLinearLayoutManager
import com.icoo.ssgsag_android.util.view.WrapContentLinearLayoutManager
import org.koin.androidx.viewmodel.ext.android.viewModel

class FeedCategoryFragment : BaseFragment<FragmentCategoryFeedBinding, FeedViewModel>(){

    override val layoutResID: Int
        get() = R.layout.fragment_category_feed
    override val viewModel: FeedViewModel by viewModel()

    private var feedRecyclerViewAdapter: FeedRecyclerViewAdapter? = null
    private var curPage = 0
    private var categoryIdx = 0

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewDataBinding.vm = viewModel
        viewDataBinding.fragCategoryFeedsLlContainer.setOnTouchListener(object : View.OnTouchListener{
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                return true;
            }
        })

        setRv()
        setRefreshRv()
        setButton()
        navigator()

    }

    private fun setRv(){
        viewModel.tmpFeeds.observe(this, Observer { value ->
            if(feedRecyclerViewAdapter != null){
                feedRecyclerViewAdapter!!.apply {
                    addItem(value)
                    notifyDataSetChanged()
                }
            }else{
                feedRecyclerViewAdapter = FeedRecyclerViewAdapter(value)
                feedRecyclerViewAdapter!!.run{
                    setOnFeedItemClickListener(onFeedItemClickListener)
                    setHasStableIds(true)
                }

                viewDataBinding.fragCategoryFeedsRv.apply {
                    adapter = feedRecyclerViewAdapter
                }
            }
        })

        viewDataBinding.fragCategoryFeedsRv.apply{
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(
                    recyclerView: RecyclerView,
                    newState: Int
                ) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if (newState == SCROLL_STATE_DRAGGING || newState == SCROLL_STATE_IDLE) {
                        var position =
                            (layoutManager as WrapContentLinearLayoutManager).findLastVisibleItemPosition()

                        feedRecyclerViewAdapter!!.apply {

                            if (itemList!!.size > 0 && (10 * (curPage + 1) - 2 < position)) {
                                curPage = (position + 1) / 10
                                viewModel.getCategoryFeeds(curPage, viewModel.category.value!!)
                            }
                        }

                    }
                }

            })

            (this.itemAnimator as SimpleItemAnimator).run {
                changeDuration = 0
                supportsChangeAnimations = false
            }
            layoutManager = WrapContentLinearLayoutManager()
        }


        viewDataBinding.fragCategoryFeedsSrl.apply {
            setOnRefreshListener(object : SwipeRefreshLayout.OnRefreshListener {
                override fun onRefresh() {
                    // 새로고침 코드
                    viewDataBinding.fragCategoryFeedsRv.apply {
                        (this.adapter as FeedRecyclerViewAdapter).apply {
                            clearAll()
                            curPage = 0
                            viewModel.getCategoryFeeds(curPage, viewModel.category.value!!)

                        }
                        viewDataBinding.fragCategoryFeedsSrl.isRefreshing = false

                    }
                }
            })
        }

    }

    private fun setRefreshRv(){
        viewModel.refreshedFeed.observe(this, Observer {value ->
            (viewDataBinding.fragCategoryFeedsRv.adapter as? FeedRecyclerViewAdapter)?.run{
                refreshItem(value, viewModel.refreshedFeedPosition)
                notifyItemChanged(viewModel.refreshedFeedPosition)

            }
        })
    }

    private val onFeedItemClickListener =
        object : FeedRecyclerViewAdapter.OnFeedItemClickListener {
            override fun onItemClicked(
                feedIdx: Int, feedUrl: String, feedName: String, isSave: Int, position: Int
            ) {
                viewModel.navigate(feedUrl, feedIdx, position)
            }

            override fun onShareClicked(feedUrl: String) {
                val intent = Intent(android.content.Intent.ACTION_SEND)
                intent.setType("text/plain")

                intent.putExtra(Intent.EXTRA_TEXT, feedUrl)

                val chooser = Intent.createChooser(intent, "친구에게 링크 공유하기")
                startActivity(chooser)
            }

            override fun onBookmarkClicked(feedIdx: Int, isSaved: Int,  position: Int) {
                viewModel.bookmark(feedIdx, isSaved, position)
            }
        }

    private fun setButton(){
        viewDataBinding.fragCategoryFeedsIvBack.setSafeOnClickListener {
            activity!!.supportFragmentManager.beginTransaction().remove(this).commit()
        }
    }
    private fun navigator() {
        viewModel.activityToStart.observe(this, Observer { value ->
            val intent = Intent(context, value.first.java)
            value.second?.let {
                intent.putExtras(it)
            }
            startActivity(intent)
        })
    }

    fun setCategoryIdx(idx: Int){
        categoryIdx = idx
        viewModel.setCategory(idx)
    }

}