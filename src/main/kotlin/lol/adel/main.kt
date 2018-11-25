package lol.adel

import com.squareup.moshi.Moshi
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.call
import io.ktor.client.engine.apache.Apache
import io.ktor.client.response.readText
import io.ktor.features.AutoHeadResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.OutgoingContent
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.copyAndClose
import java.util.*

fun List<Format>.bestAndSmallestAudio(): Format? =
    filter { it.audioQuality == AudioQuality.AUDIO_QUALITY_MEDIUM }.minBy { it.bitrate.rate }
        ?: minBy { it.bitrate.rate }

suspend fun HttpClient.bestAndSmallestAudio(json: Moshi, id: VideoId): Format? =
    videoInfo(
        getString = { call(it.url).response.readText() },
        fromJson = { s, c -> json.adapter(c).fromJson(s.json) },
        videoId = id
    )?.streamingData?.formats?.bestAndSmallestAudio()

object Config {
    val PORT = System.getenv("PORT")?.toInt() ?: 8080
    val FFMPEG_PORT = PORT + 1
}

suspend fun proxy(from: HttpClientCall, to: ApplicationCall) {

    val original = from.response
    val modified = to.response

    modified.status(original.status)
    modified.header(HttpHeaders.AcceptRanges, value = "bytes")

    to.respond(object : OutgoingContent.WriteChannelContent() {

        override val contentType: ContentType? = ContentType.Audio.MPEG

        override val contentLength: Long? = null

        override suspend fun writeTo(channel: ByteWriteChannel) {
            original.content.copyAndClose(channel)
        }
    })
}

fun main(args: Array<String>) {
    val client = HttpClient(Apache)
    val json = Moshi.Builder().build()

    embeddedServer(Netty, port = Config.PORT) {
        install(AutoHeadResponse)

        routing {
            get("/") {
                val format = client.bestAndSmallestAudio(json, VideoId(v = "-RV0hKAAVro"))!!

                val output = "http://localhost:${Config.FFMPEG_PORT}/${UUID.randomUUID()}.mp3"

                val ffmpeg = FFMpeg(
                    input = format.url,
                    skipVideo = true,
                    http = FFMpegHTTP(listen = true),
                    output = URL(output)
                ).toCommand()

                Runtime.getRuntime().exec(ffmpeg).awaitMetadata()

                try {
                    proxy(client.call(output), call)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }.start(wait = true)
}
