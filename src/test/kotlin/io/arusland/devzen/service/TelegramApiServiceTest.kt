package io.arusland.devzen.service

import io.arusland.devzen.config.Config
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

class TelegramApiServiceTest {
     private val file = File("devzen.json")

    @Test
    @Disabled
    fun testSendFileTwice() {
        TelegramApiService.create().use {
            it.sendFile(Config.alertChannelId, file)
        }

        TelegramApiService.create().use {
            it.sendFile(Config.alertChannelId, file)
        }
    }

    @Test
    fun testGetChats() {
        val api = TelegramApiService.create()

        api.loadAllChats()
    }

    @Test
    fun testFindInputPeer() {
        val api = TelegramApiService.create()
        val peer = api.findInputPeer("@devzen")!!

        println(peer::class.java)
    }
}
