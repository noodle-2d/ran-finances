package com.ran.kolibri.common.client.telegram.model

interface SendMessageRequest

data class SendTextMessageRequest(
    var chatId: Int? = null,
    var text: String? = null
) : SendMessageRequest

data class SendButtonMessageRequest(
    var chatId: Int? = null,
    var text: String? = null,
    var replyMarkup: ReplyMarkup? = null
) : SendMessageRequest
