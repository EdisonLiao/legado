package com.edison.ebookpub.utils

import cn.hutool.crypto.digest.DigestUtil
/**
 * 将字符串转化为MD5
 */
@Suppress("unused")
object MD5Utils {

    fun md5Encode(str: String?): String {
        return DigestUtil.digester("MD5").digestHex(str)
    }

    fun md5Encode16(str: String): String {
        var reStr = md5Encode(str)
        reStr = reStr.substring(8, 24)
        return reStr
    }
}
