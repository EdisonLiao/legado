package com.edison.ebookpub.model

import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import com.edison.ebookpub.constant.EventBus
import com.edison.ebookpub.constant.IntentAction
import com.edison.ebookpub.constant.Status
import com.edison.ebookpub.data.appDb
import com.edison.ebookpub.data.entities.Book
import com.edison.ebookpub.data.entities.BookChapter
import com.edison.ebookpub.data.entities.BookSource
import com.edison.ebookpub.help.ContentProcessor
import com.edison.ebookpub.help.coroutine.Coroutine
import com.edison.ebookpub.service.AudioPlayService
import com.edison.ebookpub.utils.postEvent
import com.edison.ebookpub.utils.startService
import splitties.init.appCtx

object AudioPlay {
    var titleData = MutableLiveData<String>()
    var coverData = MutableLiveData<String>()
    var status = Status.STOP
    var book: Book? = null
    var durChapter: BookChapter? = null
    var inBookshelf = false
    var bookSource: BookSource? = null
    val loadingChapters = arrayListOf<Int>()

    fun headers(hasLoginHeader: Boolean): Map<String, String>? {
        return bookSource?.getHeaderMap(hasLoginHeader)
    }

    /**
     * 播放当前章节
     */
    fun play(context: Context) {
        book?.let {
            if (durChapter == null) {
                upDurChapter(it)
            }
            durChapter?.let {
                context.startService<AudioPlayService> {
                    action = IntentAction.play
                }
            }
        }
    }

    /**
     * 更新当前章节
     */
    fun upDurChapter(book: Book) {
        durChapter = appDb.bookChapterDao.getChapter(book.bookUrl, book.durChapterIndex)
        postEvent(EventBus.AUDIO_SUB_TITLE, durChapter?.title ?: "")
        postEvent(EventBus.AUDIO_SIZE, durChapter?.end?.toInt() ?: 0)
        postEvent(EventBus.AUDIO_PROGRESS, book.durChapterPos)
    }

    fun pause(context: Context) {
        if (AudioPlayService.isRun) {
            context.startService<AudioPlayService> {
                action = IntentAction.pause
            }
        }
    }

    fun resume(context: Context) {
        if (AudioPlayService.isRun) {
            context.startService<AudioPlayService> {
                action = IntentAction.resume
            }
        }
    }

    fun stop(context: Context) {
        if (AudioPlayService.isRun) {
            context.startService<AudioPlayService> {
                action = IntentAction.stop
            }
        }
    }

    fun adjustSpeed(context: Context, adjust: Float) {
        if (AudioPlayService.isRun) {
            context.startService<AudioPlayService> {
                action = IntentAction.adjustSpeed
                putExtra("adjust", adjust)
            }
        }
    }

    fun adjustProgress(context: Context, position: Int) {
        if (AudioPlayService.isRun) {
            context.startService<AudioPlayService> {
                action = IntentAction.adjustProgress
                putExtra("position", position)
            }
        }
    }

    fun skipTo(context: Context, index: Int) {
        Coroutine.async {
            book?.let { book ->
                book.durChapterIndex = index
                book.durChapterPos = 0
                durChapter = null
                saveRead(book)
                play(context)
            }
        }
    }

    fun prev(context: Context) {
        Coroutine.async {
            book?.let { book ->
                if (book.durChapterIndex <= 0) {
                    return@let
                }
                book.durChapterIndex = book.durChapterIndex - 1
                book.durChapterPos = 0
                durChapter = null
                saveRead(book)
                play(context)
            }
        }
    }

    fun next(context: Context) {
        book?.let { book ->
            if (book.durChapterIndex >= book.totalChapterNum) {
                return@let
            }
            book.durChapterIndex = book.durChapterIndex + 1
            book.durChapterPos = 0
            durChapter = null
            saveRead(book)
            play(context)
        }
    }

    fun addTimer() {
        val intent = Intent(appCtx, AudioPlayService::class.java)
        intent.action = IntentAction.addTimer
        appCtx.startService(intent)
    }

    fun setTimer(minute: Int) {
        val intent = Intent(appCtx, AudioPlayService::class.java)
        intent.action = IntentAction.setTimer
        intent.putExtra("minute", minute)
        appCtx.startService(intent)
    }

    fun saveRead(book: Book) {
        book.lastCheckCount = 0
        book.durChapterTime = System.currentTimeMillis()
        Coroutine.async {
            appDb.bookChapterDao.getChapter(book.bookUrl, book.durChapterIndex)?.let {
                book.durChapterTitle = it.getDisplayTitle(
                    ContentProcessor.get(book.name, book.origin).getTitleReplaceRules()
                )
            }
            book.save()
        }
    }

    /**
     * 保存章节长度
     */
    fun saveDurChapter(audioSize: Long) {
        Coroutine.async {
            durChapter?.let {
                it.end = audioSize
                appDb.bookChapterDao.upDate(it)
            }
        }
    }
}