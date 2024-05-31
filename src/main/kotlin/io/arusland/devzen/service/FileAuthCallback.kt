package io.arusland.devzen.service

import io.arusland.devzen.config.Config
import io.arusland.devzen.error.TgDevzenException
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * File-based implementation of AuthCallback
 */
class FileAuthCallbackImpl(private val botApiService: TelegramBotApiService) : AuthCallback {
    override fun onAuth(): String {
        var key = ""
        val waitTil = LocalDateTime.now().plusMinutes(AUTH_CODE_TIMEOUT)

        botApiService.sendAlertMessage("Devzen Telegram: auth code request")

        while (key.isBlank()) {
            log.info("Please, save auth code from Telegram to file: {}", Config.AUTH_CODE_FILE)
            Thread.sleep(5000)

            if (LocalDateTime.now() > waitTil) {
                throw TgDevzenException("Auth code wait timeout")
            }

            if (Config.AUTH_CODE_FILE.exists()) {
                log.info("Reading auth code from {}...", Config.AUTH_CODE_FILE)
                key = Config.AUTH_CODE_FILE.readText().trim()
                log.info("Auth code read: {}", key)
            }
        }

        return key
    }

    override fun on2StepAuth(): String {
        botApiService.sendAlertMessage("Devzen Telegram: 2step auth code request")
        TODO("2step auth code need")
    }

    private companion object {
        const val AUTH_CODE_TIMEOUT = 10L // in minutes
        val log = LoggerFactory.getLogger(FileAuthCallbackImpl::class.java)!!
    }
}
