package io.arusland.devzen.util

import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*

object ThreadUtil {
    private val log = LoggerFactory.getLogger(ThreadUtil::class.java)!!

    fun sleepUntil(nextTime: OffsetDateTime) {
        val now = getNow()
        val diff = ChronoUnit.MILLIS.between(now, nextTime)

        if (diff > 0) {
            log.info("Sleep until {}, duration: {}", nextTime, Duration.ofMillis(diff))
            Thread.sleep(diff)
        }
    }

    fun getNow(): OffsetDateTime = OffsetDateTime.now(TIME_ZONE.toZoneId())

    val TIME_ZONE = TimeZone.getTimeZone("GMT+3:00")!!
}
