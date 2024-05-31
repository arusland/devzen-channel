package io.arusland.devzen.service

import io.arusland.devzen.config.Config
import io.arusland.devzen.config.LastEpisode
import io.arusland.devzen.config.StorableConfig
import io.arusland.devzen.error.AuthNotRegisteredException
import io.arusland.devzen.error.TgDevzenException
import io.arusland.devzen.util.ThreadUtil
import io.arusland.devzen.util.UrlUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.Thread.sleep
import java.net.URL
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

/**
 * Main logic is here
 */
class MainService {
    private val debug = Config.debug
    private val chatId = if (debug) Config.alertChannelId else "@devzen"
    private val devzenService = DevzenService()
    private val botApiService = TelegramBotApiService.create()
    private val waitLiveDevzenService = LiveNotificationDevzenService(chatId, botApiService)

    fun init() {
        TelegramApiService.create().use {
            try {
                // check validity of auth
                it.checkAuth()
            } catch (e: AuthNotRegisteredException) {
                // try to sign in
                it.trySignIn(FileAuthCallbackImpl(botApiService))
            }
        }

        log.info("Telegram Api Service auth checked")
        log.info("Application config: {}", Config)
    }

    fun run() {
        if (Config.liveEnabled) {
            waitLiveDevzenService.start()
        }
        var state = State.PostNextEpisode

        while (true) {
            val oldState = state
            state = when (state) {
                State.PostNextEpisode -> postNextEpisode()
                State.WaitNextDayAfterLive -> waitNextDayAfterLive()
                State.WaitNextEpisode -> waitNexEpisode()
                State.ExitProgram -> {
                    log.info("Exiting program")
                    exitProcess(0)
                }
            }

            if (oldState != state) {
                log.info("Switched state from {} to {}", oldState, state)

                if (state == State.WaitNextDayAfterLive) {
                    botApiService.sendAlertMessage("Switched state from $oldState to $state")
                }
            }
        }
    }

    private fun postNextEpisode(): State {
        val config = StorableConfig.load()
        val lastEpisode = config.lastEpisode ?: throw TgDevzenException("lastEpisodeId not configured")

        log.info("Loaded with lastEpisodeId: {}", lastEpisode)
        val nextEpisodeId = devzenService.calcNextEpisodeId(lastEpisode.episodeId)
        val episode = devzenService.loadEpisode(nextEpisodeId)

        if (episode != null) {
            log.info("Sending episode to telegram: {}", episode)

            if (episode.downloadLink != null) {
                botApiService.sendAlertMessage("Start loading ${episode.downloadLink}")
                val file = UrlUtil.download(URL(episode.downloadLink), useCache = debug)

                val episodeHtml = devzenService.episode2Html(episode)
                File(Config.FILES_DIR, "devzen-${episode.id}.html").writeText(episodeHtml)
                val disableNotification = false

                botApiService.sendHtmlMsg(chatId, episodeHtml, disableNotification)

                TelegramApiService.create().use {
                    it.sendFile(chatId, file)
                }
            } else {
                botApiService.sendAlertMessage("Mp3 not found for episode: ${episode.id}")
            }

            val newConfig = config.copy(
                lastEpisode = LastEpisode(
                    episodeId = nextEpisodeId,
                    postDate = LocalDate.parse(episode.date, dateFormatter)
                )
            )
            newConfig.save()

            return State.PostNextEpisode
        }

        if (Config.exitWhenNoEpisode) {
            log.info("Next episode not found: {}", nextEpisodeId)
            return State.ExitProgram
        }

        val today = LocalDate.now(ThreadUtil.TIME_ZONE.toZoneId())
        val daysAgo = today.dayOfYear - lastEpisode.postDate.dayOfYear

        return if (daysAgo < 6) State.WaitNextDayAfterLive else State.WaitNextEpisode
    }

    /**
     * Метод ожидает следующего дня после эфира
     */
    private fun waitNextDayAfterLive(): State {
        val now = ThreadUtil.getNow()

        var nextTime = now.withHour(10)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        // если сегодня следующий день после эфира, то пропускаем. Нам нужен этот день на следующей неделе
        val nextDayAfterLive = Config.liveWeekDay.plus(1)
        while (nextTime.dayOfWeek == nextDayAfterLive) {
            nextTime = nextTime.plusDays(1)
        }

        while (nextTime.dayOfWeek != nextDayAfterLive) {
            nextTime = nextTime.plusDays(1)
        }

        ThreadUtil.sleepUntil(nextTime)

        return State.PostNextEpisode
    }

    /**
     * Ждет короткое время в течении дня появления следующего эпизода
     */
    private fun waitNexEpisode(): State {
        val now = ThreadUtil.getNow()
        val nextDayAfterLive = Config.liveWeekDay.plus(1)
        val nextDayAfterLive2 = Config.liveWeekDay.plus(2)

        if (now.dayOfWeek == nextDayAfterLive) {
            sleep(SLEEP_TIME_SHORT)
        } else if (now.dayOfWeek == nextDayAfterLive2) {
            sleep(SLEEP_TIME_SHORT * 2)
        } else {
            sleep(SLEEP_TIME_LONG)
        }

        return State.PostNextEpisode
    }

    companion object {
        private val log = LoggerFactory.getLogger(MainService::class.java)
        private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        private val SLEEP_TIME_SHORT = Duration.ofMinutes(10).toMillis()
        private val SLEEP_TIME_LONG = Duration.ofHours(1).toMillis()
    }

    enum class State {
        PostNextEpisode,

        WaitNextDayAfterLive,

        WaitNextEpisode,

        ExitProgram
    }
}
