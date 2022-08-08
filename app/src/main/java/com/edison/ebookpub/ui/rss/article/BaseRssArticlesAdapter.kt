package com.edison.ebookpub.ui.rss.article

import android.content.Context
import androidx.viewbinding.ViewBinding
import com.edison.ebookpub.base.adapter.RecyclerAdapter
import com.edison.ebookpub.data.entities.RssArticle


abstract class BaseRssArticlesAdapter<VB : ViewBinding>(context: Context, val callBack: CallBack) :
    RecyclerAdapter<RssArticle, VB>(context) {

    interface CallBack {
        val isGridLayout: Boolean
        fun readRss(rssArticle: RssArticle)
    }
}