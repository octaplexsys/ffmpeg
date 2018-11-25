package lol.adel

import com.squareup.moshi.Moshi
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.engine.apache.Apache
import io.ktor.client.response.readText
import kotlinx.coroutines.runBlocking

fun List<Format>.bestAndSmallestAudio(): Format? =
    filter { it.audioQuality == AudioQuality.AUDIO_QUALITY_MEDIUM }.minBy { it.bitrate.rate }
        ?: minBy { it.bitrate.rate }

suspend fun HttpClient.bestAndSmallestAudio(json: Moshi, id: VideoId): Format? =
    videoInfo(
        getString = { call(it.url).response.readText() },
        fromJson = { s, c -> json.adapter(c).fromJson(s.json) },
        videoId = id
    )?.streamingData?.formats?.bestAndSmallestAudio()

fun main(args: Array<String>) {
    val client = HttpClient(Apache)
    val json = Moshi.Builder().build()

    runBlocking {
        val f = client.bestAndSmallestAudio(json, VideoId(v = "-RV0hKAAVro"))!!

        println(f)

        val output = "http://localhost:8080/foo.mp3"

        val ffmpeg = FFMpeg(
            input = f.url,
            skipVideo = true,
            http = FFMpegHTTP(
                listen = true,
                seekable = true,
                multipleRequests = true
            ),
            output = URL(output)
        ).toCommand()

        Runtime.getRuntime().run {
            exec(ffmpeg).awaitMetadata()
            exec("open $output").run {
                println(inputStream.reader().use { it.readText() })
                println(errorStream.reader().use { it.readText() })
            }
        }
    }
}
