package io.arusland.devzen.util

import io.arusland.devzen.config.Config
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL

object UrlUtil {
    private val log = LoggerFactory.getLogger(UrlUtil::class.java)

    fun download(url: URL, targetDir: File = Config.FILES_DIR, useCache: Boolean = false): File {
        val fileName = getFileName(url.path, "file.bin")
        targetDir.mkdirs()
        val targetFile = File(targetDir, fileName)

        val lastTime = System.currentTimeMillis()
        log.info("Start downloading file: {} to: {}", url, targetFile)

        if (targetFile.exists() && useCache) {
            log.warn("File already exists: {}", targetFile)
        } else {
            targetFile.outputStream().use { output ->
                url.openStream().use { input -> IOUtils.copy(input, output) }
            }
            log.info(
                "File ({} bytes) downloaded in {} secs from {}",
                targetFile.length(), (System.currentTimeMillis() - lastTime) / 1000, url
            )
        }

        return targetFile
    }

    private fun getFileName(filePath: String, defName: String) = if (filePath.contains('.'))
        filePath.substring(filePath.lastIndexOf('/') + 1) else defName
}
