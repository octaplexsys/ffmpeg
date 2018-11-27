package lol.adel

import java.time.Duration

fun parseDuration(trim: String): Duration {
    val segments = trim.split(":")
    val (hours, minutes) = segments.take(2).map(String::toLong)
    val (seconds, nanos) = segments.last().split(".").map(String::toLong)
    return Duration.ofHours(hours) + Duration.ofMinutes(minutes) + Duration.ofSeconds(seconds, nanos)
}

inline class Bitrate(val bps: Long) {
    companion object {
        fun ofKbps(kbps: Long): Bitrate =
            Bitrate(bps = 1024 * kbps)
    }
}

fun Bitrate.toKbps(): Long =
    bps / 1024

inline class Bitsize(val bits: Long)

fun Bitsize.toBytes(): Long =
    bits / 8

fun Bitsize.toKiloBytes(): Long =
    bits / 1024 / 8

operator fun Bitrate.times(d: Duration): Bitsize =
    Bitsize(bits = d.seconds * bps)

