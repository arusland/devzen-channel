package io.arusland.devzen.service

import io.arusland.devzen.config.Config
import io.arusland.devzen.util.ThreadUtil
import org.slf4j.LoggerFactory
import java.lang.Thread.sleep
import java.time.Duration
import java.time.OffsetDateTime
import java.util.concurrent.Executors

/**
 * Сервис отправляет сообщение в канал за 10 минут до эфира
 *
 * Работает асинхронно
 */
class LiveNotificationDevzenService(
    private val chatId: String,
    private val botApiService: TelegramBotApiService
) : Runnable {
    private val log = LoggerFactory.getLogger(javaClass)
    private val executorService = Executors.newCachedThreadPool()

    fun start() {
        executorService.execute(this)
    }

    override fun run() {
        log.info("Calculating next live time...")

        while (true) {
            val nextLiveTime = getNextLiveTime()
            log.info("Notification will be fired at {}", nextLiveTime)

            ThreadUtil.sleepUntil(nextLiveTime)

            val html = notificationTemplate()
            val messageId = botApiService.sendHtmlMsg(chatId, html, disableNotification = false)

            executorService.submit {
                sleep(Duration.ofHours(4).toMillis())
                log.info("Try to delete notification message: {}", messageId)
                val success = botApiService.deleteMessage(chatId, messageId)
                botApiService.sendAlertMessage("Delete notification message, success: $success")
            }

            sleep(1000 * 60 * 2)
        }
    }

    /**
     * Получить дату следующего эфира - 10 минут
     */
    private fun getNextLiveTime(): OffsetDateTime {
        val now = ThreadUtil.getNow()

        val liveTime = Config.liveTime
        var nextLiveTime = now.withHour(liveTime.hour)
            .withMinute(liveTime.minute)
            .withSecond(0)
            .withNano(0)

        val liveWeekDay = Config.liveWeekDay
        while (nextLiveTime.dayOfWeek != liveWeekDay) {
            nextLiveTime = nextLiveTime.plusDays(1)
        }

        if (now.isAfter(nextLiveTime)) {
            nextLiveTime = nextLiveTime.plusWeeks(1)
        }

        return nextLiveTime
    }

    private fun notificationTemplate() =
        """🎙️Скорее всего, через <b>10 минут</b> <i>(22:30 MSK)</i> будет <a href="http://devzen.ru/live">прямой эфир</a> подкаста. Также можно пообщаться в <a href="https://t.me/devzen_live">телеграм-чате</a>"""
}
