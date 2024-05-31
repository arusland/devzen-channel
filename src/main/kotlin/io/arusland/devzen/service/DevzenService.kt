package io.arusland.devzen.service

import io.arusland.devzen.parser.DevzenParser
import io.arusland.devzen.parser.Episode
import org.apache.commons.lang3.Validate
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.net.URL
import java.util.*

class DevzenService {
    fun loadEpisode(episodeId: String): Episode? {
        val episodeUrl = "https://devzen.ru/episode-$episodeId/"

        try {
            log.info("Try download episode ({}): {}", episodeId, episodeUrl)
            val content = URL(episodeUrl).openStream().use { String(it.readBytes()) }
            val parser = DevzenParser()
            val episode = parser.parse(content, episodeId, episodeUrl)

            log.info("Parsed episode: {}", episode)

            return episode
        } catch (e: FileNotFoundException) {
            log.info("Episode ({}) not found by url: {}", episodeId, episodeUrl)

            // TODO: быстрое решение, переписать в будущем
            if (episodeId.length == 4 && episodeId.startsWith('0')) {
                return loadEpisode(episodeId.substring(1))
            }

            return null
        }
    }

    fun calcNextEpisodeId(prevEpisodeId: String): String {
        Validate.notBlank(prevEpisodeId, "prevEpisodeId")

        return String.format("%04d", prevEpisodeId.toInt() + 1)
    }

    fun episode2Html(e: Episode): String {
        val description = themeTitle(escapeBasic(e.description), e)
        val title = escapeBasic(e.title) + if (e.id == "0113") " — Episode 0113" else ""


        val url = if (e.title.contains("Очень Надежный Выпуск")) "https://devzen.ru/episode-0239/" else e.url
        val res = """<b>${title}</b>
<i>Опубликовано</i> <a href="${url}">${e.date}</a>

${description}

"""
        val sb = StringBuilder()

        if (e.links.isNotEmpty()) {
            sb.append("<b>Шоу нотес:</b>\n")

            e.links.forEach { l ->
                run {
                    val title = escapeBasic(l.title)
                    sb.append("""<a href="${l.url}">$title</a>""")
                    sb.append("\n")
                }
            }
        }

        if (e.gitterLink != null) {
            sb.append("\n<b>Лог чата:</b> ${e.gitterLink!!.url}\n")
        }

        if (e.members.isNotEmpty()) {
            sb.append("\n<b>Участники:</b>\n")

            e.members.forEach { l ->
                run {
                    val title = escapeBasic(l.title)
                    sb.append("""<a href="${l.url}">$title</a>""")
                    sb.append("\n")
                }
            }
        }

        return res + sb.toString()
    }

    fun getLastThemeUrl(): String {
        val content = URL("https://devzen.ru").openStream().use { String(it.readBytes()) }
        val doc = Jsoup.parse(content)

        return doc.select("h1.entry-title a")
            .map { it.attr("href") ?: "" }
            .firstOrNull { it.contains("/themes-") } ?: ""
    }

    private fun themeTitle(description: String, e: Episode): String {
        return if (description.contains("Темы выпуска:")) {
            description.replace("Темы выпуска:", "<b>Темы выпуска:</b>")
        } else if (description.contains("Тема выпуска:")) {
            description.replace("Тема выпуска:", "<b>Тема выпуска:</b>")
        } else if (description.contains("В этом выпуске:")) {
            description.replace("В этом выпуске:", "<b>В этом выпуске:</b>")
        } else if (description.contains("В этом выпуске.")) {
            description.replace("В этом выпуске.", "<b>В этом выпуске:</b>")
        } else if (description.contains("В этом выпуске ")) {
            description.replace("В этом выпуске ", "<b>В этом выпуске</b> ")
        } else if (description.contains("Сегодня вы узнаете:")) {
            description.replace("Сегодня вы узнаете:", "<b>Сегодня вы узнаете:</b>")
        } else if (Arrays.asList(
                "0034", "0035", "0123", "0126",
                "0127", "0132", "0166", "0167", "0171", "0174",
                "0175", "0176", "0264"
            ).contains(e.id)
        ) {
            "<b>Темы выпуска:</b> " + description
        } else if (e.id.endsWith("a") || description.startsWith("тем", true)) {
            description
        } else {
            "<b>Темы выпуска:</b> " + description
        }
    }

    private fun escapeBasic(input: String): String {
        var input = input
        if (input.contains("&")) {
            input = input.replace("&", "&amp;")
        }

        if (input.contains("\"")) {
            input = input.replace("\"", "&quot;")
        }

        if (input.contains("<")) {
            input = input.replace("<", "&lt;")
        }

        if (input.contains(">")) {
            input = input.replace(">", "&gt;")
        }

        return input
    }

    companion object {
        private val log = LoggerFactory.getLogger(DevzenService::class.java)
    }
}
