package io.arusland.devzen.config

import com.github.badoualy.telegram.api.TelegramApp
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.*
import kotlin.system.exitProcess

/**
 * Main config file for the application.
 *
 * BOT_TOKEN can be obtained from BotFather
 * API_ID and API_HASH can be obtained from https://my.telegram.org/apps
 */
object Config {
    private val log = LoggerFactory.getLogger(Config::class.java)!!

    // dir for config files, auth files, etc
    val STATE_DIR = File("./state")

    // dir for downloaded files
    val FILES_DIR = File(STATE_DIR, "files")

    // used by [FileApiStorage] to store auth key
    val AUTH_KEY_FILE = File(STATE_DIR, "auth.key")

    // used by [FileApiStorage] to store nearest data center
    val NEAREST_DC_FILE = File(STATE_DIR, "dc.save")

    // used by [FileAuthCallbackImpl] only once to get auth code
    val AUTH_CODE_FILE = File(STATE_DIR, "auth.txt") // TODO: find better way to read auth code

    // used by [StorableConfig] to store last episode
    val DEF_CONFIG_FILE = File(STATE_DIR, "tg-devzen-config.json")

    private val properties = Properties().apply {
        val configFile = File(STATE_DIR, "config.properties")
        if (configFile.exists()) {
            try {
                log.info("Config file found: {}", configFile.absolutePath)
                load(FileInputStream(configFile))
            } catch (e: Exception) {
                log.error("Failed to load config file: {}", configFile, e)
                throw IllegalStateException("Failed to load config file: $configFile", e)
            }
        } else {
            log.info("Config file not found: {}, try get config from environment", configFile.absolutePath)
            // try to get settings from environment
            try {
                this["bot.token"] = getEnv("BOT_TOKEN")
                this["alert.chatId"] = getEnv("ALERT_CHAT_ID")
                this["apiId"] = getEnv("API_ID")
                this["apiHash"] = getEnv("API_HASH")
                this["phoneNumber"] = getEnv("PHONE_NUMBER")
                this["debug"] = System.getenv("DEBUG")
            } catch (ex: Exception) {
                log.error("Failed to load config from environment: {}", ex.message, ex)
                log.error("Please, provide config file or following environment variables: BOT_TOKEN, ALERT_CHAT_ID, API_ID, API_HASH, PHONE_NUMBER")
                exitProcess(1)
            }
        }
    }

    // used as channel for alerts and for debug purposes
    val alertChannelId: String get() = getProp("alert.chatId", "")
    // when true, send episodes+mp3 to alert channel
    val debug: Boolean get() = getProp("debug", "false").toBoolean()

    val apiId = getProp("apiId", "0").toIntOrNull() ?: 0
    val apiHash = getProp("apiHash", "")
    val deviceModel = getProp("deviceModel", "DeviceModel")
    val systemVersion = getProp("systemVersion", "SysVer")
    val appVersion = getProp("appVersion", "AppVersion")
    val langCode = getProp("langCode", "en")
    val phoneNumber = getProp("phoneNumber", "+33000000000")

    val botApiToken = getProp("bot.token", "")

    val liveTime: LocalTime = LocalTime.parse(getProp("liveTime", "22:30"))

    val liveWeekDay = DayOfWeek.valueOf(getProp("liveWeekDay", DayOfWeek.SATURDAY.name))

    val liveEnabled: Boolean = getProp("liveEnabled", "true").toBoolean()

    val exitWhenNoEpisode: Boolean = getProp("exitWhenNoEpisode", "true").toBoolean()

    val application = TelegramApp(
        apiId,
        apiHash,
        deviceModel,
        systemVersion,
        appVersion,
        langCode
    )

    private fun getProp(key: String, default: String) = properties.getProperty(key, default)!!

    private fun getEnv(name: String): String = System.getenv(name)
        ?: throw IllegalStateException("Environment variable $name not found")
}
