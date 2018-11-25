package lol.adel

import com.squareup.moshi.Moshi
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.engine.apache.Apache
import io.ktor.client.response.readText
import io.ktor.features.AutoHeadResponse
import io.ktor.features.PartialContent
import io.ktor.http.content.LocalFileContent
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.io.File
import java.util.concurrent.TimeUnit

fun List<Format>.bestAndSmallestAudio(): Format? =
    filter { it.audioQuality == AudioQuality.AUDIO_QUALITY_MEDIUM }.minBy { it.bitrate.rate }
        ?: minBy { it.bitrate.rate }

suspend fun HttpClient.bestAndSmallestAudio(json: Moshi, id: VideoId): Format? =
    videoInfo(
        getString = { call(it.string).response.readText() },
        fromJson = { s, c -> json.adapter(c).fromJson(s.json) },
        videoId = id
    )?.streamingData?.formats?.bestAndSmallestAudio()

object Config {
    val PORT = System.getenv("PORT")?.toInt() ?: 8080
}

fun main(args: Array<String>) {
    val client = HttpClient(Apache)
    val json = Moshi.Builder().build()

    embeddedServer(Netty, port = Config.PORT) {
        install(AutoHeadResponse)
        install(PartialContent)

        routing {
            get("/") {
                val format = client.bestAndSmallestAudio(json, VideoId(v = "-RV0hKAAVro"))!!

                val output = File("/tmp/file.mp3")

                val ffmpeg = FFMpeg(
                    input = format.url,
                    skipVideo = true,
                    output = FFMpegOutput.File(File("/tmp/file.mp3"))
                ).toCommand()

                if (Runtime.getRuntime().exec(ffmpeg).waitFor(1, TimeUnit.MINUTES)) {
                    call.respond(LocalFileContent(output))
                }
            }
        }
    }.start(wait = true)
}
