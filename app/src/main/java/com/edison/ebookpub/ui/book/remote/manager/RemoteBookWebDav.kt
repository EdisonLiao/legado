package com.edison.ebookpub.ui.book.remote.manager


import android.net.Uri
import com.edison.ebookpub.constant.AppPattern.bookFileRegex
import com.edison.ebookpub.exception.NoStackTraceException
import com.edison.ebookpub.help.AppWebDav
import com.edison.ebookpub.lib.webdav.WebDav
import com.edison.ebookpub.lib.webdav.WebDavFile
import com.edison.ebookpub.model.localBook.LocalBook
import com.edison.ebookpub.ui.book.remote.RemoteBook
import com.edison.ebookpub.ui.book.remote.RemoteBookManager
import com.edison.ebookpub.utils.NetworkUtils
import com.edison.ebookpub.utils.isContentScheme
import com.edison.ebookpub.utils.readBytes
import kotlinx.coroutines.runBlocking
import splitties.init.appCtx
import java.io.File

object RemoteBookWebDav : RemoteBookManager() {
    val rootBookUrl get() = "${AppWebDav.rootWebDavUrl}${remoteBookFolder}"

    init {
        runBlocking {
            initRemoteContext()
        }
    }

    override suspend fun initRemoteContext() {
        AppWebDav.authorization?.let {
            WebDav(rootBookUrl, it).makeAsDir()
        }
    }

    /**
     * 获取远程书籍列表
     */
    @Throws(Exception::class)
    override suspend fun getRemoteBookList(path: String): MutableList<RemoteBook> {
        val remoteBooks = mutableListOf<RemoteBook>()
        AppWebDav.authorization?.let {
            //读取文件列表
            val remoteWebDavFileList: List<WebDavFile> = WebDav(path, it).listFiles()
            //转化远程文件信息到本地对象
            remoteWebDavFileList.forEach { webDavFile ->
                if (webDavFile.isDir) {
                    remoteBooks.add(
                        RemoteBook(
                            webDavFile.displayName, webDavFile.path, webDavFile.size,
                            "folder", webDavFile.lastModify
                        )
                    )
                } else {
                    //分割后缀
                    val fileExtension = webDavFile.displayName.substringAfterLast(".")

                    //扩展名符合阅读的格式则认为是书籍
                    if (bookFileRegex.matches(webDavFile.displayName)) {
                        val isOnBookShelf = LocalBook.isOnBookShelf(webDavFile.displayName)
                        remoteBooks.add(
                            RemoteBook(
                                webDavFile.displayName, webDavFile.path, webDavFile.size,
                                fileExtension, webDavFile.lastModify, isOnBookShelf
                            )
                        )
                    }
                }
            }
        } ?: throw NoStackTraceException("webDav没有配置")
        return remoteBooks
    }

    /**
     * 下载指定的远程书籍到本地
     */
    override suspend fun getRemoteBook(remoteBook: RemoteBook): Uri? {
        return AppWebDav.authorization?.let {
            val webdav = WebDav(remoteBook.path, it)
            webdav.download().let { bytes ->
                LocalBook.saveBookFile(bytes, remoteBook.filename)
            }
        }
    }

    /**
     * 上传本地导入的书籍到远程
     */
    override suspend fun upload(localBookUri: Uri): Boolean {
        if (!NetworkUtils.isAvailable()) return false

        val localBookName = localBookUri.path?.substringAfterLast(File.separator)
        val putUrl = "${rootBookUrl}${File.separator}${localBookName}"
        AppWebDav.authorization?.let {
            if (localBookUri.isContentScheme()) {
                WebDav(putUrl, it).upload(
                    byteArray = localBookUri.readBytes(appCtx),
                    contentType = "application/octet-stream"
                )
            } else {
                WebDav(putUrl, it).upload(localBookUri.path!!)
            }
        }
        return true
    }

    override suspend fun delete(remoteBookUrl: String): Boolean {
        AppWebDav.authorization?.let {
            return WebDav(remoteBookUrl, it).delete()
        }
        return false
    }

}