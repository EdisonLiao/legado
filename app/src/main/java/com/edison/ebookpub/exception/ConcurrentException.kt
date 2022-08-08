@file:Suppress("unused")

package com.edison.ebookpub.exception

/**
 * 并发限制
 */
class ConcurrentException(msg: String, val waitTime: Int) : NoStackTraceException(msg)