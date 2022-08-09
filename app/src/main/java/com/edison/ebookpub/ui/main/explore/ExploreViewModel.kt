package com.edison.ebookpub.ui.main.explore

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.edison.ebookpub.R
import com.edison.ebookpub.base.BaseViewModel
import com.edison.ebookpub.constant.AppPattern
import com.edison.ebookpub.data.appDb
import com.edison.ebookpub.data.entities.BookSource
import com.edison.ebookpub.exception.NoStackTraceException
import com.edison.ebookpub.help.ContentProcessor
import com.edison.ebookpub.help.SourceHelp
import com.edison.ebookpub.help.config.AppConfig
import com.edison.ebookpub.help.http.newCallResponseBody
import com.edison.ebookpub.help.http.okHttpClient
import com.edison.ebookpub.utils.*
import com.jayway.jsonpath.JsonPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ExploreViewModel(application: Application) : BaseViewModel(application) {

    val allSources = arrayListOf<BookSource>()
    val successLiveData = MutableLiveData<Int>()
    val errorLiveData = MutableLiveData<String>()
    val initLocalSourceDoneLiveData = MutableLiveData<Int>()

    fun topSource(bookSource: BookSource) {
        execute {
            val minXh = appDb.bookSourceDao.minOrder
            bookSource.customOrder = minXh - 1
            appDb.bookSourceDao.insert(bookSource)
        }
    }

    fun initLocalBookSource(){
        var bookJson = ""
        execute {
            bookJson = SourceHelp.readAssetsJsonFile(getApplication(),"local_booksource1.json")
        }.onSuccess {
            importSource(bookJson)
        }
    }

    private fun importSource(text: String) {
        execute {
            val mText = text.trim()
            when {
                mText.isJsonObject() -> {
                    kotlin.runCatching {
                        val json = JsonPath.parse(mText)
                        json.read<List<String>>("$.sourceUrls")
                    }.onSuccess {
                        it.forEach {
                            importSourceUrl(it)
                        }
                    }.onFailure {
                        BookSource.fromJson(mText).getOrThrow().let {
                            allSources.add(it)
                        }
                    }
                }
                mText.isJsonArray() -> BookSource.fromJsonArray(mText).getOrThrow().let { items ->
                    allSources.addAll(items)
                }
                mText.isAbsUrl() -> {
                    importSourceUrl(mText)
                }
                else -> throw NoStackTraceException(context.getString(R.string.wrong_format))
            }
        }.onError {
            it.printOnDebug()
            errorLiveData.postValue(it.localizedMessage ?: "")
        }.onSuccess {
            importSelect()
        }
    }

    private suspend fun importSourceUrl(url: String) {
        okHttpClient.newCallResponseBody {
            url(url)
        }.byteStream().let {
            allSources.addAll(BookSource.fromJsonArray(it).getOrThrow())
        }
    }

    private fun importSelect() {
        execute {
            val selectSource = arrayListOf<BookSource>()
            allSources.forEachIndexed { _, b ->
                selectSource.add(b)
            }
            SourceHelp.insertBookSource(*selectSource.toTypedArray())
            ContentProcessor.upReplaceRules()
        }.onFinally {
            initLocalSourceDoneLiveData.value = 200
        }
    }

    fun getAllBookCount(): Int{
        return appDb.bookSourceDao.allCount()
    }

}