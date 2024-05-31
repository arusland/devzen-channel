package io.arusland.devzen.parser

import org.apache.commons.lang3.Validate
import org.jsoup.Jsoup
import java.io.File
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author Ruslan Absalyamov
 * @since 1.0
 */
class DevzenParser {
    fun parse(siteRoot: File): List<Episode> {
        val dirs = siteRoot.listFiles { fi -> fi.isDirectory && fi.name.startsWith("episode-") }

        return dirs.map { d -> convertToEpisode(d) }.sortedBy { p -> p.id }
    }

    fun parse(html: String, id: String, url: String): Episode {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy")
        val doc = Jsoup.parse(html)
        val date = dateFormat.parse(doc.select("time.entry-date").first()?.text() ?: dateFormat.format(Date()))
        val allLinks = doc.select("div.entry-content li").select("a")
        val links = allLinks.filter { p -> p.hasText() && p.attr("href").isNotBlank() }
                .map { Link(title = it.text(), url = normalizeUrl(it.attr("href"))) }
        val description = doc.select("div.entry-content p")
                .filter { p -> p.text().isNotBlank() }
                .first().text().trim()
        val title = doc.select("h1.entry-title").first()?.text()?.trim() ?: ""

        val ddd = doc.select("div.entry-content p")
                .filter { p -> p.text().contains("Голоса выпуска", true) }
                .firstOrNull()

        val members = if (ddd != null) {
            ddd.select("a")
                    .stream()
                    .filter { it.attr("href").isNotBlank() }
                    .map { Link(title = it.text(), url = normalizeUrl(it.attr("href"))) }
                    .toList()
        } else {
            Collections.emptyList()
        }

        if (members.isEmpty() && !id.endsWith("a")) {
            throw RuntimeException("Members cannot be found: " + url)
        }

        val gitter = doc.select("div.entry-content a")
                .filter { p -> p.attr("href").isNotBlank() && p.attr("href").contains("gitter.im/DevZenRu") }
                .firstOrNull();
        val gitterLink = if (gitter != null) Link(title = gitter.text(), url = gitter.attr("href")) else null

        val thumbImageLink = doc.select(".entry-content img")
                .map { it.attr("src") }
                .filter { it.isNotBlank() }
                .map { normalizeUrl(it) }
                .firstOrNull()

        val downloadLink = doc.select("a.powerpress_link_d")
                .map { it.attr("href") }
                .filter { it.isNotBlank() }
                .map { normalizeUrl(it) }
                .firstOrNull()

        Validate.notBlank(description, "description")
        Validate.notBlank(title, "title")

        return Episode(id = id,
                title = title,
                url = url,
                description = description,
                date = dateFormat.format(date),
                members = members,
                links = links,
                gitterLink = gitterLink,
                thumbImageLink = thumbImageLink,
                downloadLink = downloadLink)
    }

    private fun convertToEpisode(episodeDir: File?): Episode {
        if (episodeDir != null) {
            val id = episodeDir.name.substring(episodeDir.name.lastIndexOf("-") + 1)
            val file = File(episodeDir, "index.html")
            val html = file.readText(StandardCharsets.UTF_8)
            var url = "https://devzen.ru/" + episodeDir.name


            return parse(html, id, url)
        }

        throw RuntimeException("episodeDir is null")
    }

    private fun normalizeUrl(url: String?): String {
        if (url == null || url.isBlank()) {
            throw RuntimeException("url cannot be empty")
        }

        var resUrl = url

        if (resUrl.startsWith("../")) {
            resUrl = url.replace("../", "https://devzen.ru/")
        }

        if (resUrl.endsWith("/index.html")) {
            resUrl = resUrl.replace("/index.html", "")
        }

        return resUrl
    }
}
