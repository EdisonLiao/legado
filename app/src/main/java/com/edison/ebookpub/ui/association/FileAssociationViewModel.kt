package com.edison.ebookpub.ui.association

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import com.edison.ebookpub.constant.AppLog
import com.edison.ebookpub.constant.AppPattern.bookFileRegex
import com.edison.ebookpub.exception.NoStackTraceException
import com.edison.ebookpub.model.localBook.LocalBook
import com.edison.ebookpub.utils.isJson
import com.edison.ebookpub.utils.printOnDebug
import java.io.File
import java.io.InputStream

class FileAssociationViewModel(application: Application) : BaseAssociationViewModel(application) {
    val importBookLiveData = MutableLiveData<Uri>()
    val onLineImportLive = MutableLiveData<Uri>()
    val openBookLiveData = MutableLiveData<String>()
    val notSupportedLiveData = MutableLiveData<Pair<Uri, String>>()

    @Suppress("BlockingMethodInNonBlockingContext")
    fun dispatchIndent(uri: Uri) {
        execute {
            lateinit var fileName: String
            lateinit var content: String
            lateinit var fileStream: InputStream
            //如果是普通的url，需要根据返回的内容判断是什么
            if (uri.scheme == "file" || uri.scheme == "content") {
                if (uri.scheme == "file") {
                    val file = File(uri.path.toString())
                    fileStream = file.inputStream()
                    fileName = file.name
                } else {
                    val file = DocumentFile.fromSingleUri(context, uri)
                    if (file?.exists() != true) throw NoStackTraceException("文件不存在")
                    fileStream = context.contentResolver.openInputStream(uri)!!
                    fileName = file.name ?: ""
                }
                kotlin.runCatching {
                    content = fileStream.reader(Charsets.UTF_8).use { it.readText() }
                    if (content.isJson()) {
                        importJson(content)
                        return@execute
                    }
                }.onFailure {
                    it.printOnDebug()
                    AppLog.put("尝试导入为JSON文件失败\n${it.localizedMessage}", it)
                }
                if (fileName.matches(bookFileRegex)) {
                    importBookLiveData.postValue(uri)
                    return@execute
                }
                notSupportedLiveData.postValue(Pair(uri, fileName))
            } else {
                onLineImportLive.postValue(uri)
            }
        }.onError {
            it.printOnDebug()
            errorLive.postValue(it.localizedMessage)
        }
    }

    fun importBook(uri: Uri) {
        val book = LocalBook.importFile(uri)
        openBookLiveData.postValue(book.bookUrl)
    }
}