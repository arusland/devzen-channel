package io.arusland.devzen.service

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.DeleteMessage
import com.pengrad.telegrambot.request.SendMessage
import io.arusland.devzen.config.Config
import org.slf4j.LoggerFactory

class TelegramBotApiService {
    fun sendHtmlMsg(chatId: String, html: String, disableNotification: Boolean): Int {
        val request = SendMessage(chatId, html)
        request.parseMode(ParseMode.HTML)
        request.disableWebPagePreview(true)
        request.disableNotification(disableNotification)
        val api = TelegramBot(Config.botApiToken)
        val sendResponse = api.execute(request)

        if (sendResponse != null) {
            if (sendResponse.isOk) {
                return sendResponse.message().messageId()
            }

            throw RuntimeException("Telegram send message failed: " + sendResponse.description())
        }

        throw RuntimeException("Telegram send message failed: empty response")
    }

    fun sendAlertMessage(text: String) {
        val request = SendMessage(Config.alertChannelId, text)
        request.disableWebPagePreview(true)
        val api = TelegramBot(Config.botApiToken)
        val sendResponse = api.execute(request)

        if (sendResponse != null) {
            if (sendResponse.isOk) {
                return
            }

            log.error("Telegram send message failed: {}", sendResponse.description())
        }

        log.error("Telegram send message failed: empty response")
    }

    fun deleteMessage(chatId: String, messageId: Int): Boolean {
        val request = DeleteMessage(chatId, messageId)
        val api = TelegramBot(Config.botApiToken)
        val sendResponse = api.execute(request)

        if (sendResponse != null) {
            if (sendResponse.isOk) {
                return true
            }

            log.error("Telegram delete message failed: {}", sendResponse.description())
        }

        log.error("Telegram delete message failed: empty response")

        return false
    }

    companion object {
        fun create(): TelegramBotApiService {
            return TelegramBotApiService()
        }

        private val log = LoggerFactory.getLogger(TelegramBotApiService::class.java)
    }
}
