package lol.adel

import com.squareup.moshi.Moshi
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.engine.apache.Apache
import io.ktor.client.response.readText
import io.ktor.features.AutoHeadResponse
import io.ktor.features.CallLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.cio.KtorDefaultPool
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.jvm.javaio.toByteReadChannel
import java.time.Duration

fun List<Format>.bestAndSmallestAudio(): Format? =
    asSequence()
        .filter { it.audioQuality == AudioQuality.AUDIO_QUALITY_MEDIUM }
        .minBy { it.width }

suspend fun HttpClient.bestAndSmallestAudio(json: Moshi, id: VideoId): Format? =
    videoInfo(
        getString = { call(it.string).response.readText() },
        fromJson = { s, c -> json.adapter(c).fromJson(s.json) },
        videoId = id
    )?.streamingData?.formats?.bestAndSmallestAudio()

object Config {
    val PORT = System.getenv("PORT")?.toInt() ?: 8080
}

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

fun main(args: Array<String>) {
    val client = HttpClient(Apache)
    val json = Moshi.Builder().build()

    embeddedServer(Netty, port = Config.PORT) {
        install(AutoHeadResponse)
        install(CallLogging)

        routing {
            get("/") {
                call.request.queryParameters["v"]
                    ?.let { v -> client.bestAndSmallestAudio(json, VideoId(v)) }
                    ?.let { format ->
                        val command = FFMpeg(
                            input = format.url,
                            skipVideo = true,
                            format = "mp3",
                            output = FFMpegOutput.Pipe
                        ).toCommand()

                        Runtime.getRuntime().exec(command).use { ffmpeg ->
                            call.respond(object : OutgoingContent.ReadChannelContent() {

                                override val contentType: ContentType = ContentType.Audio.MPEG

                                override fun readFrom(): ByteReadChannel =
                                    ffmpeg.inputStream.toByteReadChannel(pool = KtorDefaultPool)
                            })
                        }
                    }
                    ?: call.respond(HttpStatusCode.NotFound)
            }
        }
    }.start(wait = true)
}

inline fun <T> Process.use(f: (Process) -> T): T =
    try {
        f(this)
    } finally {
        destroy()
    }
