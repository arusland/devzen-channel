package io.arusland.devzen.util

import com.mpatric.mp3agic.Mp3File
import java.io.File

object Mp3Util {
    fun extract(file: File): Mp3Info {
        val mp3File = Mp3File(file)

        if (mp3File.hasId3v1Tag()) {
            val tag = mp3File.id3v1Tag

            return Mp3Info(performer = tag.artist ?: ARTIST_UNKNOWN,
                    title = tag.title ?: TITLE_UNKNOWN,
                    duration = mp3File.lengthInSeconds.toInt())
        }

        if (mp3File.hasId3v2Tag()) {
            val tag = mp3File.id3v2Tag

            return Mp3Info(performer = tag.artist ?: ARTIST_UNKNOWN,
                    title = tag.title ?: TITLE_UNKNOWN,
                    duration = mp3File.lengthInSeconds.toInt())
        }

        return Mp3Info(performer = ARTIST_UNKNOWN,
                title = TITLE_UNKNOWN,
                duration = mp3File.lengthInSeconds.toInt())
    }

    private const val ARTIST_UNKNOWN = "Unknown"
    private const val TITLE_UNKNOWN = "Untitled"
}

data class Mp3Info(val performer: String, val title: String, val duration: Int)
