package io.arusland.devzen.config

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File
import java.time.LocalDate


data class StorableConfig(val lastEpisode: LastEpisode?) {
    fun save(file: File = Config.DEF_CONFIG_FILE) {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, this)
    }

    companion object {
        private val objectMapper = ObjectMapper().registerModule(KotlinModule()).registerModule(JavaTimeModule())

        fun load(file: File = Config.DEF_CONFIG_FILE): StorableConfig {
            return if (file.exists())
                objectMapper.readValue(file, StorableConfig::class.java)
            else
                StorableConfig(lastEpisode = null)
        }
    }
}

data class LastEpisode(
    val episodeId: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val postDate: LocalDate
)
