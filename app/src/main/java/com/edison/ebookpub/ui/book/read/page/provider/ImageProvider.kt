package com.edison.ebookpub.ui.book.read.page.provider

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Size
import androidx.collection.LruCache
import com.edison.ebookpub.R
import com.edison.ebookpub.constant.AppLog.putDebug
import com.edison.ebookpub.data.entities.Book
import com.edison.ebookpub.data.entities.BookSource
import com.edison.ebookpub.exception.NoStackTraceException
import com.edison.ebookpub.help.BookHelp
import com.edison.ebookpub.help.config.AppConfig
import com.edison.ebookpub.help.coroutine.Coroutine
import com.edison.ebookpub.model.localBook.EpubFile
import com.edison.ebookpub.utils.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import java.io.File
import java.io.FileOutputStream

object ImageProvider {

    private val errorBitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(appCtx.resources, R.drawable.image_loading_error)
    }

    /**
     * 缓存bitmap LruCache实现
     * filePath bitmap
     */
    private const val M = 1024 * 1024
    val cacheSize get() = AppConfig.bitmapCacheSize * M
    val bitmapLruCache = object : LruCache<String, Bitmap>(cacheSize) {

        override fun sizeOf(filePath: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }

        override fun entryRemoved(
            evicted: Boolean,
            filePath: String,
            oldBitmap: Bitmap,
            newBitmap: Bitmap?
        ) {
            //错误图片不能释放,占位用,防止一直重复获取图片
            if (oldBitmap != errorBitmap) {
                oldBitmap.recycle()
                putDebug("ImageProvider: trigger bitmap recycle. URI: $filePath")
                putDebug("ImageProvider : cacheUsage ${size()}bytes / ${maxSize()}bytes")
            }
        }
    }

    /**
     *缓存网络图片和epub图片
     */
    suspend fun cacheImage(
        book: Book,
        src: String,
        bookSource: BookSource?
    ): File {
        return withContext(IO) {
            val vFile = BookHelp.getImage(book, src)
            if (!vFile.exists()) {
                if (book.isEpub()) {
                    EpubFile.getImage(book, src)?.use { input ->
                        val newFile = FileUtils.createFileIfNotExist(vFile.absolutePath)
                        @Suppress("BlockingMethodInNonBlockingContext")
                        FileOutputStream(newFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } else {
                    BookHelp.saveImage(bookSource, book, src)
                }
            }
            return@withContext vFile
        }
    }

    /**
     *获取图片宽度高度信息
     */
    suspend fun getImageSize(
        book: Book,
        src: String,
        bookSource: BookSource?
    ): Size {
        val file = cacheImage(book, src, bookSource)
        val op = BitmapFactory.Options()
        // inJustDecodeBounds如果设置为true,仅仅返回图片实际的宽和高,宽和高是赋值给opts.outWidth,opts.outHeight;
        op.inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.absolutePath, op)
        if (op.outWidth < 1 && op.outHeight < 1) {
            Coroutine.async {
                putDebug("ImageProvider: delete file due to image size ${op.outHeight}*${op.outWidth}. path: ${file.absolutePath}")
                file.delete()
            }
            return Size(errorBitmap.width, errorBitmap.height)
        }
        return Size(op.outWidth, op.outHeight)
    }

    /**
     *获取bitmap 使用LruCache缓存
     */
    fun getImage(
        book: Book,
        src: String,
        width: Int,
        height: Int? = null
    ): Bitmap {
        //src为空白时 可能被净化替换掉了 或者规则失效
        if (book.getUseReplaceRule() && src.isBlank()) {
           book.setUseReplaceRule(false)
           appCtx.toastOnUi(R.string.error_image_url_empty)
        }
        val vFile = BookHelp.getImage(book, src)
        if (!vFile.exists()) return errorBitmap
        //epub文件提供图片链接是相对链接，同时阅读多个epub文件，缓存命中错误
        //bitmapLruCache的key同一改成缓存文件的路径
        val cacheBitmap = bitmapLruCache.get(vFile.absolutePath)
        if (cacheBitmap != null) return cacheBitmap
        @Suppress("BlockingMethodInNonBlockingContext")
        return kotlin.runCatching {
            val bitmap = BitmapUtils.decodeBitmap(vFile.absolutePath, width, height)
                ?: throw NoStackTraceException(appCtx.getString(R.string.error_decode_bitmap))
            bitmapLruCache.put(vFile.absolutePath, bitmap)
            bitmap
        }.onFailure {
            //错误图片占位,防止重复获取
            bitmapLruCache.put(vFile.absolutePath, errorBitmap)
            putDebug(
                "ImageProvider: decode bitmap failed. path: ${vFile.absolutePath}\n$it",
                it
            )
        }.getOrDefault(errorBitmap)
    }

}
