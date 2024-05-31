package io.arusland.devzen.service

import com.github.badoualy.telegram.api.Kotlogram
import com.github.badoualy.telegram.api.TelegramClient
import com.github.badoualy.telegram.api.utils.id
import com.github.badoualy.telegram.api.utils.title
import com.github.badoualy.telegram.mtproto.secure.RandomUtils
import com.github.badoualy.telegram.tl.api.*
import com.github.badoualy.telegram.tl.api.auth.TLAuthorization
import com.github.badoualy.telegram.tl.core.TLBytes
import com.github.badoualy.telegram.tl.core.TLVector
import com.github.badoualy.telegram.tl.exception.RpcErrorException
import io.arusland.devzen.config.Config
import io.arusland.devzen.config.FileApiStorage
import io.arusland.devzen.error.AuthNotRegisteredException
import io.arusland.devzen.error.TgDevzenException
import io.arusland.devzen.util.Mp3Info
import io.arusland.devzen.util.Mp3Util
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import kotlin.collections.HashMap

class TelegramApiService(private val client: TelegramClient) : Closeable {
    // TODO: avoid using hardcoded channel id
    private val DEVZEN_CHANNEL = TLInputPeerChannel(1279979093, 5822242522685807834L)

    fun checkAuth() {
        findInputPeerInternal("kuber9000") ?: throw TgDevzenException("User not found")
    }

    fun trySignIn(callback: AuthCallback): TLAuthorization? {
        // Send code to account
        val sentCode = client.authSendCode(false, Config.phoneNumber, true)
        log.info("Wait for auth code by phone: {}", Config.phoneNumber)
        val code = callback.onAuth()
        log.info("Got auth code: {}", code)

        if (code.isNotBlank()) {
            // Auth with the received code
            val authorization =
                    try {
                        client.authSignIn(Config.phoneNumber, sentCode.phoneCodeHash, code)
                    } catch (e: RpcErrorException) {
                        if ("SESSION_PASSWORD_NEEDED".equals(e.tag, true)) {
                            // We receive this error is two-step auth is enabled
                            log.info("Wait for two-step auth code...")
                            val password = callback.on2StepAuth()
                            log.info("Got auth password")
                            client.authCheckPassword(password)
                        } else throw TgDevzenException(e)
                    }

            authorization.user.asUser.apply {
                log.info("You are now signed in as $firstName $lastName @$username")
            }

            return authorization
        }

        return null
    }

    fun loadAllChats(): List<String> {
        val tlAbsDialogs = client.messagesGetDialogs(false, 0, 0, TLInputPeerEmpty(), 10)
        val channels = tlAbsDialogs.chats

        channels.forEach {
            when (it) {
                is TLChannel -> println("username: ${it.username}, title: ${it.title}, ${it.id}, accessHash: ${it.accessHash}")
                is TLChat -> println("title: ${it.title}, id: ${it.id}, participantsCount: ${it.participantsCount}")
            }
        }

        return emptyList()
    }

    fun showDialogs() {
        try {
            val tlAbsDialogs = client.messagesGetDialogs(true, 0, 0, TLInputPeerEmpty(), 10)

            // Create a map of id to name map
            val nameMap = HashMap<Int, String>()
            tlAbsDialogs.users.filterIsInstance<TLUser>()
                    .map { Pair(it.id, "${it.firstName} ${it.lastName}") }
                    .toMap(nameMap)
            tlAbsDialogs.chats.map { Pair(it.id, it.title ?: "") }.toMap(nameMap)

            val messageMap = tlAbsDialogs.messages.map { Pair(it.id, it) }.toMap()

            tlAbsDialogs.dialogs.forEach { dialog ->
                val topMessage = messageMap[dialog.topMessage]!!
                val topMessageContent =
                        if (topMessage is TLMessage) topMessage.message
                        else if (topMessage is TLMessageService) "Service: ${topMessage.action}"
                        else "Empty message (TLMessageEmpty)"

                println("${nameMap[dialog.peer.id]}: $topMessageContent")
            }
        } catch (e: Exception) {
            if (e is RpcErrorException && e.code == 401) {
                throw AuthNotRegisteredException(e)
            }

            throw TgDevzenException(e)
        }
    }

    fun sendFile(chatId: String, file: File) {
        sendFile(chatId, file.name, file)
    }

    fun sendFile(chatId: String, name: String, file: File) {
        val inputPeer = findInputPeerInternal(chatId) ?: throw TgDevzenException("Chat not found: $chatId")

        sendFile(inputPeer, name, file, DEFAULT_PART_SIZE)
    }

    fun findInputPeer(chatId: String): TLAbsInputPeer? {
        val inputPear = findInputPeerInternal(chatId)

        return inputPear
    }

    private fun sendFile(inputPeer: TLAbsInputPeer, name: String, file: File, partSize: Int) {
        if (file.length() > 0) {
            val fileId = RandomUtils.randomLong()
            val totalParts = Math.toIntExact((file.length() + (partSize - 1)) / partSize)
            var filePart = 0
            // TODO: parse video tags
            val mp3Info = if (file.name.endsWith(".mp3")) Mp3Util.extract(file) else null

            if (mp3Info != null)
                log.info("Sending file, size: {}, parts: {}, mp3 info: {}, path: {}", file.length(), totalParts, mp3Info, file)
            else
                log.info("Sending file, size: {}, parts: {}, path: {}", file.length(), totalParts, file)

            try {
                FileInputStream(file).use { stream ->
                    val buffer = ByteArray(partSize)
                    var read = stream.read(buffer, 0, partSize)

                    while (read != -1) {
                        val bytes = TLBytes(buffer, 0, read)
                        client.uploadSaveBigFilePart(fileId, filePart++, totalParts, bytes)
                        val percent = filePart * 100 / totalParts

                        log.debug("Sending file fileId: {}, percent: {}%, file: {}", fileId, percent, file)
                        read = stream.read(buffer, 0, partSize)
                    }
                }
            } catch (e: Exception) {
                log.error("Error uploading file to server", e)
            }
            sendToChannel(client, inputPeer, name, fileId, totalParts, mp3Info)
        } else {
            log.warn("File is empty: {}", file)
        }
    }

    private fun sendToChannel(telegramClient: TelegramClient, tlInputPeerChannel: TLAbsInputPeer,
                              name: String, fileId: Long, totalParts: Int, mp3Info: Mp3Info?) {
        try {
            val mimeType = getMimeType(name)

            val attributes = TLVector<TLAbsDocumentAttribute>()
            attributes.add(TLDocumentAttributeFilename(name))

            if (mp3Info != null) {
                attributes.add(TLDocumentAttributeAudio(false, mp3Info.duration,
                        mp3Info.title, mp3Info.performer, null))
            }

            val inputFileBig = TLInputFileBig(fileId, totalParts, name)
            val document = TLInputMediaUploadedDocument(inputFileBig, mimeType, attributes, "", null)
            val tlAbsUpdates = telegramClient.messagesSendMedia(false, false, false,
                    tlInputPeerChannel, null, document, RandomUtils.randomLong(), null)
        } catch (e: Exception) {
            log.error("Error sending file by id into channel", e)
            throw TgDevzenException("File upload failed: $name", e)
        }
    }

    private fun getMimeType(name: String) = if (name.endsWith(".mp3")) "audio/mpeg" else
        name.substring(name.indexOf(".") + 1)

    private fun findInputPeerInternal(chatId: String, lastId: Int = 0, counter: Int = 0): TLAbsInputPeer? {
        try {
            log.info("findInputPeer chatId: {}, lastId: {}, counter: {}", chatId, lastId, counter)

            val tlAbsDialogs = client.messagesGetDialogs(false, 0, lastId, TLInputPeerEmpty(), 100)
            val channels = tlAbsDialogs.chats

            val channel = channels.find {
                it.id.toString() == chatId
                        || "-100${it.id}" == chatId
                        || it is TLChannel && (it.username == chatId || "@${it.username}" == chatId)
            }

            if (channel != null) {
                log.info("By chatId: {} found: {}", chatId, channel)

                return when (channel) {
                    is TLChannel -> TLInputPeerChannel(channel.id, channel.accessHash)
                    is TLChat -> TLInputPeerChat(channel.id)
                    else -> throw TgDevzenException("Unsupported type: ${channel.javaClass}")
                }
            }

            val users = tlAbsDialogs.users
            val user = users.find {
                it.id.toString() == chatId
                        || "-100${it.id}" == chatId
                        || it is TLUser && (it.username == chatId || "@${it.username}" == chatId)
            }

            if (user != null) {
                log.info("By chatId: {} found: {}", chatId, user)

                return when (user) {
                    is TLUser -> TLInputPeerUser(user.id, user.accessHash)
                    else -> throw TgDevzenException("Unsupported type: ${user.javaClass}")
                }
            }

            if (chatId == "@devzen") {
                return DEVZEN_CHANNEL
            }

            return null
        } catch (e: Exception) {
            if (e is RpcErrorException && e.code == 401) {
                throw AuthNotRegisteredException(e)
            }

            throw TgDevzenException(e)
        }
    }

    override fun close() {
        client.close(shutdown = false)
    }

    companion object {
        fun create(): TelegramApiService {
            // This is a synchronous client, that will block until the response arrive (or until timeout)
            val client = Kotlogram.getDefaultClient(Config.application, FileApiStorage())

            return TelegramApiService(client)
        }

        private val log = LoggerFactory.getLogger(TelegramApiService::class.java)
        private const val DEFAULT_PART_SIZE = 512 * 1024
    }
}

interface AuthCallback {
    fun onAuth(): String

    fun on2StepAuth(): String
}
