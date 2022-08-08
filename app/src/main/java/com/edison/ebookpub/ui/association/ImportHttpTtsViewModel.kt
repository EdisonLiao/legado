package com.edison.ebookpub.ui.association

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.edison.ebookpub.R
import com.edison.ebookpub.base.BaseViewModel
import com.edison.ebookpub.data.appDb
import com.edison.ebookpub.data.entities.HttpTTS
import com.edison.ebookpub.exception.NoStackTraceException
import com.edison.ebookpub.help.http.newCallResponseBody
import com.edison.ebookpub.help.http.okHttpClient
import com.edison.ebookpub.help.http.text
import com.edison.ebookpub.utils.isAbsUrl
import com.edison.ebookpub.utils.isJsonArray
import com.edison.ebookpub.utils.isJsonObject
import com.edison.ebookpub.utils.printOnDebug

class ImportHttpTtsViewModel(app: Application) : BaseViewModel(app) {

    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allSources = arrayListOf<HttpTTS>()
    val checkSources = arrayListOf<HttpTTS?>()
    val selectStatus = arrayListOf<Boolean>()

    val isSelectAll: Boolean
        get() {
            selectStatus.forEach {
                if (!it) {
                    return false
                }
            }
            return true
        }

    val selectCount: Int
        get() {
            var count = 0
            selectStatus.forEach {
                if (it) {
                    count++
                }
            }
            return count
        }

    fun importSelect(finally: () -> Unit) {
        execute {
            val selectSource = arrayListOf<HttpTTS>()
            selectStatus.forEachIndexed { index, b ->
                if (b) {
                    selectSource.add(allSources[index])
                }
            }
            appDb.httpTTSDao.insert(*selectSource.toTypedArray())
        }.onFinally {
            finally.invoke()
        }
    }

    fun importSource(text: String) {
        execute {
            importSourceAwait(text.trim())
        }.onError {
            it.printOnDebug()
            errorLiveData.postValue(it.localizedMessage ?: "")
        }.onSuccess {
            comparisonSource()
        }
    }

    private suspend fun importSourceAwait(text: String) {
        when {
            text.isJsonObject() -> {
                HttpTTS.fromJson(text).getOrThrow().let {
                    allSources.add(it)
                }
            }
            text.isJsonArray() -> HttpTTS.fromJsonArray(text).getOrThrow().let { items ->
                allSources.addAll(items)
            }
            text.isAbsUrl() -> {
                importSourceUrl(text)
            }
            else -> throw NoStackTraceException(context.getString(R.string.wrong_format))
        }
    }

    private suspend fun importSourceUrl(url: String) {
        okHttpClient.newCallResponseBody {
            url(url)
        }.text().let {
            importSourceAwait(it)
        }
    }

    private fun comparisonSource() {
        execute {
            allSources.forEach {
                val source = appDb.httpTTSDao.get(it.id)
                checkSources.add(source)
                selectStatus.add(source == null || source.lastUpdateTime < it.lastUpdateTime)
            }
            successLiveData.postValue(allSources.size)
        }
    }

}