package com.icoo.ssgsag_android.ui.main.feed

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import com.icoo.ssgsag_android.R
import com.icoo.ssgsag_android.base.BaseFragment
import com.icoo.ssgsag_android.data.local.pref.SharedPreferenceController
import com.icoo.ssgsag_android.databinding.FragmentFeedPageBinding
import com.icoo.ssgsag_android.ui.main.myPage.MyPageActivity
import org.jetbrains.anko.startActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.SimpleItemAnimator
import com.icoo.ssgsag_android.ui.main.MainActivity
import com.icoo.ssgsag_android.ui.main.feed.adapter.FeedAnchorRecyclerViewAdapter
import com.icoo.ssgsag_android.ui.main.feed.adapter.FeedBestCardViewPagerAdapter
import com.icoo.ssgsag_android.ui.main.feed.adapter.FeedCareerViewPagerAdapter
import com.icoo.ssgsag_android.ui.main.feed.bookmark.BookmarkedFeedActivity
import com.icoo.ssgsag_android.util.extensionFunction.setSafeOnClickListener
import com.icoo.ssgsag_android.util.view.WrapContentLinearLayoutManager
import org.jetbrains.anko.support.v4.startActivity
import com.icoo.ssgsag_android.ui.main.feed.category.FeedCategoryActivity
import com.icoo.ssgsag_android.ui.main.feed.category.FeedCategoryRecyclerViewAdapter
import org.jetbrains.anko.verticalMargin


class FeedFragment : BaseFragment<FragmentFeedPageBinding, FeedViewModel>(){

    override val layoutResID: Int
        get() = R.layout.fragment_feed_page
    override val viewModel: FeedViewModel by viewModel()

    private var feedAnchorRecyclerViewAdapter : FeedAnchorRecyclerViewAdapter? = null
    private var feedCategoryRecyclerViewAdapter: FeedCategoryRecyclerViewAdapter? = null
    private var careerViewPagerAdapter : FeedCareerViewPagerAdapter? = null

    private var viewTop = 0



    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewDataBinding.vm = viewModel

        setAnchorRv()
        setBestViewPager()
        setContentsRv()
        setCareerViewPager()
        setButton()
        navigator()

        if(!SharedPreferenceController.getFeedCoachMark(activity!!))
            setCoachMark()
    }

    override fun onResume() {
        super.onResume()

        viewModel.getTodayFeeds()

    }
    private fun setAnchorRv(){

        viewModel.feedCategoryList.observe(this, Observer { value ->
            if(feedAnchorRecyclerViewAdapter != null){
                feedAnchorRecyclerViewAdapter!!.apply{
                    //replaceAll(value)
                    notifyItemRangeChanged(0, 4)
                }
            }else{
                feedAnchorRecyclerViewAdapter = FeedAnchorRecyclerViewAdapter(viewModel.feedCategoryList.value!!)
                feedAnchorRecyclerViewAdapter!!.run {
                    setOnFeedAnchorItemClickListener(onFeedAnchorItemClickListener)
                    setHasStableIds(true)
                }

                viewDataBinding.fragFeedPageRvAnchor.adapter = feedAnchorRecyclerViewAdapter
            }

        })

    }

    private fun setBestViewPager() {

        viewModel.bestFeeds.observe(this, Observer {value ->
            var cardViewPagerAdapter =
                FeedBestCardViewPagerAdapter(
                    activity!!,
                    value
                )
            cardViewPagerAdapter.setOnItemClickListener(onFeedBestItemClickListener)

            viewDataBinding.fragFeedPageVpBest.run{
                clipToPadding = false
                setPadding(66,0 , 261,10 )
                pageMargin = 45

                adapter = cardViewPagerAdapter

            }
        })
    }

    private fun setContentsRv(){

        viewModel.categoryFeeds.observe(this, Observer { value ->
            if(feedCategoryRecyclerViewAdapter != null){
                feedCategoryRecyclerViewAdapter!!.apply {
                    replaceAll(value)
                    notifyItemRangeChanged(0, this.itemCount)

                }
            }else{
                feedCategoryRecyclerViewAdapter =
                    FeedCategoryRecyclerViewAdapter(value, viewModel)
                feedCategoryRecyclerViewAdapter!!.run{
                    setOnFeedCategoryClickListener(onFeedCategoryClickListener)
                    //setHasStableIds(true)
                }

                viewDataBinding.fragFeedPageRvContents.apply {
                    adapter = feedCategoryRecyclerViewAdapter


                    (itemAnimator as SimpleItemAnimator).run {
                        changeDuration = 0
                        supportsChangeAnimations = false
                    }

                    layoutManager = WrapContentLinearLayoutManager()
                }
            }

        })

    }

    private fun setCareerViewPager() {

        val feedNavigation: View =
            LayoutInflater.from(activity!!).inflate(R.layout.feed_career_navigation_bar, null)

        viewModel.careerFeeds.observe(this, Observer {value ->

            if(careerViewPagerAdapter != null){
                careerViewPagerAdapter!!.run{
                    replaceAll(value)
                }
            }else{
                careerViewPagerAdapter = FeedCareerViewPagerAdapter(activity!!, value, viewModel)
                careerViewPagerAdapter!!.setOnFeedCategoryClickListener(onFeedCategoryClickListener)

                viewDataBinding.fragFeedPageVpCareer.run{
                    adapter = careerViewPagerAdapter
                }

                viewDataBinding.fragFeedPageTlCareer.run {
                    setupWithViewPager(viewDataBinding.fragFeedPageVpCareer)
                    getTabAt(0)!!.customView =
                        feedNavigation.findViewById(R.id.feed_career_navigation_bar_rl_it) as RelativeLayout
                    getTabAt(1)!!.customView =
                        feedNavigation.findViewById(R.id.feed_career_navigation_bar_rl_marketing) as RelativeLayout
                    getTabAt(2)!!.customView =
                        feedNavigation.findViewById(R.id.feed_career_navigation_bar_rl_design) as RelativeLayout

                    setTabRippleColor(null)
                }
            }

        })

    }

    // anchor 클릭
    private val onFeedAnchorItemClickListener =
        object : FeedAnchorRecyclerViewAdapter.OnFeedAnchorItemClickListener{
            override fun onItemClicked(position: Int, categoryIdx: Int) {
                viewTop = 0

                when(position){
                    0 -> scrollToView(viewDataBinding.fragFeedPageNsv, viewDataBinding.fragFeedPageNsv)
                    1 -> scrollToView(
                        viewDataBinding.fragFeedPageRvContents.layoutManager!!.findViewByPosition(0)!!
                        , viewDataBinding.fragFeedPageNsv)
                    2 -> scrollToView(
                        viewDataBinding.fragFeedPageRvContents.layoutManager!!.findViewByPosition(1)!!
                        , viewDataBinding.fragFeedPageNsv)
                    3 -> scrollToView(viewDataBinding.fragFeedPageVpCareer, viewDataBinding.fragFeedPageNsv)
                }
                viewModel.checkCate(position!!)
            }

    }

    // best feed 클릭
    private val onFeedBestItemClickListener =
        object : FeedBestCardViewPagerAdapter.OnItemClickListener{
            override fun onItemClick(feedUrl: String, feedIdx: Int, position: Int) {
                viewModel.navigate(feedUrl, feedIdx, position)
            }
        }

    // categoryList 더보기 버튼 클릭
    private val onFeedCategoryClickListener =
        object : FeedCategoryRecyclerViewAdapter.OnFeedCategoryClickListener{
            override fun onMoreBtnClicked(categoryIdx: Int) {
                val intent = Intent(activity!!, FeedCategoryActivity::class.java)
                intent.putExtra("categoryIdx", categoryIdx)

                startActivity(intent)
            }
        }

    fun scrollToView(view: View, scrollView: NestedScrollView){
        if(view != null && view != scrollView){
            viewTop += view.top
            scrollToView((view.parent as View), scrollView)
        } else if(scrollView != null){
            Handler().postDelayed({ scrollView.smoothScrollTo(0, viewTop) }, 100)
        }
    }


    private fun setButton() {
        viewDataBinding.fragFeedPageMyPage.setSafeOnClickListener {
            view!!.context.startActivity<MyPageActivity>()
            (view!!.context as Activity).overridePendingTransition(
                R.anim.anim_slide_in_left,
                R.anim.anim_not_move
            )

        }

        viewDataBinding.fragFeedPageIvMenu.setSafeOnClickListener {
            startActivity<BookmarkedFeedActivity>()
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

    private fun setCoachMark(){

        viewDataBinding.fragFeedPageClCoachmarkContainer.visibility = VISIBLE

        val d = resources.displayMetrics.density
        val widthPx = MainActivity.GetWidth.windowWidth / 8

        val leftDpValue = widthPx / d - 30
        val bottomDpValue = 13

        val leftMargin = (leftDpValue * d).toInt()
        val bottomMargin = (bottomDpValue * d).toInt()

        (viewDataBinding.fragFeedPageClCoachmark.layoutParams as ConstraintLayout.LayoutParams).apply{
            marginStart = leftMargin
            verticalMargin = bottomMargin
        }

        viewDataBinding.run {
            fragFeedPageClCoachmarkContainer.setOnTouchListener { v, event -> true }
        }

        viewDataBinding.fragFeedPageClCoachmarkContainer.visibility = VISIBLE

        viewDataBinding.fragFeedPageClCoachmark.setOnClickListener {
            viewDataBinding.fragFeedPageClCoachmarkContainer.visibility = GONE
            SharedPreferenceController.setFeedCoachMark(activity!!, true)

        }





    }

    companion object {
        private val TAG = "FeedFragment"
    }
}
