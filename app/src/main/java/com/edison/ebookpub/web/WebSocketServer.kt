package com.edison.ebookpub.web

import fi.iki.elonen.NanoWSD
import com.edison.ebookpub.web.socket.BookSourceDebugWebSocket
import com.edison.ebookpub.web.socket.RssSourceDebugWebSocket

class WebSocketServer(port: Int) : NanoWSD(port) {

    override fun openWebSocket(handshake: IHTTPSession): WebSocket? {
        return when (handshake.uri) {
            "/bookSourceDebug" -> {
                BookSourceDebugWebSocket(handshake)
            }
            "/rssSourceDebug" -> {
                RssSourceDebugWebSocket(handshake)
            }
            else -> null
        }
    }
}
