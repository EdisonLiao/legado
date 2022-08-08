package com.edison.ebookpub.ui.book.read.config

import android.app.Application
import android.net.Uri
import android.speech.tts.TextToSpeech
import com.edison.ebookpub.base.BaseViewModel
import com.edison.ebookpub.data.appDb
import com.edison.ebookpub.data.entities.HttpTTS
import com.edison.ebookpub.exception.NoStackTraceException
import com.edison.ebookpub.help.DefaultData
import com.edison.ebookpub.help.http.newCallResponseBody
import com.edison.ebookpub.help.http.okHttpClient
import com.edison.ebookpub.help.http.text
import com.edison.ebookpub.utils.isJsonArray
import com.edison.ebookpub.utils.isJsonObject
import com.edison.ebookpub.utils.readText
import com.edison.ebookpub.utils.toastOnUi

class SpeakEngineViewModel(application: Application) : BaseViewModel(application) {

    val sysEngines: List<TextToSpeech.EngineInfo> = TextToSpeech(context, null).engines

    fun importDefault() {
        execute {
            DefaultData.importDefaultHttpTTS()
        }
    }

    fun importOnLine(url: String) {
        execute {
            okHttpClient.newCallResponseBody {
                url(url)
            }.text("utf-8").let { json ->
                import(json)
            }
        }.onSuccess {
            context.toastOnUi("导入成功")
        }.onError {
            context.toastOnUi("导入失败")
        }
    }

    fun importLocal(uri: Uri) {
        execute {
            import(uri.readText(context))
        }.onSuccess {
            context.toastOnUi("导入成功")
        }.onError {
            context.toastOnUi("导入失败\n${it.localizedMessage}")
        }
    }

    fun import(text: String) {
        when {
            text.isJsonArray() -> {
                HttpTTS.fromJsonArray(text).getOrThrow().let {
                    appDb.httpTTSDao.insert(*it.toTypedArray())
                }
            }
            text.isJsonObject() -> {
                HttpTTS.fromJson(text).getOrThrow().let {
                    appDb.httpTTSDao.insert(it)
                }
            }
            else -> {
                throw NoStackTraceException("格式不对")
            }
        }
    }

}