package io.arusland.devzen

import io.arusland.devzen.service.MainService
import io.arusland.devzen.service.TelegramBotApiService
import io.arusland.devzen.util.ProxyAuth
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.net.Authenticator


object MainApp {
    @JvmStatic
    fun main(args: Array<String>) {
        // if your country blocks Telegram use socks proxy
        val socksUsername = System.getProperty("java.net.socks.username")
        val socksPassword = System.getProperty("java.net.socks.password")

        if (socksUsername != null && socksPassword != null &&
            socksUsername.isNotBlank() && socksPassword.isNotBlank()
        ) {
            log.info(
                "using SOCKS: socksUsername: {}, host: {}:{}", socksUsername,
                System.getProperty("socksProxyHost"), System.getProperty("socksProxyPort")
            )
            Authenticator.setDefault(ProxyAuth(socksUsername, socksPassword))
        }

        try {
            val main = MainService()
            main.init()
            main.run()
        } catch (e: Exception) {
            log.error("Application failed: {}", e.message, e)
            TelegramBotApiService.create().sendAlertMessage(
                """TgDevzen failed: ${e.message}
                |
                |${StringUtils.abbreviate(e.toString(), 1000)}
            """.trimMargin()
            )

            System.exit(1)
        }

    }

    private val log = LoggerFactory.getLogger(MainApp::class.java)!!
}


